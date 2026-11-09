package sk.ntnlabs.kyberia.data

import java.nio.charset.StandardCharsets
import sk.ntnlabs.kyberia.KyberiaUrls
import org.jsoup.nodes.Document
import sk.ntnlabs.kyberia.parse.EditForm
import sk.ntnlabs.kyberia.parse.NodeActionsParser
import sk.ntnlabs.kyberia.parse.NodePage
import sk.ntnlabs.kyberia.parse.NodeParser
import sk.ntnlabs.kyberia.parse.PageError
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
        val response = post(form.action, ReplyFormParser.fields(form, content, replyTo, noHtml), form.multipart)
        return Replies.outcome(page, response, me)
    }

    /** Jedno K nodu [page] alebo jeho odpovedi [childId]. Odobrať K appka nevie a ani nebude (penalizácia). */
    suspend fun giveK(page: NodePage, childId: Long?): KResult {
        val target = checkNotNull(if (childId == null) page.kNode else page.kChild) { "K tu kyberia neponúka" }
        val response = post(target.action, NodeActionsParser.kFields(target, childId), target.multipart)
        return NodeOutcomes.k(page, childId, response)
    }

    /** Pôvodný text nodu [id] z jeho „conf“ stránky; null = kyberia úpravu neponúkla (nie je tvoj). */
    suspend fun editSource(id: Long): EditForm? = NodeActionsParser.edit(client.loggedInPage(KyberiaUrls.configure(id)))

    /**
     * Uloží nový text (`event=configure_content`). Kyberia vráti stránku upraveného nodu; ak sa upravovala odpoveď
     * v inom node ([shownId]), ten sa stiahne znova, aby bolo zmenu vidno.
     */
    suspend fun saveEdit(form: EditForm, content: String, editedId: Long, shownId: Long): EditResult {
        val response = post(form.target.action, NodeActionsParser.editFields(form, content), form.target.multipart)
        val error = PageError.of(response)
        val page = if (editedId == shownId) NodeParser.parse(response, shownId) else runCatching { load(shownId) }.getOrNull()
        return if (error != null) EditResult.Rejected(page, error) else EditResult.Saved(page)
    }

    /**
     * POST akcie: jednorazový (OkHttp ho po chybe spojenia neopakuje) – IOException znamená „nevieme, či prešla“.
     * O odhlásení rozhoduje prihlasovací formulár na vrátenej stránke.
     */
    private suspend fun post(action: String, fields: List<Pair<String, String>>, multipart: Boolean): Document {
        val response = client.postForm(action, fields, StandardCharsets.UTF_8, multipart, oneShot = true)
        when (Session.classify(response.document)) {
            PageKind.LOGGED_IN -> Unit
            PageKind.LOGGED_OUT -> throw SessionExpiredException(LogoutDetail.of(action, response.url))
            PageKind.UNKNOWN -> throw UnexpectedPageException(response.code)
        }
        return response.document
    }
}
