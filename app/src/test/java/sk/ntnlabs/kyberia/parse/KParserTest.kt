package sk.ntnlabs.kyberia.parse

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KParserTest {

    @Test
    fun myKFromBookmarksModule() {
        // Formát podľa šablóny kyberie (get_temp_k): „N k“, odkaz na príspevok, celkové K, <br>.
        val html = """
            <div class="module module_your_k_nodes">
              <form method="post">your K nodes for last: <input name="interval" value="1"><input type="submit" name="template_event" value="Hour"></form>
              <span class="most_important">2 k</span> <a href="/id/5101/">Môj príspevok o vlakoch</a> 14k <br />
              <span class="most_important">1 k</span> <a href="/id/5102/">Druhý</a> 1k <br />
            </div>
        """
        val myK = MyKParser.parse(Jsoup.parse(html, "https://kyberia.sk/id/19/"))
        assertEquals(
            listOf(MyK(5101, "Môj príspevok o vlakoch", added = 2, total = 14), MyK(5102, "Druhý", added = 1, total = 1)),
            myK,
        )
    }

    @Test
    fun noKInLastHour() {
        val empty = """<div class="module module_your_k_nodes"><form method="post">your K nodes for last: <input name="interval" value="1"></form></div>"""
        assertTrue(MyKParser.parse(Jsoup.parse(empty)).isEmpty())
        assertTrue(MyKParser.parse(Fixtures.load("bookmarks.html", "https://kyberia.sk/id/19/")).isEmpty())
    }

    @Test
    fun intervalRequestLikeWeb() {
        // Ako „Hour“ na webe: bez event – kyberia len prekreslí /id/19/ s iným intervalom.
        assertEquals(
            listOf("anticsrf" to "tok", "interval" to "24", "template_event" to "Hour"),
            MyKParser.fields(24, "tok"),
        )
        assertEquals("test-token", MyKParser.token(Fixtures.load("node.html", "https://kyberia.sk/id/5001/")))
        assertEquals(null, MyKParser.token(Jsoup.parse("<p>bez formulára</p>")))
    }

    @Test
    fun topKItems() {
        val items = TopKParser.parse(Fixtures.load("topk.html", "https://kyberia.sk/id/8340566/"))
        assertEquals(listOf(7001L, 7002L), items.map { it.id })
        val first = items[0]
        assertEquals("Pekný výhľad", first.title)
        assertEquals(7000L, first.parentId)
        assertEquals("Fotky z výletov", first.parentTitle)
        assertEquals(601L, first.authorId)
        assertEquals("alfa", first.author)
        assertEquals("https://kyberia.sk/images/nodes/6/0/601.gif", first.avatarUrl)
        assertEquals("07.11.2026 - 16:08:14", first.created)
        assertEquals(24, first.k)
        assertEquals(3, first.children)
        assertEquals(ContentBlock.Image("https://example.org/vyhlad.jpg", internal = false), first.content.last())

        val second = items[1]
        assertEquals(1, second.k)
        assertEquals(0, second.children)
        assertEquals(ContentBlock.Embed("https://www.youtube-nocookie.com/embed/abc123XYZ"), second.content.single())
    }

    @Test
    fun topKOnOtherPageIsEmpty() {
        assertTrue(TopKParser.parse(Fixtures.load("node.html", "https://kyberia.sk/id/5001/")).isEmpty())
    }
}
