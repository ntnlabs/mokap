package sk.ntnlabs.kyberia.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import sk.ntnlabs.kyberia.MokapApp
import sk.ntnlabs.kyberia.data.CheckTrigger

/** Periodická kontrola cez WorkManager (v noci ju Android môže odkladať – preto aj [KeepAliveReceiver]). */
class CheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as MokapApp).container
        val ok = container.checker.run(CheckTrigger.SCHEDULED)
        // Po reštarte telefónu budík neprežije, WorkManager áno – tak ho tu znova natiahneme.
        if (container.auth.loggedIn.value) container.scheduler.armKeepAlive()
        return if (ok) Result.success() else Result.retry()
    }
}
