package sk.ntnlabs.kyberia.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sk.ntnlabs.kyberia.parse.Bookmark

class BookmarkNewsTest {

    private fun bookmark(id: Long, newChildren: Int) =
        Bookmark(id, "b$id", newChildren, newDescendants = false, changed = false)

    @Test
    fun reportsOnlyGrowth() {
        val previous = mapOf(1L to 2, 2L to 5)
        val current = listOf(bookmark(1, 4), bookmark(2, 1), bookmark(3, 7), bookmark(4, 0))
        val increases = BookmarkNews.increases(previous, current)
        assertEquals(listOf(3L to 7, 1L to 2), increases.map { it.bookmark.id to it.added })
    }

    @Test
    fun nothingNewMeansEmpty() {
        val current = listOf(bookmark(1, 2))
        assertTrue(BookmarkNews.increases(BookmarkNews.snapshot(current), current).isEmpty())
    }

    @Test
    fun snapshotRoundTrip() {
        val snapshot = BookmarkNews.snapshot(listOf(bookmark(1, 3), bookmark(2, 0), bookmark(9, 12)))
        assertEquals(mapOf(1L to 3, 9L to 12), snapshot)
        assertEquals(snapshot, BookmarkNews.decode(BookmarkNews.encode(snapshot)))
    }

    @Test
    fun decodeSkipsGarbage() {
        assertEquals(mapOf(5L to 1), BookmarkNews.decode("5:1,x:2,7,8:y,"))
        assertEquals(emptyMap<Long, Int>(), BookmarkNews.decode(""))
    }
}
