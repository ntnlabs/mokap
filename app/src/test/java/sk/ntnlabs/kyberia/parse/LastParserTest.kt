package sk.ntnlabs.kyberia.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LastParserTest {

    private val last = LastParser.parse(Fixtures.load("bookmarks.html", "https://kyberia.sk/id/19/"))

    @Test
    fun parsesAllItems() {
        assertEquals(listOf(8001L, 8002L), last.map { it.nodeId })
        assertEquals(listOf("alfa", "beta"), last.map { it.author })
        assertEquals(listOf(501L, 502L), last.map { it.authorId })
    }

    @Test
    fun postWithParentTimeAndSnippet() {
        val post = last[0]
        assertEquals("Nový príspevok o vlakoch", post.title)
        assertEquals(7000L, post.parentId)
        assertEquals("nejaké fórum", post.parentTitle)
        assertEquals("23:03:51", post.time)
        assertEquals("prvý riadok druhý riadok <3", post.snippet)
        assertEquals("https://kyberia.sk/images/nodes/5/0/501.gif", post.avatarUrl)
    }

    @Test
    fun replyInThread() {
        val post = last[1]
        assertEquals("Re[2]: 16.10.2026-18:28:37", post.title)
        assertEquals(8000L, post.parentId)
        assertEquals("22:46:47", post.time)
        assertEquals("krátka odpoveď", post.snippet)
    }

    @Test
    fun pageWithoutBlockGivesEmptyList() {
        assertTrue(LastParser.parse(Fixtures.load("login.html", "https://kyberia.sk/id/19/")).isEmpty())
    }
}
