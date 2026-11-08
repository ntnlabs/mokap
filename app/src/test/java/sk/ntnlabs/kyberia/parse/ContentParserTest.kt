package sk.ntnlabs.kyberia.parse

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentParserTest {

    private fun parse(html: String) = ContentParser.parse(Jsoup.parse(html, "https://kyberia.sk/id/1/").body())

    private fun List<ContentBlock>.texts() = filterIsInstance<ContentBlock.Text>().map { b -> b.spans.joinToString("") { it.text } }

    @Test
    fun topicFromFixture() {
        val topic = NodeParser.parse(Fixtures.load("node.html", "https://kyberia.sk/id/5001/"), 5001).content
        val texts = topic.texts()
        assertEquals("Vlaky a cestovanie\n\nTu sa píše o vlakoch, staniciach a meškaní.\nPravidlá sú na tejto stránke a mapa na example.com.\n\nzlý odkaz", texts[0])
        assertEquals("Koniec.", texts.last())
        // Obrázok z kyberie sa zobrazí, cudzí len ako odkaz.
        assertEquals(
            listOf(
                ContentBlock.Image("https://kyberia.sk/images/nodes/5/0/5001-foto.jpg", internal = true),
                ContentBlock.Image("https://example.org/obrazok.jpg", internal = false),
            ),
            topic.filterIsInstance<ContentBlock.Image>(),
        )
    }

    @Test
    fun formattingAndLinks() {
        val spans = (parse("""Tu je <b>tučné</b>, <i>šikmé</i> a <a href="/id/42">odkaz</a>.""").single() as ContentBlock.Text).spans
        assertEquals(TextSpan("Tu je "), spans[0])
        assertEquals(TextSpan("tučné", bold = true), spans[1])
        assertEquals(TextSpan("šikmé", italic = true), spans[3])
        assertEquals(TextSpan("odkaz", link = "https://kyberia.sk/id/42"), spans[5])
    }

    @Test
    fun dangerousLinksAndScriptsDropped() {
        val blocks = parse("""<a href="javascript:alert(1)">klik</a><script>zle()</script><img src="data:image/png;base64,AAA">""")
        val text = blocks.single() as ContentBlock.Text
        assertEquals(listOf(TextSpan("klik")), text.spans)
    }

    @Test
    fun whitespaceAndBreaks() {
        assertEquals(listOf("a b\nc\n\nd"), parse("  a   \n b<br>c<br><br><br><br>d  ").texts())
        assertEquals(listOf("• jeden\n• dva"), parse("<ul><li>jeden</li><li>dva</li></ul>").texts())
    }

    @Test
    fun skipPredicate() {
        val blocks = ContentParser.parse(Jsoup.parse("""text<small class="mood">nálada</small>""").body()) { it.hasClass("mood") }
        assertEquals(listOf("text"), blocks.texts())
        assertTrue(parse("").isEmpty())
    }
}
