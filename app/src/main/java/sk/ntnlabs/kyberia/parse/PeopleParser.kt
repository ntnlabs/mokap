package sk.ntnlabs.kyberia.parse

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import sk.ntnlabs.kyberia.UrlPolicy

/**
 * Všetci online z /id/27/ (zoznam „users“). Každý je `div[data-idle]` s nickom v `td.important`,
 * avatarom, `[<a href=/id/KDE title=…>]` a nečinnosťou – `data-idle` je rovno v sekundách.
 */
object PeopleParser {

    private val IDLE_TEXT = Regex("""(\d+\s*(?:h|min|sec)\s*)+""")

    fun parse(document: Document): List<Friend> =
        usersList(document).select("div[data-idle]").mapNotNull { item ->
            val links = item.select("a[href]")
            val profile = links.firstOrNull { it.selectFirst("img") != null } ?: return@mapNotNull null
            val id = NodeText.nodeId(profile.attr("href")) ?: return@mapNotNull null
            val image = profile.selectFirst("img")
            val location = links.firstOrNull { it.selectFirst("img") == null }
            val idleCell = location?.parent()
            Friend(
                id = id,
                nick = item.selectFirst("td.important")?.text()?.trim()?.ifEmpty { null }
                    ?: image?.attr("name")?.ifEmpty { image.attr("alt") }.orEmpty(),
                avatarUrl = image?.absUrl("src")?.takeIf(UrlPolicy::isInternal),
                locationId = location?.attr("href")?.let(NodeText::nodeId),
                // Text odkazu býva skrátený, title má celý názov.
                locationTitle = location?.let { it.attr("title").ifBlank { it.text() }.trim() }?.ifEmpty { null },
                time = idleCell?.let { IDLE_TEXT.find(it.ownText())?.value?.trim() }.orEmpty(),
                timeSeconds = item.attr("data-idle").toLongOrNull(),
            )
        }.distinctBy { it.id }

    /** Zoznam za nadpisom „users“; priatelia majú vlastný zoznam vyššie a sú aj tu. */
    private fun usersList(document: Document): Element {
        val heading = document.select("b").firstOrNull { it.text().trim().equals("users", ignoreCase = true) }
        val list = heading?.nextElementSiblings()?.firstOrNull { it.hasClass("user-list") }
        return list ?: document.select("div.user-list").lastOrNull() ?: document
    }
}
