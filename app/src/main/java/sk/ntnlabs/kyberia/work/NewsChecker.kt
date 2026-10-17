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
import sk.ntnlabs.kyberia.data.KeepAlivePolicy
import sk.ntnlabs.kyberia.data.NewsTracker
import sk.ntnlabs.kyberia.data.SessionExpiredException
import sk.ntnlabs.kyberia.data.SettingsStore
import sk.ntnlabs.kyberia.notify.Notifier

/**
 * Jedna kontrola kyberie: zároveň udrží session nažive a pošle notifikácie o novinkách.
 * Spúšťa ju periodický worker aj hodinový keepalive budík; mutex zabráni súbehu.
 */
class NewsChecker(
    private val context: Context,
    private val auth: AuthRepository,
    private val bookmarks: BookmarksRepository,
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
        val now = System.currentTimeMillis()
        val quiet = settings.current.isQuiet(LocalTime.now().hour)
        // V tichom režime len udržiavame session, a to najviac raz za hodinu.
        if (quiet && !KeepAlivePolicy.quietKeepAliveDue(diagnostics.current.lastOkMillis, now)) return true

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

        if (quiet) {
            // Stav noviniek neaktualizujeme, aby po tichom režime prišlo všetko, čo sa nazbieralo.
            diagnostics.record(CheckEntry(now, CheckResult.QUIET_KEEPALIVE, trigger))
            return true
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
        true
    }
}
