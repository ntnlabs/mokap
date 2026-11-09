package sk.ntnlabs.kyberia.parse

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import sk.ntnlabs.kyberia.UrlPolicy

/** Tvoj príspevok, ktorý za poslednú hodinu dostal K; [added] za hodinu, [total] celkovo. */
data class MyK(val nodeId: Long, val title: String, val added: Int, val total: Int?)

/** Príspevok zo stránky Top K (`/id/8340566/`). */
data class TopKItem(
    val id: Long,
    val title: String,
    val parentId: Long?,
    val parentTitle: String?,
    val authorId: Long?,
    val author: String?,
    val avatarUrl: String?,
    /** „07.11.2026 - 16:08:14“ */
    val created: String?,
    val k: Int?,
    /** Počet odpovedí („- 3ch“). */
    val children: Int?,
    val content: List<ContentBlock>,
)

/**
 * Blok „your K nodes for last: [1] Hour“ na /id/19/ (`div.module_your_k_nodes`, `get_temp_k` v zdrojákoch
 * kyberie): za formulárom `<span class=most_important>N k</span> <a href=/id/ID/>názov</a> Mk <br>`.
 * Kyberia interval nikde neukladá – bežné načítanie /id/19/ ukáže vždy poslednú hodinu.
 */
object MyKParser {

    private val K = Regex("""(\d+)\s*k\b""", RegexOption.IGNORE_CASE)

    fun parse(document: Document): List<MyK> {
        val module = document.selectFirst("div.module_your_k_nodes") ?: return emptyList()
        return module.select("span.most_important").mapNotNull { span ->
            val added = K.find(span.text())?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
            var link: Element? = null
            var total: Int? = null
            // Za číslom: odkaz na príspevok a celkové K – po najbližšie <br>.
            var node: Node? = span.nextSibling()
            while (node != null && !(node is Element && node.normalName() == "br")) {
                when {
                    link == null && node is Element && node.normalName() == "a" -> link = node
                    link != null -> K.find(text(node))?.let { total = it.groupValues[1].toIntOrNull() }
                }
                node = node.nextSibling()
            }
            val a = link ?: return@mapNotNull null
            val id = NodeText.nodeId(a.attr("href")) ?: return@mapNotNull null
            MyK(id, a.attr("title").ifBlank { a.text() }.trim(), added, total)
        }
    }

    /**
     * Ako web po „Hour“: formulár s intervalom je vnorený vo formulári bookmarkov, takže prehliadač pošle ten
     * vonkajší – `anticsrf`, `interval` a `template_event=Hour`, bez `event` (nič sa nemení, len sa prekreslí /id/19/).
     */
    fun fields(hours: Int, anticsrf: String): List<Pair<String, String>> = listOf(
        "anticsrf" to anticsrf,
        "interval" to hours.toString(),
        "template_event" to "Hour",
    )

    /** Token session (na stránke je jeden pre všetky formuláre). */
    fun token(document: Document): String? =
        document.select("input[name=anticsrf]").firstNotNullOfOrNull { it.attr("value").ifEmpty { null } }

    private fun text(node: Node): String = when (node) {
        is TextNode -> node.text()
        is Element -> node.text()
        else -> ""
    }
}

/**
 * Top K: `div.item` – vľavo `div.icon-k` (avatar autora a „24 k“), vpravo `div.title`
 * (`<a>názov</a> in <a>rodič</a> by <a>autor</a> created: … - 3ch`) a `div.content`.
 */
object TopKParser {

    private val K = Regex("""(\d+)\s*k\b""", RegexOption.IGNORE_CASE)
    private val CREATED = Regex("""created:\s*(\d{1,2}\.\d{1,2}\.\d{4}\s*-\s*\d{1,2}:\d{2}:\d{2})""")
    private val CHILDREN = Regex("""-\s*(\d+)\s*ch\b""")

    fun parse(document: Document): List<TopKItem> = document.select("div.item").mapNotNull(::item)

    private fun item(item: Element): TopKItem? {
        val title = item.selectFirst("div.right-panel div.title") ?: return null
        // Priame odkazy v poradí: príspevok, rodič, autor („fook“ je vo vlastnom div-e).
        val links = title.children().filter { it.normalName() == "a" }
        val node = links.getOrNull(0) ?: return null
        val id = NodeText.nodeId(node.attr("href")) ?: return null
        val parent = links.getOrNull(1)
        val author = links.getOrNull(2)
        val text = title.ownText()
        val icon = item.selectFirst("div.icon-k")
        return TopKItem(
            id = id,
            title = node.text().trim(),
            parentId = parent?.attr("href")?.let(NodeText::nodeId),
            parentTitle = parent?.text()?.trim()?.ifEmpty { null },
            authorId = author?.attr("href")?.let(NodeText::nodeId),
            author = author?.text()?.trim()?.ifEmpty { null },
            avatarUrl = icon?.selectFirst("img")?.absUrl("src")?.takeIf(UrlPolicy::isInternal),
            created = CREATED.find(text)?.groupValues?.get(1)?.replace(Regex("""\s+"""), " "),
            k = icon?.selectFirst("div.most_important")?.text()?.let { K.find(it) }?.groupValues?.get(1)?.toIntOrNull(),
            children = CHILDREN.find(text)?.groupValues?.get(1)?.toIntOrNull(),
            content = item.selectFirst("div.right-panel div.content")?.let { ContentParser.parse(it) }.orEmpty(),
        )
    }
}
