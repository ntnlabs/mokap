package sk.ntnlabs.kyberia.ui.common

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Či ide telefón práve cez meranú sieť (mobilné dáta, hotspot). Sleduje sa len keď [enabled];
 * inak je výsledok vždy `false` a appka nič neregistruje.
 */
@Composable
fun rememberMeteredNetwork(enabled: Boolean): State<Boolean> {
    val context = LocalContext.current
    val connectivity = remember { context.getSystemService(ConnectivityManager::class.java) }
    // Počiatočná hodnota hneď, aby prvá požiadavka na avatar nevyšla skôr, ako príde callback.
    val metered = remember(enabled) { mutableStateOf(enabled && connectivity.isActiveNetworkMetered) }
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                metered.value = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            }
        }
        connectivity.registerDefaultNetworkCallback(callback, Handler(Looper.getMainLooper()))
        onDispose { connectivity.unregisterNetworkCallback(callback) }
    }
    return metered
}
