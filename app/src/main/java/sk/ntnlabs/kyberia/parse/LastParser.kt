package sk.ntnlabs.kyberia.parse

import org.jsoup.nodes.Document
import org.jsoup.nodes.TextNode
import sk.ntnlabs.kyberia.UrlPolicy

/** Najnovší príspevok na celej kyberii (blok „Last“ na /id/19/). */
data class LastPost(
    val nodeId: Long,
    val title: String,
    val authorId: Long?,
    val author: String,
    val avatarUrl: String?,
    /** Kam príspevok patrí (rodičovský node), ak ho kyberia uvádza. */
    val parentId: Long?,
    val parentTitle: String?,
    /** Čas tak, ako ho ukazuje kyberia (napr. „23:03:51“). */
    val time: String?,
    val snippet: String,
)

/**
 * Každá položka je `div.last`:
 * `<a href=/id/AUTOR><img title="nick | čas - dátum"></a> <a href=/id/NODE>názov</a> in<br>
 *  <a href=/id/RODIČ>názov</a> čas<br> úryvok`
 */
object LastParser {

    private val TIME = Regex("""\b\d{1,2}:\d{2}(:\d{2})?\b""")

    fun parse(document: Document): List<LastPost> =
        document.select("div.last").mapNotNull { item ->
            val links = item.children().filter { it.normalName() == "a" }
            val avatar = links.firstOrNull { it.selectFirst("img") != null }
            val texts = links.filter { it.selectFirst("img") == null }
            val node = texts.firstOrNull() ?: return@mapNotNull null
            val nodeId = NodeText.nodeId(node.attr("href")) ?: return@mapNotNull null
            val parent = texts.getOrNull(1)
            val image = avatar?.selectFirst("img")
            val imageTitle = image?.attr("title").orEmpty()
            val lines = NodeText.lines(item)
            LastPost(
                nodeId = nodeId,
                title = node.attr("title").ifBlank { node.text() }.trim(),
                authorId = avatar?.attr("href")?.let(NodeText::nodeId),
                author = imageTitle.substringBefore(" | ").trim().ifEmpty { image?.attr("alt").orEmpty() },
                avatarUrl = image?.absUrl("src")?.takeIf(UrlPolicy::isInternal),
                parentId = parent?.attr("href")?.let(NodeText::nodeId),
                parentTitle = parent?.let { it.attr("title").ifBlank { it.text() }.trim() }?.ifEmpty { null },
                // Len voľný text riadku – názov rodiča („Re: 16.10.2026-18:28:37“) tiež obsahuje čas.
                time = lines.getOrNull(1)?.let { TIME.find(NodeText.join(it.filterIsInstance<TextNode>()))?.value }
                    ?: TIME.find(imageTitle)?.value,
                snippet = NodeText.join(lines.drop(2).flatten()),
            )
        }
}
