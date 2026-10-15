package sk.ntnlabs.kyberia.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.IOException
import java.time.LocalTime
import sk.ntnlabs.kyberia.MokapApp
import sk.ntnlabs.kyberia.data.SessionExpiredException
import sk.ntnlabs.kyberia.notify.Notifier

/** Jedna požiadavka na /id/19/ a lokálne notifikácie o tom, čo pribudlo od minula. */
class CheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as MokapApp).container
        if (!container.auth.loggedIn.value) return Result.success()

        // V tichom režime kyberiu vôbec nekontrolujeme; novinky prídu po jeho skončení.
        val settings = container.settings.current
        if (settings.isQuiet(LocalTime.now().hour)) return Result.success()

        val page = try {
            container.bookmarks.load()
        } catch (e: SessionExpiredException) {
            container.auth.sessionEnded()
            Notifier.sessionExpired(applicationContext)
            return Result.success()
        } catch (e: IOException) {
            return Result.retry()
        }

        // Stav sa aktualizuje vždy, aby po zapnutí notifikácie neprišla stará záplava.
        val tracker = container.newsTracker
        val replies = tracker.updateReplies(page.replies, page.user.userId)
        val increases = tracker.updateBookmarks(page.bookmarks)
        val friends = tracker.updateFriends(page.friends)

        if (settings.notifyReplies && replies.isNotEmpty()) Notifier.replies(applicationContext, replies)
        if (settings.notifyBookmarks && increases.isNotEmpty()) Notifier.bookmarks(applicationContext, increases)
        if (settings.notifyFriends && friends.isNotEmpty()) Notifier.friendsOnline(applicationContext, friends)
        return Result.success()
    }
}
