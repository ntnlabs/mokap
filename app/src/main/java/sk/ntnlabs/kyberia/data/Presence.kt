package sk.ntnlabs.kyberia.data

import java.io.IOException
import sk.ntnlabs.kyberia.KyberiaUrls

/**
 * „Kde je“ nastaví kyberia pri každom zobrazení nodu na jeho názov. Na konci každého obnovenia preto appka
 * načíta ešte node „používa MOKAP“, aby to priatelia videli namiesto „bookmarks“; obsah sa nečíta.
 * Rozhodnutie používateľa: jedna malá požiadavka navyše na obnovenie, dá sa vypnúť v nastaveniach.
 */
class Presence(private val client: KyberiaClient, private val enabled: () -> Boolean) {

    /** Volať ako poslednú stránku obnovenia – kyberia ukazuje posledný zobrazený node. Chyba nič nepokazí. */
    suspend fun mark() {
        val url = KyberiaUrls.PRESENCE ?: return
        if (!enabled()) return
        try {
            client.get(url)
        } catch (e: IOException) {
            // Len „kde je“ – samotné obnovenie už prebehlo.
        }
    }
}
