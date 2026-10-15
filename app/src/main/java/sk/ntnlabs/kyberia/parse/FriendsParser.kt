package sk.ntnlabs.kyberia.parse

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import sk.ntnlabs.kyberia.UrlPolicy

/** Priateľ, ktorý je práve na kyberii (blok „Friends“ na /id/19/). */
data class Friend(
    val id: Long,
    val nick: String,
    val avatarUrl: String?,
    /** Node, kde sa práve nachádza (napr. pošta). */
    val locationId: Long?,
    val locationTitle: String?,
    /** Čas tak, ako ho ukazuje kyberia (napr. „84m39s“). */
    val time: String,
    val timeSeconds: Long?,
)

/**
 * Každý priateľ je `div.active_friend`:
 * `<a href=/id/ID/ title=nick><img src=avatar></a> [<a href=/id/KDE title=…>kde</a>] <br> 84m39s | nick…`
 */
object FriendsParser {

    private val NODE_ID = Regex("""/id/(\d+)""")
    private val TIME = Regex("""(?:\d+[dhms])+""")
    private val TIME_PART = Regex("""(\d+)([dhms])""")

    fun parse(document: Document): List<Friend> =
        document.select("div.active_friend").mapNotNull { item ->
            val links = item.children().filter { it.normalName() == "a" }
            val profile = links.firstOrNull { it.selectFirst("img") != null } ?: return@mapNotNull null
            val id = nodeId(profile.attr("href")) ?: return@mapNotNull null
            val image = profile.selectFirst("img")
            val location = links.firstOrNull { it !== profile }
            val time = TIME.find(textAfterBreak(item))?.value.orEmpty()
            Friend(
                id = id,
                nick = profile.attr("title").ifEmpty { image?.attr("alt").orEmpty() },
                avatarUrl = image?.absUrl("src")?.takeIf(UrlPolicy::isInternal),
                locationId = location?.attr("href")?.let(::nodeId),
                locationTitle = location?.let { it.attr("title").ifEmpty { it.text() } },
                time = time,
                timeSeconds = seconds(time),
            )
        }

    /** „1h2m3s“ → 3723; prázdne alebo neznáme → null. */
    fun seconds(time: String): Long? {
        val parts = TIME_PART.findAll(time).toList()
        if (parts.isEmpty()) return null
        return parts.sumOf { match ->
            val value = match.groupValues[1].toLong()
            when (match.groupValues[2]) {
                "d" -> value * 86_400
                "h" -> value * 3_600
                "m" -> value * 60
                else -> value
            }
        }
    }

    private fun nodeId(href: String): Long? = NODE_ID.find(href)?.groupValues?.get(1)?.toLongOrNull()

    private fun textAfterBreak(item: Element): String {
        val nodes = item.childNodes()
        val br = nodes.indexOfFirst { it is Element && it.normalName() == "br" }
        return nodes.drop(br + 1).filterIsInstance<TextNode>().joinToString(" ") { it.text() }
    }
}
