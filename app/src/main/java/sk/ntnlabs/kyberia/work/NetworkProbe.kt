package sk.ntnlabs.kyberia.work

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.PowerManager
import sk.ntnlabs.kyberia.data.NetState
import sk.ntnlabs.kyberia.data.NetworkConditions

/** Či mal telefón pri chybe sieť a čím appku Android na pozadí obmedzoval. */
object NetworkProbe {

    fun conditions(context: Context): NetworkConditions {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val power = context.getSystemService(PowerManager::class.java)
        return NetworkConditions(
            network = network(connectivity),
            deviceIdle = power.isDeviceIdleMode,
            powerSave = power.isPowerSaveMode,
            dataSaver = connectivity.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED,
            backgroundRestricted = context.getSystemService(ActivityManager::class.java).isBackgroundRestricted,
            standbyBucket = context.getSystemService(UsageStatsManager::class.java).appStandbyBucket,
        )
    }

    // Zablokovanú sieť ukáže len NetworkInfo – activeNetwork vtedy vráti null, ako keby siete nebolo.
    @Suppress("DEPRECATION")
    private fun network(connectivity: ConnectivityManager): NetState {
        if (connectivity.activeNetworkInfo?.detailedState == NetworkInfo.DetailedState.BLOCKED) return NetState.BLOCKED
        val capabilities = connectivity.activeNetwork?.let(connectivity::getNetworkCapabilities) ?: return NetState.NONE
        return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) NetState.OK else NetState.NO_INTERNET
    }
}
