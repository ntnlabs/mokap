package sk.ntnlabs.kyberia.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sk.ntnlabs.kyberia.KyberiaUrls

class NodeParserTest {

    private val page = NodeParser.parse(Fixtures.load("node.html", "https://kyberia.sk/id/5001/"), 5001)

    @Test
    fun headerFromTitleAndSidebar() {
        assertEquals(5001L, page.id)
        assertEquals("Vlaky a cestovanie", page.title)
        // „viewed by“ má v kyberii tiež id sidebar_owner – vlastník je len riadok „owner:“.
        assertEquals(601L, page.ownerId)
        assertEquals("alfa", page.owner)
        assertEquals("02.07.2018 - 19:48:06", page.created)
        assertEquals(5000L, page.parentId)
        assertEquals("Doprava", page.parentTitle)
        assertEquals(12, page.k)
        assertEquals(45, page.childrenTotal)
    }

    @Test
    fun childrenKeepThreadOrderAndDepth() {
        assertEquals(listOf(5101L, 5102L, 5103L), page.children.map { it.id })
        assertEquals(listOf(0, 1, 0), page.children.map { it.depth })
        assertEquals(listOf("beta", "alfa", "gama"), page.children.map { it.author })
        assertEquals(listOf(602L, 601L, 603L), page.children.map { it.authorId })
    }

    @Test
    fun childHeaderDetails() {
        val first = page.children[0]
        assertEquals("Re: Vlaky a cestovanie", first.title)
        assertEquals("06.11.2026 - 13:06:35", first.created)
        assertEquals(3, first.k)
        assertFalse(first.isNew)
        assertEquals("https://kyberia.sk/images/nodes/6/0/602.gif", first.avatarUrl)

        val reply = page.children[1]
        assertTrue(reply.isNew)
        assertNull(reply.k)
        // Avatar mimo kyberie sa nesťahuje.
        assertNull(page.children[2].avatarUrl)
    }

    @Test
    fun childBodyWithoutMoodAndWithEmbed() {
        val blocks = page.children[0].content
        assertEquals(ContentBlock.Embed("https://www.youtube-nocookie.com/embed/abc123"), blocks[1])
        val text = blocks.filterIsInstance<ContentBlock.Text>().joinToString(" ") { b -> b.spans.joinToString("") { it.text } }
        assertEquals("Pozrite si toto video:", text)
        // http:// odkaz na kyberiu ide ako https.
        val link = (page.children[1].content.single() as ContentBlock.Text).spans.first { it.link != null }
        assertEquals("https://kyberia.sk/id/5101", link.link)
    }

    @Test
    fun avatarUrlFromUserId() {
        assertEquals("https://kyberia.sk/images/nodes/5/5/5551234.gif", KyberiaUrls.avatar(5551234))
        assertEquals("https://kyberia.sk/images/nodes/9/8/987.gif", KyberiaUrls.avatar(987))
        assertNull(KyberiaUrls.avatar(7))
    }

    @Test
    fun pageWithoutThreadStillParses() {
        val login = NodeParser.parse(Fixtures.load("login.html", "https://kyberia.sk/id/19/"), 19)
        assertTrue(login.children.isEmpty())
        assertNull(login.owner)
    }
}
