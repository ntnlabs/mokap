package sk.ntnlabs.kyberia.parse

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.FormElement

/**
 * Formulár na pridanie odpovede (`form[name=formular]` s `textarea[name=node_content]`). Kyberia ho ukáže
 * len s právom zápisu do nodu – bez neho appka odpovedať neponúkne. Token drží len v pamäti.
 */
class ReplyForm(
    val action: String,
    val anticsrf: String,
    val parent: String,
    val templateId: String,
    val multipart: Boolean,
) {
    // Token nepatrí do logov ani do výpisov stavu.
    override fun toString(): String = "ReplyForm(action=$action, parent=$parent)"
}

object ReplyFormParser {

    private const val CONTENT = "node_content"

    fun parse(document: Document): ReplyForm? {
        val form = Forms.find(document) { form ->
            form.attr("name") == "formular" && form.control(CONTENT)?.normalName() == "textarea"
        } ?: return null
        return ReplyForm(
            action = Forms.action(form, document),
            anticsrf = form.control("anticsrf")?.attr("value")?.ifEmpty { null } ?: return null,
            parent = form.control("node_parent")?.attr("value")?.ifEmpty { null } ?: return null,
            // Bez neho kyberia vezme 4 (submission) – posielame rovnako ako web.
            templateId = form.control("template_id")?.attr("value")?.ifEmpty { null } ?: "4",
            multipart = Forms.isMultipart(form),
        )
    }

    /**
     * Len polia na pridanie príspevku – **nie** všetko z formulára: `formular` obsahuje aj tlačidlá K, put,
     * delete_hardlink, výber bookmarkov a zaškrtávacie `node_chosen[]` pri odpovediach. [replyTo] = odpoveď na
     * konkrétnu odpoveď (`node_chosen[]`, kyberia ju použije ako rodiča). Názov prázdny – kyberia doplní „Re: …“.
     */
    fun fields(form: ReplyForm, content: String, replyTo: Long?, noHtml: Boolean): List<Pair<String, String>> = buildList {
        add("anticsrf" to form.anticsrf)
        add("node_name" to "")
        if (noHtml) add("no_html" to "yes")
        add(CONTENT to content)
        add("template_id" to form.templateId)
        add("node_parent" to form.parent)
        if (replyTo != null) add("node_chosen[]" to replyTo.toString())
        add("event" to "add")
    }

    /** Chyba, ktorú kyberia po odoslaní ukáže v hlavičke stránky („Chyba HTML syntaxe!“, prázdny obsah…). */
    fun error(document: Document): String? = PageError.of(document)

    private fun FormElement.control(name: String): Element? = elements().firstOrNull { it.attr("name") == name }
}
