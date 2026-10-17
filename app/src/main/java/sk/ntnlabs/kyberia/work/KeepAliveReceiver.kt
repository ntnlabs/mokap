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

/**
 * Hodinový keepalive. Budík „allow while idle“ Android pustí aj v hlbokom spánku (Doze)
 * a na pár sekúnd povolí sieť – stihneme jednu požiadavku, ktorá udrží session nažive.
 */
class KeepAliveReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as MokapApp).container
        val pending = goAsync()
        scope.launch {
            try {
                container.checker.run(CheckTrigger.KEEPALIVE)
            } finally {
                if (container.auth.loggedIn.value) container.scheduler.armKeepAlive()
                pending.finish()
            }
        }
    }

    private companion object {
        // Chyba v jednej kontrole nesmie zhodiť appku na pozadí.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, _ -> })
    }
}
