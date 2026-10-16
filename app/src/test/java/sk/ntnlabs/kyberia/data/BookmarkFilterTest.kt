package sk.ntnlabs.kyberia.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sk.ntnlabs.kyberia.parse.Bookmark

class BookmarkFilterTest {

    private fun bookmark(id: Long, title: String, newChildren: Int = 0) =
        Bookmark(id, title, newChildren, newDescendants = false, changed = false)

    private val bookmarks = listOf(
        bookmark(1, "Čaj o piatej", newChildren = 3),
        bookmark(2, "chata"),
        bookmark(3, "Cestovanie", newChildren = 10),
        bookmark(4, "hory"),
        bookmark(5, "ukáž mi ju"),
        bookmark(6, "Áno alebo nie"),
        bookmark(7, "auto"),
    )

    @Test
    fun alphabetUsesSlovakCollation() {
        val titles = BookmarkFilter.apply(bookmarks, "", onlyNews = false, sort = BookmarkSort.ALPHABET).map { it.title }
        assertEquals(listOf("Áno alebo nie", "auto", "Cestovanie", "Čaj o piatej", "hory", "chata", "ukáž mi ju"), titles)
    }

    @Test
    fun newsSortPutsMostActiveFirst() {
        val ids = BookmarkFilter.apply(bookmarks, "", onlyNews = false, sort = BookmarkSort.NEWS).map { it.id }
        assertEquals(listOf(3L, 1L), ids.take(2))
    }

    @Test
    fun onlyNewsHidesQuietBookmarks() {
        val ids = BookmarkFilter.apply(bookmarks, "", onlyNews = true, sort = BookmarkSort.ALPHABET).map { it.id }
        assertEquals(listOf(3L, 1L), ids)
    }

    @Test
    fun searchIgnoresDiacriticsAndCase() {
        assertEquals(listOf(5L), BookmarkFilter.apply(bookmarks, "UKAZ", false, BookmarkSort.ALPHABET).map { it.id })
        assertEquals(listOf(1L), BookmarkFilter.apply(bookmarks, "caj", false, BookmarkSort.ALPHABET).map { it.id })
        assertEquals(7, BookmarkFilter.apply(bookmarks, "  ", false, BookmarkSort.ALPHABET).size)
    }

    @Test
    fun textSearchBasics() {
        assertTrue(TextSearch.matches("Ťažký ŽIVOT", "tazky zivot"))
        assertFalse(TextSearch.matches("hory", "more"))
    }
}
