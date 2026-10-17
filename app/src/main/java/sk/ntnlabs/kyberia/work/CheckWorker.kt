package sk.ntnlabs.kyberia.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import sk.ntnlabs.kyberia.MokapApp
import sk.ntnlabs.kyberia.data.CheckTrigger

/** Periodická kontrola cez WorkManager. */
class CheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ok = (applicationContext as MokapApp).container.checker.run(CheckTrigger.SCHEDULED)
        return if (ok) Result.success() else Result.retry()
    }
}
