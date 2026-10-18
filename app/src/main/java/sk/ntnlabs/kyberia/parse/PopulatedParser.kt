package sk.ntnlabs.kyberia.parse

import org.jsoup.nodes.Document
import org.jsoup.parser.Parser

/** Node, kde sa práve zdržuje najviac ľudí. */
data class Hotspot(val nodeId: Long, val title: String, val people: Int)

data class Populated(
    val hotspots: List<Hotspot>,
    /** Koľko ľudí je vo verejných nodoch a koľko je online spolu – „(68/110)“ pod zoznamom. */
    val placed: Int? = null,
    val online: Int? = null,
)

/**
 * Stránka „most populated nodes“ (/id/4830026/) kreslí zoznam až JavaScriptom z poľa
 * `var tags = [[ID,'názov'],…]` – jedna položka za každého človeka. Tu sa len spočítajú.
 */
object PopulatedParser {

    private val TAGS = Regex("""var\s+tags\s*=\s*\[(.*?)\]\s*;""", RegexOption.DOT_MATCHES_ALL)
    private val ENTRY = Regex("""\[\s*(\d+)\s*,\s*'((?:[^'\\]|\\.)*)'\s*]""")
    private val ESCAPE = Regex("""\\(.)""")
    private val COUNTS = Regex("""\(\s*(\d+)\s*/\s*(\d+)\s*\)""")

    fun parse(document: Document): Populated {
        val tags = document.select("script").firstNotNullOfOrNull { TAGS.find(it.data())?.groupValues?.get(1) }.orEmpty()
        val entries = ENTRY.findAll(tags).mapNotNull { match ->
            val id = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            id to Parser.unescapeEntities(ESCAPE.replace(match.groupValues[2]) { it.groupValues[1] }, false).trim()
        }.toList()
        val hotspots = entries.groupBy({ it.first }, { it.second })
            .map { (id, titles) -> Hotspot(id, titles.first(), titles.size) }
            .sortedWith(compareByDescending<Hotspot> { it.people }.thenBy { it.title.lowercase() })
        val counts = document.select("small").firstNotNullOfOrNull { COUNTS.find(it.text()) }
        return Populated(
            hotspots = hotspots,
            placed = counts?.groupValues?.get(1)?.toIntOrNull(),
            online = counts?.groupValues?.get(2)?.toIntOrNull(),
        )
    }
}
