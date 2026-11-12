package sk.ntnlabs.kyberia.data

import java.nio.charset.StandardCharsets
import kotlinx.coroutines.delay
import org.jsoup.nodes.Document
import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.parse.MailCheck
import sk.ntnlabs.kyberia.parse.MailCheckParser
import sk.ntnlabs.kyberia.parse.MailPage
import sk.ntnlabs.kyberia.parse.MailParser
import sk.ntnlabs.kyberia.parse.PageError
import sk.ntnlabs.kyberia.parse.PageKind
import sk.ntnlabs.kyberia.parse.Session

/** Výsledok odoslania správy; vždy s čerstvou poštou, ktorú kyberia vrátila. */
sealed interface SendResult {
    val page: MailPage

    data class Sent(override val page: MailPage, val mailId: Long) : SendResult

    /** Kyberia správu odmietla (preklep v nicku, prázdny text, chyba HTML…). */
    data class Rejected(override val page: MailPage, val message: String) : SendResult

    /** Chybu nehlásila, ale novú odoslanú správu nevidno – appka znova neposiela sama. */
    data class Unconfirmed(override val page: MailPage) : SendResult
}

object MailOutcomes {
    /** Odoslaná = v pošte je nová správa od teba, ktorá predtým nebola. */
    fun send(before: MailPage, response: Document): SendResult {
        val page = MailParser.parse(response)
        PageError.of(response)?.let { return SendResult.Rejected(page, it) }
        return recheck(before, page)
    }

    /** Overenie na čerstvo načítanej pošte (po odoslaní kyberia novú správu niekedy hneď neukáže). */
    fun recheck(before: MailPage, page: MailPage): SendResult {
        val known = before.mails.mapTo(HashSet()) { it.id }
        val sent = page.mails.firstOrNull { it.id !in known && page.outgoing(it) }
        return if (sent != null) SendResult.Sent(page, sent.id) else SendResult.Unconfirmed(page)
    }
}

/**
 * Pošta. Počet nových cez ľahký AJAX endpoint (kontrola v pozadí, pár bajtov); samotná pošta (`/id/24/`) len pri
 * otvorení záložky – kyberia ju tým, ako na webe, označí ako prečítanú. Správy appka drží len v pamäti.
 */
class MailRepository(private val client: KyberiaClient) {

    /** null = nečakaná odpoveď; pri sieťovej chybe IOException. */
    suspend fun check(): MailCheck? = MailCheckParser.parse(client.getText(KyberiaUrls.MAIL_CHECK))

    /** O odhlásení rozhoduje [loggedInPage]. */
    suspend fun load(): MailPage = MailParser.parse(client.loggedInPage(KyberiaUrls.MAIL))

    /**
     * Pošle správu – jednorazový POST (OkHttp ho po chybe spojenia neopakuje), appka ho nikdy neposiela znova sama;
     * IOException = nevieme, či odišla. [byId] = adresát je číslo profilu.
     */
    suspend fun send(page: MailPage, to: String, byId: Boolean, text: String, noHtml: Boolean): SendResult {
        val form = checkNotNull(page.form) { "Pošta bez formulára" }
        val response = client.postForm(
            form.action,
            MailParser.sendFields(form, to, byId, text, noHtml),
            StandardCharsets.UTF_8,
            form.multipart,
            oneShot = true,
        )
        when (Session.classify(response.document)) {
            PageKind.LOGGED_IN -> Unit
            PageKind.LOGGED_OUT -> throw SessionExpiredException(LogoutDetail.of(form.action, response.url))
            PageKind.UNKNOWN -> throw UnexpectedPageException(response.code)
        }
        var result = MailOutcomes.send(page, response.document)
        // Kyberia chybu nehlásila, ale novú správu v odpovedi nevidno. Raz počkať a poštu načítať znova – len
        // v tomto prípade, jedna požiadavka navyše (rozhodnutie používateľa). Nikdy znova neposielať.
        if (result is SendResult.Unconfirmed) {
            delay(RECHECK_DELAY_MILLIS)
            result = MailOutcomes.recheck(page, load())
        }
        return result
    }

    private companion object {
        const val RECHECK_DELAY_MILLIS = 3_000L
    }
}
