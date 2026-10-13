package sk.ntnlabs.kyberia.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.IOException
import sk.ntnlabs.kyberia.MokapApp
import sk.ntnlabs.kyberia.data.SessionExpiredException
import sk.ntnlabs.kyberia.notify.Notifier

/** Jedna požiadavka na /id/19/ a lokálna notifikácia, ak v bookmarkoch pribudlo niečo nové. */
class CheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as MokapApp).container
        if (!container.auth.loggedIn.value) return Result.success()

        val page = try {
            container.bookmarks.load()
        } catch (e: SessionExpiredException) {
            container.auth.sessionEnded()
            Notifier.sessionExpired(applicationContext)
            return Result.success()
        } catch (e: IOException) {
            return Result.retry()
        }

        val increases = container.bookmarkTracker.update(page.bookmarks)
        if (increases.isNotEmpty()) Notifier.bookmarks(applicationContext, increases)
        return Result.success()
    }
}
