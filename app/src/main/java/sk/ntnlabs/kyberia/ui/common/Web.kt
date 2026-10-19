package sk.ntnlabs.kyberia.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Browser
import androidx.core.net.toUri

/**
 * Kyberia v prehliadači telefónu. `EXTRA_APPLICATION_ID` povie Chromium prehliadačom (Chrome, Brave…),
 * že odkaz prišiel z tejto appky – ďalšie odkazy z nej potom otvoria v tej istej karte, nie v novej.
 */
object Web {

    fun intent(context: Context, url: String): Intent =
        Intent(Intent.ACTION_VIEW, url.toUri())
            .putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun open(context: Context, url: String) {
        try {
            context.startActivity(intent(context, url))
        } catch (e: ActivityNotFoundException) {
            // Bez prehliadača niet kam – nič nerobíme.
        }
    }
}
