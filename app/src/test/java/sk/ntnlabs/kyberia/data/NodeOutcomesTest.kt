package sk.ntnlabs.kyberia.data

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sk.ntnlabs.kyberia.parse.NodeParser

class NodeOutcomesTest {

    private val url = "https://kyberia.sk/id/5001/"
    private val html = requireNotNull(javaClass.getResourceAsStream("/fixtures/node.html")).use { it.readBytes().toString(Charsets.UTF_8) }
    private val before = NodeParser.parse(Jsoup.parse(html, url), 5001)

    @Test
    fun kForNode() {
        val after = Jsoup.parse(html.replace("<span class='most_important'>12 ", "<span class='most_important'>13 "), url)
        assertTrue(NodeOutcomes.k(before, null, after) is KResult.Given)
        assertTrue(NodeOutcomes.k(before, null, Jsoup.parse(html, url)) is KResult.Unconfirmed)
    }

    @Test
    fun kForChild() {
        assertEquals(3, before.children.first { it.id == 5101L }.k)
        val after = Jsoup.parse(html.replace("<span class='node_header_k'>3K</span>", "<span class='node_header_k'>4K</span>"), url)
        assertTrue(NodeOutcomes.k(before, 5101, after) is KResult.Given)
        // K pre node nemení K odpovede.
        assertTrue(NodeOutcomes.k(before, 5101, Jsoup.parse(html.replace("most_important'>12 ", "most_important'>13 "), url)) is KResult.Unconfirmed)
    }

    @Test
    fun kRejected() {
        val page = Jsoup.parse(html.replace("<body>", "<body><center><span class='most_important'>nemas k</span></center>"), url)
        assertEquals("nemas k", (NodeOutcomes.k(before, null, page) as KResult.Rejected).message)
    }
}
