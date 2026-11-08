package sk.ntnlabs.kyberia.data

import java.nio.charset.StandardCharsets
import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.parse.NodePage
import sk.ntnlabs.kyberia.parse.NodeParser
import sk.ntnlabs.kyberia.parse.PageKind
import sk.ntnlabs.kyberia.parse.ReplyFormParser
import sk.ntnlabs.kyberia.parse.Session

/**
 * Node na čítanie v appke – jedna požiadavka ako na webe, nič vopred. Bez „používa MOKAP“ po ňom:
 * otvorený node je skutočné „kde je“ (ručné kliknutie).
 */
class NodeRepository(private val client: KyberiaClient) {

    /** O odhlásení rozhoduje [loggedInPage]; pri sieťovej chybe IOException. */
    suspend fun load(id: Long): NodePage = NodeParser.parse(client.loggedInPage(KyberiaUrls.node(id)), id)

    /**
     * Pridá odpoveď do nodu [page] (alebo na jeho odpoveď [replyTo]) – POST `event=add` s tokenom z tej istej
     * stránky. Odoslanie je jednorazové: OkHttp ho po chybe spojenia neopakuje, aby nevznikol príspevok dvakrát;
     * IOException teda znamená „nevieme, či prešlo“. [me] = vlastné ID na potvrdenie novej odpovede.
     */
    suspend fun reply(page: NodePage, content: String, replyTo: Long?, noHtml: Boolean, me: Long?): ReplyResult {
        val form = checkNotNull(page.replyForm) { "Node bez formulára na odpoveď" }
        val response = client.postForm(
            form.action,
            ReplyFormParser.fields(form, content, replyTo, noHtml),
            StandardCharsets.UTF_8,
            form.multipart,
            oneShot = true,
        )
        when (Session.classify(response.document)) {
            PageKind.LOGGED_IN -> Unit
            PageKind.LOGGED_OUT -> throw SessionExpiredException(LogoutDetail.of(form.action, response.url))
            PageKind.UNKNOWN -> throw UnexpectedPageException(response.code)
        }
        return Replies.outcome(page, response.document, me)
    }
}
