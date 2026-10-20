package sk.ntnlabs.kyberia.parse

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepliesParserTest {

    private val replies = RepliesParser.parse(Fixtures.load("bookmarks.html", "https://kyberia.sk/id/19/"))

    @Test
    fun parsesAllItems() {
        assertEquals(3, replies.size)
        assertEquals(listOf("alfa", "beta", "ja"), replies.map { it.author })
        assertEquals(listOf(501L, 502L, 4242L), replies.map { it.authorId })
    }

    @Test
    fun avatarIsAbsoluteKyberiaUrl() {
        assertEquals("https://kyberia.sk/images/nodes/5/0/501.gif", replies[0].avatarUrl)
    }

    @Test
    fun linkPointsToMyPostWithFullTitle() {
        val first = replies[0]
        assertEquals(2001L, first.nodeId)
        assertEquals("Re[2]: Dlhý názov môjho príspevku o vlakoch", first.nodeTitle)
    }

    @Test
    fun snippetIsTextAfterBreakWithoutCounters() {
        assertEquals("súhlasím, ale vlaky meškajú", replies[0].snippet)
        assertTrue(replies[1].snippet.startsWith("image:"))
    }

    @Test
    fun kAndChildCountersAreSeparate() {
        assertEquals(listOf(null, 3, null), replies.map { it.k })
        assertEquals(listOf(2, null, null), replies.map { it.children })
    }

    @Test
    fun sameParentDifferentAuthorsHaveDifferentKeys() {
        assertEquals(replies[0].nodeId, replies[2].nodeId)
        assertNotEquals(replies[0].key, replies[2].key)
    }

    @Test
    fun newMarkerIsNotMyPost() {
        // Tá istá odpoveď pri dvoch načítaniach: prvýkrát so značkou „NEW“, potom bez nej.
        fun page(marker: String) = RepliesParser.parse(
            Jsoup.parse(
                """<div class="user_sub_child">
                  <a href="/id/503/" title=""><img alt="gama" title="gama" src="/images/nodes/5/0/503.gif"></a>
                  $marker
                  <a href="/id/2003" title="Môj príspevok">Môj príspevok</a>
                  [<span class="most_important">1K</span>] <br />&nbsp;čerstvá odpoveď
                </div>""",
                "https://kyberia.sk/id/19/",
            ),
        ).single()

        val fresh = page("""<a class="most_important" href="/id/3579407/" style="color:#ff0000">NEW</a>:""")
        val seen = page("")
        assertEquals(2003L, fresh.nodeId)
        assertEquals("Môj príspevok", fresh.nodeTitle)
        assertEquals(1, fresh.k)
        assertEquals(seen.key, fresh.key)
    }

    @Test
    fun pageWithoutBlockGivesEmptyList() {
        assertTrue(RepliesParser.parse(Fixtures.load("login.html", "https://kyberia.sk/id/19/")).isEmpty())
    }
}
