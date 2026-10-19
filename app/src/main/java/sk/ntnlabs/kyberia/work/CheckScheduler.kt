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
import sk.ntnlabs.kyberia.data.DiagnosticsStore
import sk.ntnlabs.kyberia.data.SessionGuard
import sk.ntnlabs.kyberia.data.SettingsStore

/**
 * Pravidelná kontrola kyberie podľa intervalu z nastavení (min. 15 minút) a strážca session.
 * Strážca je jeden budík na „posledné načítanie + 100 min“; každé načítanie ho posunie, takže
 * kým kontroly bežia, nespustí sa – kyberia nedostane ani jednu požiadavku navyše.
 */
class CheckScheduler(
    context: Context,
    private val settings: SettingsStore,
    private val diagnostics: DiagnosticsStore,
) {

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
        // Od poslednej úspešnej požiadavky, nie od „teraz“ – otvorenie appky samo nič nestiahne.
        val lastOk = diagnostics.current.lastOkMillis
        touch(SessionGuard.delayAfter(lastOk, System.currentTimeMillis()))
    }

    /** Kyberia práve videla načítanie stránky – strážcu posuň. */
    fun touch(delayMillis: Long = SessionGuard.DELAY_MILLIS) {
        // Nepresný budík „allow while idle“: Android ho pustí aj v Doze a na pár sekúnd povolí sieť.
        // Netreba povolenie na presné budíky; nové natiahnutie nahradí staré.
        alarmManager?.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + delayMillis,
            guardIntent(),
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
        alarmManager?.cancel(guardIntent())
    }

    private fun guardIntent(): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        0,
        Intent(appContext, SessionGuardReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val WORK_NAME = "kyberia-check"
    }
}
