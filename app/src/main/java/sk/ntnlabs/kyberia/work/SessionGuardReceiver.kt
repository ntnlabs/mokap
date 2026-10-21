package sk.ntnlabs.kyberia.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import sk.ntnlabs.kyberia.MokapApp
import sk.ntnlabs.kyberia.data.CheckTrigger
import sk.ntnlabs.kyberia.data.SessionGuard

/**
 * Strážca session: Android 100 minút nepustil kontrolu (Doze, menej používaná appka),
 * tak urobíme jednu sami, kým kyberia po 2 hodinách neodhlási.
 */
class SessionGuardReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as MokapApp).container
        val pending = goAsync()
        scope.launch {
            var ok = false
            try {
                ok = container.checker.run(CheckTrigger.KEEPALIVE)
            } finally {
                // Úspešné načítanie strážcu posunie samo; bez siete skús znova, len kým to ešte môže session zachrániť.
                if (!ok && container.auth.loggedIn.value) {
                    SessionGuard.retryAfter(container.diagnostics.current.lastOkMillis, System.currentTimeMillis())
                        ?.let(container.scheduler::touch)
                }
                pending.finish()
            }
        }
    }

    private companion object {
        // Chyba v jednej kontrole nesmie zhodiť appku na pozadí.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, _ -> })
    }
}
