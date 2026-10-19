package sk.ntnlabs.kyberia

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * Appka je len po slovensky. Android však tvar pri počtoch (plurals) vyberá podľa jazyka telefónu –
 * v angličtine pozná len „1“ a „viac“, takže by ukázal „2 ľudí“. Preto appka beží vždy so slovenčinou.
 */
object AppLocale {

    private val SLOVAK: Locale = Locale.forLanguageTag("sk")

    fun wrap(base: Context): Context {
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(SLOVAK))
        return base.createConfigurationContext(config)
    }
}
