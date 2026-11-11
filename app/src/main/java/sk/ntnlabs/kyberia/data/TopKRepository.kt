package sk.ntnlabs.kyberia.data

import java.nio.charset.StandardCharsets
import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.parse.PageKind
import sk.ntnlabs.kyberia.parse.Session
import sk.ntnlabs.kyberia.parse.TopKItem
import sk.ntnlabs.kyberia.parse.TopKPage
import sk.ntnlabs.kyberia.parse.TopKParser

/** Top K (`/id/8340566/`) – sťahuje sa len na požiadanie v appke, nikdy v pozadí. */
class TopKRepository(private val client: KyberiaClient, private val presence: Presence? = null) {

    /** Po stránke ešte „používa MOKAP“ ([Presence]), aby „kde je“ neostalo na top K. */
    suspend fun load(): TopKPage = TopKParser.page(client.loggedInPage(KyberiaUrls.TOP_K).also { presence?.mark() })

    /**
     * Jedno K príspevku z Top K – ako web (`ajaxK`). Jednorazový POST (OkHttp ho po chybe spojenia neopakuje);
     * appka nikdy neposiela `revoKe`. Výsledok podľa vrátenej stránky nodu ([NodeOutcomes.kFromList]).
     */
    suspend fun giveK(item: TopKItem, anticsrf: String): KResult {
        val url = KyberiaUrls.node(item.id)
        val page = client.postForm(url, TopKParser.kFields(anticsrf), StandardCharsets.UTF_8, multipart = false, oneShot = true)
        when (Session.classify(page.document)) {
            PageKind.LOGGED_IN -> Unit
            PageKind.LOGGED_OUT -> throw SessionExpiredException(LogoutDetail.of(url, page.url))
            PageKind.UNKNOWN -> throw UnexpectedPageException(page.code)
        }
        return NodeOutcomes.kFromList(item.id, item.k, page.document)
    }
}
