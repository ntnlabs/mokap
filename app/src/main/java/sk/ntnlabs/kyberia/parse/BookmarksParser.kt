package sk.ntnlabs.kyberia.parse

import org.jsoup.nodes.Document

data class Bookmark(
    val id: Long,
    val title: String,
    /** „N NEW Child“ – nové priame odpovede v node. */
    val newChildren: Int,
    /** „descdnt“ – niečo nové hlbšie vo vlákne. */
    val newDescendants: Boolean,
    /** „changed“ – zmenil sa samotný node. */
    val changed: Boolean,
) {
    val hasNews: Boolean get() = newChildren > 0 || newDescendants || changed
}

/**
 * Parser stránky /id/19/. Každý bookmark je:
 * `<input name='bookmarks_chosen[]' value=ID> <a href='/id/ID'>názov</a> :: <span>…</span> <br>`
 */
object BookmarksParser {

    private val NEW_CHILD = Regex("""(\d+)\s+NEW\s+Child""", RegexOption.IGNORE_CASE)

    fun parse(document: Document): List<Bookmark> =
        document.select("input[type=checkbox][name^=bookmarks_chosen]").mapNotNull { input ->
            val id = input.attr("value").trim().toLongOrNull() ?: return@mapNotNull null
            val link = input.nextElementSibling()?.takeIf { it.normalName() == "a" } ?: return@mapNotNull null
            var newChildren = 0
            var descendants = false
            var changed = false
            var el = link.nextElementSibling()
            while (el != null && el.normalName() != "br" && el.normalName() != "input") {
                if (el.normalName() == "span") {
                    val text = el.text()
                    NEW_CHILD.find(text)?.let { newChildren = it.groupValues[1].toInt() }
                    if (text.contains("descdnt", ignoreCase = true)) descendants = true
                    if (text.contains("changed", ignoreCase = true)) changed = true
                }
                el = el.nextElementSibling()
            }
            Bookmark(id, link.text().trim(), newChildren, descendants, changed)
        }.distinctBy { it.id }

    /**
     * Stránka so šablónou /id/19/ – blok Last (10 najnovších príspevkov celej kyberie) je tam vždy,
     * na iných stránkach (people, populated, bežný node) nie.
     */
    fun isBookmarksPage(document: Document): Boolean = document.selectFirst("div.last") != null

    /** Najprv najviac nových odpovedí, potom novinky hlbšie, potom zmenené, nakoniec zvyšok. */
    fun sortByNews(bookmarks: List<Bookmark>): List<Bookmark> =
        bookmarks.sortedWith(
            compareByDescending<Bookmark> { it.newChildren }
                .thenByDescending { it.newDescendants }
                .thenByDescending { it.changed }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
        )
}
