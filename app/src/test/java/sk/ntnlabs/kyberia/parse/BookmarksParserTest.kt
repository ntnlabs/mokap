package sk.ntnlabs.kyberia.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarksParserTest {

    private val bookmarks = BookmarksParser.parse(Fixtures.load("bookmarks.html", "https://kyberia.sk/id/19/"))

    @Test
    fun parsesAllEntries() {
        assertEquals(listOf(1001L, 1002L, 1003L, 1004L, 1005L, 1006L), bookmarks.map { it.id })
    }

    @Test
    fun titlesAreTextWithoutMarkup() {
        assertEquals("Tiché fórum", bookmarks[0].title)
        assertEquals(".:.HUDOBNÉ VLÁKNO.:.", bookmarks[2].title)
        assertEquals("veľké vlákno", bookmarks[4].title)
    }

    @Test
    fun readsNewsFlags() {
        val quiet = bookmarks[0]
        assertEquals(0, quiet.newChildren)
        assertFalse(quiet.hasNews)

        val news = bookmarks[1]
        assertEquals(2, news.newChildren)
        assertTrue(news.newDescendants)
        assertFalse(news.changed)

        assertTrue(bookmarks[2].newDescendants)
        assertEquals(0, bookmarks[2].newChildren)

        assertTrue(bookmarks[3].changed)
        assertFalse(bookmarks[3].newDescendants)

        val big = bookmarks[4]
        assertEquals(256, big.newChildren)
        assertTrue(big.newDescendants && big.changed)
    }

    @Test
    fun sortsByNewsFirst() {
        val sorted = BookmarksParser.sortByNews(bookmarks).map { it.id }
        assertEquals(listOf(1005L, 1002L, 1003L, 1004L, 1006L, 1001L), sorted)
    }
}
