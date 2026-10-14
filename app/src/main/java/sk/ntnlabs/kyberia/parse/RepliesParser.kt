package sk.ntnlabs.kyberia.parse

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/** Odpoveď niekoho na tvoj príspevok (blok „Last 10 user submissions children“). */
data class Reply(
    val authorId: Long?,
    val author: String,
    /** Tvoj príspevok, na ktorý odpoveď prišla – ID samotnej odpovede kyberia v bloku neuvádza. */
    val nodeId: Long,
    val nodeTitle: String,
    val snippet: String,
) {
    /** Stabilný kľúč na rozpoznanie už videných odpovedí. */
    val key: String get() = "$authorId:$nodeId:${snippet.hashCode()}"
}

/**
 * Každá položka je `div.user_sub_child`:
 * `<a href=/id/AUTOR/><img title=nick></a> <a href=/id/PRÍSPEVOK title=názov>…</a> [<span>N</span>] <br> úryvok`
 */
object RepliesParser {

    private val NODE_ID = Regex("""/id/(\d+)""")
    private val WHITESPACE = Regex("""\s+""")

    fun parse(document: Document): List<Reply> =
        document.select("div.user_sub_child").mapNotNull { item ->
            val links = item.children().filter { it.normalName() == "a" }
            val avatar = links.firstOrNull { it.selectFirst("img") != null }
            val node = links.firstOrNull { it.selectFirst("img") == null } ?: return@mapNotNull null
            val nodeId = nodeId(node.attr("href")) ?: return@mapNotNull null
            val image = avatar?.selectFirst("img")
            Reply(
                authorId = avatar?.attr("href")?.let(::nodeId),
                author = image?.attr("title")?.ifEmpty { image.attr("alt") }.orEmpty(),
                nodeId = nodeId,
                nodeTitle = node.attr("title").ifEmpty { node.text() },
                snippet = snippet(item),
            )
        }

    private fun nodeId(href: String): Long? = NODE_ID.find(href)?.groupValues?.get(1)?.toLongOrNull()

    /** Všetko za prvým `<br>` – náhľad textu odpovede. */
    private fun snippet(item: Element): String {
        val nodes = item.childNodes()
        val br = nodes.indexOfFirst { it is Element && it.normalName() == "br" }
        return nodes.drop(br + 1)
            .joinToString(" ") { node ->
                when (node) {
                    is TextNode -> node.text()
                    is Element -> node.text()
                    else -> ""
                }
            }
            .replace(' ', ' ')
            .replace(WHITESPACE, " ")
            .trim()
    }
}
