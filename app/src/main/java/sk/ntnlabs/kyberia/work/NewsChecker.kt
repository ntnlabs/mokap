package sk.ntnlabs.kyberia.work

import android.content.Context
import java.io.IOException
import java.time.LocalTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sk.ntnlabs.kyberia.data.AuthRepository
import sk.ntnlabs.kyberia.data.BookmarksRepository
import sk.ntnlabs.kyberia.data.CheckEntry
import sk.ntnlabs.kyberia.data.CheckResult
import sk.ntnlabs.kyberia.data.CheckTrigger
import sk.ntnlabs.kyberia.data.DiagnosticsStore
import sk.ntnlabs.kyberia.data.LatestPage
import sk.ntnlabs.kyberia.data.MailRepository
import sk.ntnlabs.kyberia.data.NewsTracker
import sk.ntnlabs.kyberia.data.Nsfw
import sk.ntnlabs.kyberia.data.RefreshTimer
import sk.ntnlabs.kyberia.data.SessionExpiredException
import sk.ntnlabs.kyberia.data.SettingsStore
import sk.ntnlabs.kyberia.notify.Notifier
import sk.ntnlabs.kyberia.parse.MailCheck

/**
 * Jedna kontrola kyberie a notifikácie o novinkách. Mutex zabráni súbehu,
 * ak by sa kontrola spustila dvakrát naraz.
 */
class NewsChecker(
    private val context: Context,
    private val auth: AuthRepository,
    private val bookmarks: BookmarksRepository,
    private val mail: MailRepository,
    private val tracker: NewsTracker,
    private val settings: SettingsStore,
    private val diagnostics: DiagnosticsStore,
    private val latest: LatestPage,
    private val scheduler: CheckScheduler,
) {
    private val mutex = Mutex()

    /** Vráti false pri dočasnej chybe (treba skúsiť znova). */
    suspend fun run(trigger: CheckTrigger): Boolean = mutex.withLock {
        if (!auth.loggedIn.value) return true
        val current = settings.current
        // V tichom režime len strážca session: jedna požiadavka, aby kyberia po 2 h neodhlásila.
        // Bez notifikácií a bez posunu „videného“ – novinky z noci prídu po skončení tichého režimu.
        val quiet = current.isQuiet(LocalTime.now().hour)
        if (quiet && trigger != CheckTrigger.KEEPALIVE) return true
        val now = System.currentTimeMillis()

        // Otvorená appka stránku stiahla pred chvíľou (a novinky ukázala) – druhýkrát ju netreba; pošta je zvlášť.
        // Počíta sa len načítanie v appke: WorkManager nespúšťa presne, vlastné behy by si navzájom prekážali.
        val lastAppLoad = diagnostics.current.last
            ?.takeIf { it.trigger == CheckTrigger.APP && it.result == CheckResult.OK }
            ?.atMillis
        if (trigger == CheckTrigger.SCHEDULED &&
            RefreshTimer.recentlyLoaded(lastAppLoad, now, current.intervalMinutes * 60_000L)
        ) {
            if (current.notifyMail) checkMail()
            return true
        }

        val page = try {
            bookmarks.load()
        } catch (e: SessionExpiredException) {
            diagnostics.record(CheckEntry(now, CheckResult.EXPIRED, trigger, e.detail))
            auth.sessionEnded()
            Notifier.sessionExpired(context)
            return true
        } catch (e: IOException) {
            diagnostics.record(CheckEntry(now, CheckResult.ERROR, trigger, e.message ?: e.javaClass.simpleName))
            return false
        }

        diagnostics.record(CheckEntry(now, CheckResult.OK, trigger))
        latest.publish(page, now)
        scheduler.touch()
        if (quiet) return true

        // Stav sa aktualizuje vždy, aby po zapnutí notifikácie neprišla stará záplava.
        val replies = tracker.updateReplies(page.replies, page.user.userId)
        val increases = tracker.updateBookmarks(page.bookmarks)
        val friends = tracker.updateFriends(page.friends)

        val shownReplies = Nsfw.replies(replies, current.hideNsfw)
        val shownIncreases = Nsfw.increases(increases, current.hideNsfw)
        if (current.notifyReplies && shownReplies.isNotEmpty()) Notifier.replies(context, shownReplies)
        if (current.notifyBookmarks && shownIncreases.isNotEmpty()) Notifier.bookmarks(context, shownIncreases)
        if (current.notifyFriends && friends.isNotEmpty()) Notifier.friendsOnline(context, friends)
        // Pošta je jedna malá požiadavka navyše – len keď o ňu používateľ stojí.
        if (current.notifyMail) checkMail()
        true
    }

    /** Chyba pošty nepokazí zvyšok kontroly; o session rozhoduje len stránka s loginom. */
    private suspend fun checkMail() {
        val check = try {
            mail.check()
        } catch (e: IOException) {
            return
        }
        if (check !is MailCheck.Unread) return
        val added = tracker.updateMail(check.count)
        when {
            check.count == 0 -> Notifier.cancelMail(context) // prečítané na webe
            added > 0 -> Notifier.mail(context, check.count, check.lastFrom)
        }
    }
}
