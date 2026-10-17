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
import sk.ntnlabs.kyberia.data.MailRepository
import sk.ntnlabs.kyberia.data.NewsTracker
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
) {
    private val mutex = Mutex()

    companion object {
        /** Detail do diagnostiky: kyberia namiesto stránky ukázala prihlasovací formulár. */
        const val LOGIN_SHOWN = "na stránke bol login"
    }

    /** Vráti false pri dočasnej chybe (treba skúsiť znova). */
    suspend fun run(trigger: CheckTrigger): Boolean = mutex.withLock {
        if (!auth.loggedIn.value) return true
        // V tichom režime kyberiu vôbec nekontrolujeme; novinky prídu po jeho skončení.
        if (settings.current.isQuiet(LocalTime.now().hour)) return true
        val now = System.currentTimeMillis()

        val page = try {
            bookmarks.load()
        } catch (e: SessionExpiredException) {
            diagnostics.record(CheckEntry(now, CheckResult.EXPIRED, trigger, LOGIN_SHOWN))
            auth.sessionEnded()
            Notifier.sessionExpired(context)
            return true
        } catch (e: IOException) {
            diagnostics.record(CheckEntry(now, CheckResult.ERROR, trigger, e.message ?: e.javaClass.simpleName))
            return false
        }

        diagnostics.record(CheckEntry(now, CheckResult.OK, trigger))

        // Stav sa aktualizuje vždy, aby po zapnutí notifikácie neprišla stará záplava.
        val replies = tracker.updateReplies(page.replies, page.user.userId)
        val increases = tracker.updateBookmarks(page.bookmarks)
        val friends = tracker.updateFriends(page.friends)

        val current = settings.current
        if (current.notifyReplies && replies.isNotEmpty()) Notifier.replies(context, replies)
        if (current.notifyBookmarks && increases.isNotEmpty()) Notifier.bookmarks(context, increases)
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
            added > 0 -> Notifier.mail(context, check.count)
        }
    }
}
