package sk.ntnlabs.kyberia.parse

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyFormTest {

    private val node = Fixtures.load("node.html", "https://kyberia.sk/id/5001/")

    @Test
    fun formFromNodePage() {
        val form = requireNotNull(ReplyFormParser.parse(node))
        assertEquals("https://kyberia.sk/id/5001/", form.action)
        assertEquals("test-token", form.anticsrf)
        assertEquals("5001", form.parent)
        assertEquals("4", form.templateId)
        assertTrue(form.multipart)
        assertFalse("token nesmie byť vo výpise", form.toString().contains("test-token"))
        assertTrue(NodeParser.parse(node, 5001).replyForm != null)
    }

    @Test
    fun noFormWithoutWriteAccess() {
        // Bez práva zápisu kyberia textarea nevykreslí – ostatné tlačidlá formulára áno.
        val readOnly = Jsoup.parse(
            """<form action='/id/5001/' method='post' name='formular'><input type='hidden' name='anticsrf' value='t'>
               <input type='submit' name='event' value='K'></form>""",
            "https://kyberia.sk/id/5001/",
        )
        assertNull(ReplyFormParser.parse(readOnly))
        assertNull(NodeParser.parse(readOnly, 5001).replyForm)
        assertNull(ReplyFormParser.parse(Fixtures.load("topk.html", "https://kyberia.sk/id/8340566/")))
    }

    @Test
    fun onlyAddFieldsAreSent() {
        // Živý formulár má aj tlačidlá (K, put…), výber bookmarkov a node_chosen[] – nič z toho sa nesmie poslať.
        val page = Jsoup.parse(
            """<form enctype='multipart/form-data' action='/id/5001/' method='post' name='formular'>
               <input type='hidden' name='anticsrf' value='tok'>
               <input type='text' name='node_name' value=''><textarea name='node_content'></textarea>
               <input type='submit' name='event' value='add'><input type='text' name='template_id' value='4'>
               <input type='hidden' name='node_parent' value='5001'>
               <select name='nodeshell_id_select'><option value='42'>bookmark</option></select>
               <input type='hidden' name='event' value='K'>
               <input type='checkbox' name='node_chosen[]' value='5101'></form>""",
            "https://kyberia.sk/id/5001/",
        )
        val form = requireNotNull(ReplyFormParser.parse(page))
        assertEquals(
            listOf(
                "anticsrf" to "tok",
                "node_name" to "",
                "node_content" to "Ahoj\n<b>svet</b>",
                "template_id" to "4",
                "node_parent" to "5001",
                "event" to "add",
            ),
            ReplyFormParser.fields(form, "Ahoj\n<b>svet</b>", replyTo = null, noHtml = false),
        )
        val reply = ReplyFormParser.fields(form, "x", replyTo = 5101, noHtml = true)
        assertEquals(listOf("5101"), reply.filter { it.first == "node_chosen[]" }.map { it.second })
        assertEquals(listOf("yes"), reply.filter { it.first == "no_html" }.map { it.second })
        assertEquals(listOf("add"), reply.filter { it.first == "event" }.map { it.second })
    }

    @Test
    fun errorFromHeader() {
        val rejected = Jsoup.parse("<center><span class='most_important'>Zakazany tag &lt;script&gt;!</span></center>")
        assertEquals("Zakazany tag <script>!", ReplyFormParser.error(rejected))
        // K a iné „most_important“ mimo hlavičky nie sú chyba.
        assertNull(ReplyFormParser.error(node))
    }
}
