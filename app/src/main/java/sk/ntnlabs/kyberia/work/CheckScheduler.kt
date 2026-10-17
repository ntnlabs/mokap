package sk.ntnlabs.kyberia.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import sk.ntnlabs.kyberia.data.KeepAlivePolicy
import sk.ntnlabs.kyberia.data.SettingsStore

/**
 * Plánuje kontroly kyberie: periodickú prácu podľa intervalu z nastavení (min. 15 minút)
 * a hodinový keepalive budík, ktorý prežije aj nočný Doze.
 */
class CheckScheduler(context: Context, private val settings: SettingsStore) {

    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun schedule() {
        val request = PeriodicWorkRequestBuilder<CheckWorker>(
            settings.current.intervalMinutes.toLong(),
            TimeUnit.MINUTES,
        )
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        armKeepAlive()
    }

    /** Nepresný budík (netreba povolenie na presné budíky); nové natiahnutie nahradí staré. */
    fun armKeepAlive() {
        alarmManager?.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + KeepAlivePolicy.INTERVAL_MILLIS,
            keepAliveIntent(),
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
        alarmManager?.cancel(keepAliveIntent())
    }

    private fun keepAliveIntent(): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        0,
        Intent(appContext, KeepAliveReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val WORK_NAME = "kyberia-check"
    }
}
