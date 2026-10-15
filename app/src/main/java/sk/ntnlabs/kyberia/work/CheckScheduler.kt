package sk.ntnlabs.kyberia.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import sk.ntnlabs.kyberia.data.SettingsStore

/** Pravidelná kontrola kyberie na pozadí podľa intervalu z nastavení (min. 15 minút). */
class CheckScheduler(context: Context, private val settings: SettingsStore) {

    private val workManager = WorkManager.getInstance(context.applicationContext)

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
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "kyberia-check"
    }
}
