package sk.ntnlabs.kyberia.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.LastPost
import sk.ntnlabs.kyberia.parse.Reply

class TabChangesTest {

    private fun bookmark(id: Long, newChildren: Int) = Bookmark(id, "b$id", newChildren, newDescendants = false, changed = false)

    private fun last(id: Long) = LastPost(id, "t$id", null, "a", null, null, null, null, "s")

    @Test
    fun bookmarksHighlightOnlyGrowth() {
        val seen = mapOf(1L to 2, 2L to 5)
        assertFalse(TabChanges.bookmarksChanged(seen, listOf(bookmark(1, 2), bookmark(2, 3))))
        assertTrue(TabChanges.bookmarksChanged(seen, listOf(bookmark(1, 3))))
        assertTrue(TabChanges.bookmarksChanged(seen, listOf(bookmark(9, 1))))
    }

    @Test
    fun readingOnWebLowersBaseline() {
        // Videl si 5, na webe si prečítal (2), potom pribudol 1 → 3 je novinka, hoci je menej ako 5.
        val lowered = TabChanges.lowerBookmarkBaseline(mapOf(2L to 5, 3L to 1), listOf(bookmark(2, 2)))
        assertEquals(mapOf(2L to 2), lowered)
        assertTrue(TabChanges.bookmarksChanged(lowered, listOf(bookmark(2, 3))))
    }

    @Test
    fun repliesAndLastHighlightUnseenItems() {
        val reply = Reply(1, "u1", 10, "n10", "ahoj")
        assertFalse(TabChanges.repliesChanged(setOf(reply.key), listOf(reply)))
        assertTrue(TabChanges.repliesChanged(emptySet(), listOf(reply)))
        assertFalse(TabChanges.lastChanged(setOf(1L, 2L), listOf(last(1), last(2))))
        assertTrue(TabChanges.lastChanged(setOf(1L), listOf(last(1), last(3))))
    }
}
