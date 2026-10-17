package sk.ntnlabs.kyberia.data

import java.text.Collator
import java.text.Normalizer
import java.util.Locale
import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.BookmarksParser

/** Hľadanie bez ohľadu na diakritiku a veľkosť písmen: „ukaz“ nájde „ukáž“. */
object TextSearch {

    private val MARKS = Regex("""\p{Mn}+""")

    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(MARKS, "").lowercase(Locale.ROOT)

    fun matches(text: String, query: String): Boolean =
        query.isBlank() || normalize(text).contains(normalize(query.trim()))
}

/** Čo z bookmarkov ukázať a v akom poradí. */
object BookmarkFilter {

    private val SLOVAK = Locale.forLanguageTag("sk")

    fun apply(
        bookmarks: List<Bookmark>,
        query: String,
        onlyNews: Boolean,
        sort: BookmarkSort,
    ): List<Bookmark> {
        val filtered = bookmarks.filter { (!onlyNews || it.hasNews) && TextSearch.matches(it.title, query) }
        return when (sort) {
            BookmarkSort.ALPHABET -> {
                // Slovenské radenie: á za a, č za c, ch za h; veľkosť písmen nerozhoduje.
                val collator = Collator.getInstance(SLOVAK).apply { strength = Collator.SECONDARY }
                filtered.sortedWith(compareBy(collator) { it.title })
            }
            BookmarkSort.NEWS -> BookmarksParser.sortByNews(filtered)
        }
    }
}
