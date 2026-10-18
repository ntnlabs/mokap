package sk.ntnlabs.kyberia.parse

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import sk.ntnlabs.kyberia.UrlPolicy

/** Odpoveď niekoho na tvoj príspevok (blok „Last 10 user submissions children“). */
data class Reply(
    val authorId: Long?,
    val author: String,
    /** Tvoj príspevok, na ktorý odpoveď prišla – ID samotnej odpovede kyberia v bloku neuvádza. */
    val nodeId: Long,
    val nodeTitle: String,
    val snippet: String,
    /** Avatar autora (často animovaný GIF), absolútna URL na kyberii. */
    val avatarUrl: String? = null,
    /** Koľko K odpoveď dostala (`[1K]`); null = žiadne. */
    val k: Int? = null,
    /** Počet odpovedí pod touto odpoveďou (`[N]` bez K). */
    val children: Int? = null,
) {
    /** Stabilný kľúč na rozpoznanie už videných odpovedí – K sa doňho zámerne nepočíta. */
    val key: String get() = "$authorId:$nodeId:${snippet.hashCode()}"
}

/**
 * Každá položka je `div.user_sub_child`:
 * `<a href=/id/AUTOR/><img title=nick></a> <a href=/id/PRÍSPEVOK title=názov>…</a> [<span>1K</span>] [<span>N</span>] <br> úryvok`
 */
object RepliesParser {

    private val K = Regex("""(\d+)\s*K""", RegexOption.IGNORE_CASE)
    private val COUNT = Regex("""\d+""")

    fun parse(document: Document): List<Reply> =
        document.select("div.user_sub_child").mapNotNull { item ->
            val links = item.children().filter { it.normalName() == "a" }
            val avatar = links.firstOrNull { it.selectFirst("img") != null }
            val node = links.firstOrNull { it.selectFirst("img") == null } ?: return@mapNotNull null
            val nodeId = NodeText.nodeId(node.attr("href")) ?: return@mapNotNull null
            val image = avatar?.selectFirst("img")
            val lines = NodeText.lines(item)
            val counters = counters(lines.first())
            Reply(
                authorId = avatar?.attr("href")?.let(NodeText::nodeId),
                author = image?.attr("title")?.ifEmpty { image.attr("alt") }.orEmpty(),
                nodeId = nodeId,
                nodeTitle = node.attr("title").ifEmpty { node.text() },
                // Všetko za prvým <br> – náhľad textu odpovede.
                snippet = NodeText.join(lines.drop(1).flatten()),
                avatarUrl = image?.absUrl("src")?.takeIf(UrlPolicy::isInternal),
                k = counters.firstNotNullOfOrNull { K.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() },
                children = counters.firstOrNull { COUNT.matches(it) }?.toIntOrNull(),
            )
        }

    /** Texty `<span>` pred prvým `<br>`, napr. „1K“ a „2“. */
    private fun counters(line: List<Node>): List<String> =
        line.filterIsInstance<Element>().filter { it.normalName() == "span" }.map { it.text().trim() }
}
