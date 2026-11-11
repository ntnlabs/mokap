package sk.ntnlabs.kyberia.parse

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.FormElement

/** Kam poslať akciu (POST) a s akým tokenom. Token drží len v pamäti a nie je vo výpise. */
class PostTarget(val action: String, val anticsrf: String, val multipart: Boolean) {
    override fun toString(): String = "PostTarget(action=$action)"
}

/** Text na úpravu z „conf“ stránky nodu (`/id/N/1961033`) a kam ho poslať (`event=configure_content`). */
class EditForm(val target: PostTarget, val content: String) {
    override fun toString(): String = "EditForm(${target.action})"
}

/**
 * Akcie na stránke nodu okrem odpovede ([ReplyFormParser]): K a úprava. Na stránke je jeden `anticsrf`
 * pre celú session – keď ho formulár nemá, vezme sa z inej časti stránky.
 */
object NodeActionsParser {

    private const val K = "K"
    private const val REVOKE = "revoKe"
    private const val CONFIGURE_CONTENT = "configure_content"
    private const val CONTENT = "node_content"

    /** K pre samotný node – formulár s tlačidlom K pri počte K (`span.node_info2`), nie `formular`. */
    fun kNode(document: Document): PostTarget? =
        Forms.find(document) { it.attr("name") != "formular" && Forms.submitButton(it, K) != null }?.let { target(it, document) }

    /**
     * K tomuto nodu si už dal: namiesto „K+❤️“ (`event=K`) je pri počte K tlačidlo „-❤️“ (`event=revoKe`). To je
     * na webe „druhé stlačenie“ – odobratie K (penalizované). Appka `revoKe` nikdy neposiela.
     */
    fun kGiven(document: Document): Boolean =
        Forms.find(document) { it.attr("name") != "formular" && Forms.submitButton(it, REVOKE) != null } != null

    /** K pre odpoveď – `formular` s tlačidlom K; kyberia dá K zaškrtnutému `node_chosen[]` (`K.inc`). */
    fun kChild(document: Document): PostTarget? =
        Forms.find(document) { it.attr("name") == "formular" && Forms.submitButton(it, K) != null }?.let { target(it, document) }

    /** Presne jedno K: bez [childId] samotnému nodu, inak tej odpovedi. Kyberia by dala K každému zaškrtnutému. */
    fun kFields(target: PostTarget, childId: Long?): List<Pair<String, String>> = buildList {
        add("anticsrf" to target.anticsrf)
        if (childId != null) add("node_chosen[]" to childId.toString())
        add("event" to K)
    }

    /** Formulár s pôvodným textom (`textarea[name=node_content]`) a tlačidlom `configure_content`. */
    fun edit(document: Document): EditForm? {
        val form = Forms.find(document) {
            it.control(CONTENT)?.normalName() == "textarea" && Forms.submitButton(it, CONFIGURE_CONTENT) != null
        } ?: return null
        val target = target(form, document) ?: return null
        return EditForm(target, form.control(CONTENT)?.`val`().orEmpty())
    }

    /** Len token, nový text a `event` – formulár na „conf“ stránke má aj „code“ a „wiki“, tie nechávame tak. */
    fun editFields(form: EditForm, content: String): List<Pair<String, String>> = listOf(
        "anticsrf" to form.target.anticsrf,
        CONTENT to content,
        "event" to CONFIGURE_CONTENT,
    )

    private fun target(form: FormElement, document: Document): PostTarget? {
        val token = form.control("anticsrf")?.attr("value")?.ifEmpty { null }
            ?: Forms.anticsrf(document)
            ?: return null
        return PostTarget(Forms.action(form, document), token, Forms.isMultipart(form))
    }

    private fun FormElement.control(name: String): Element? = elements().firstOrNull { it.attr("name") == name }
}

/** Chyba, ktorú kyberia po akcii ukáže v hlavičke stránky (`<center><span class=most_important>`). */
object PageError {
    fun of(document: Document): String? =
        document.select("center > span.most_important").firstNotNullOfOrNull { it.text().trim().ifEmpty { null } }
}
