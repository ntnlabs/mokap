package sk.ntnlabs.kyberia.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.Reply

class NewsTest {

    private fun bookmark(id: Long, newChildren: Int) =
        Bookmark(id, "b$id", newChildren, newDescendants = false, changed = false)

    private fun reply(authorId: Long, nodeId: Long, snippet: String) =
        Reply(authorId, "u$authorId", nodeId, "n$nodeId", snippet)

    @Test
    fun bookmarksReportOnlyGrowth() {
        val previous = mapOf(1L to 2, 2L to 5)
        val current = listOf(bookmark(1, 4), bookmark(2, 1), bookmark(3, 7), bookmark(4, 0))
        val increases = BookmarkNews.increases(previous, current)
        assertEquals(listOf(3L to 7, 1L to 2), increases.map { it.bookmark.id to it.added })
    }

    @Test
    fun mailReportsOnlyGrowth() {
        assertEquals(2, MailNews.added(previousUnread = 1, unread = 3))
        assertEquals(0, MailNews.added(previousUnread = 3, unread = 0))
        assertEquals(0, MailNews.added(previousUnread = 2, unread = 2))
    }

    @Test
    fun bookmarksNothingNewMeansEmpty() {
        val current = listOf(bookmark(1, 2))
        assertTrue(BookmarkNews.increases(BookmarkNews.snapshot(current), current).isEmpty())
    }

    @Test
    fun bookmarksSnapshotRoundTrip() {
        val snapshot = BookmarkNews.snapshot(listOf(bookmark(1, 3), bookmark(2, 0), bookmark(9, 12)))
        assertEquals(mapOf(1L to 3, 9L to 12), snapshot)
        assertEquals(snapshot, BookmarkNews.decode(BookmarkNews.encode(snapshot)))
    }

    @Test
    fun bookmarksDecodeSkipsGarbage() {
        assertEquals(mapOf(5L to 1), BookmarkNews.decode("5:1,x:2,7,8:y,"))
        assertEquals(emptyMap<Long, Int>(), BookmarkNews.decode(""))
    }

    @Test
    fun repliesReportOnlyUnseen() {
        val old = reply(1, 100, "staré")
        val new = reply(2, 100, "nové")
        val fresh = ReplyNews.fresh(setOf(old.key), listOf(new, old), selfId = 9)
        assertEquals(listOf(new), fresh)
    }

    @Test
    fun repliesSkipMyOwn() {
        val mine = reply(9, 100, "sám sebe")
        assertTrue(ReplyNews.fresh(emptySet(), listOf(mine), selfId = 9).isEmpty())
    }

    @Test
    fun repliesKeysRoundTrip() {
        val keys = listOf(reply(1, 2, "a b"), reply(3, 4, "c\nd")).map { it.key }
        assertEquals(keys.toSet(), ReplyNews.decode(ReplyNews.encode(keys)))
    }
}
