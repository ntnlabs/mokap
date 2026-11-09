package sk.ntnlabs.kyberia.parse

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NodeActionsTest {

    private val node = Fixtures.load("node.html", "https://kyberia.sk/id/5001/")

    @Test
    fun kTargetsFromNodePage() {
        val page = NodeParser.parse(node, 5001)
        assertEquals(3, page.templateId)
        val forNode = requireNotNull(page.kNode)
        assertEquals("https://kyberia.sk/id/5001/", forNode.action)
        assertFalse(forNode.multipart)
        assertFalse("token nesmie byť vo výpise", forNode.toString().contains("test-token"))
        val forChild = requireNotNull(page.kChild)
        assertTrue(forChild.multipart)
        assertEquals("test-token", forChild.anticsrf)
    }

    @Test
    fun kAlreadyGivenShowsRevoke() {
        assertFalse(NodeParser.parse(node, 5001).kGiven)
        // Po K živá kyberia namiesto „K+❤️“ ukáže „-❤️“ (event=revoKe) – K už je od teba, znova sa neponúkne.
        val given = Jsoup.parse(
            """<span class='node_info2'><br /><span class='most_important'>1 <a href=/id/900>❤️</a></span>
               <form action="/id/5001/" method="post"><input type="hidden" name="anticsrf" value="tok" />
               <button type="submit" name="event" value="revoKe"><span></span>-❤️</button>
               <input type="submit" name="event" value="book" /></form></span>
               <form name='formular' action='/id/5001/'><button type="submit" name="event" value="K">K+❤️</button></form>""",
            "https://kyberia.sk/id/5001/",
        )
        val page = NodeParser.parse(given, 5001)
        assertTrue(page.kGiven)
        assertNull(page.kNode)
        // K pre odpovede ostáva – stránka pri nich neukazuje, či si K dal.
        assertTrue(page.kChild != null)
    }

    @Test
    fun exactlyOneK() {
        val target = PostTarget("https://kyberia.sk/id/5001/", "tok", multipart = true)
        assertEquals(listOf("anticsrf" to "tok", "event" to "K"), NodeActionsParser.kFields(target, null))
        assertEquals(
            listOf("anticsrf" to "tok", "node_chosen[]" to "5101", "event" to "K"),
            NodeActionsParser.kFields(target, 5101),
        )
    }

    @Test
    fun noKWithoutButton() {
        val page = Jsoup.parse("<form name='formular' action='/id/5001/'><textarea name='node_content'></textarea></form>", "https://kyberia.sk/id/5001/")
        assertNull(NodeActionsParser.kNode(page))
        assertNull(NodeActionsParser.kChild(page))
    }

    @Test
    fun editFormFromConfigurePage() {
        // Token je len v hlavičke stránky – na stránke je jeden pre celú session.
        val conf = Jsoup.parse(
            """<form method='post' action='/id/5101/'><input type='hidden' name='anticsrf' value='tok'>
               <button name='event' value='logout'>logout</button></form>
               <form method='post' enctype='multipart/form-data' action='/id/5101/'>
               <input type='text' name='node_name' value='Re: Vlaky'><input type='submit' name='event' value='configure_node_name'></form>
               <form method='post' enctype='multipart/form-data' action='/id/5101/'>
               <textarea class=node_content name='node_content'>Ahoj &lt;b&gt;svet&lt;/b&gt;
druhý riadok</textarea>
               <input type='submit' name='event' value='configure_content'>
               <input type='checkbox' name='no_html' value='yes'><input type='checkbox' name='code' value='yes'></form>""",
            "https://kyberia.sk/id/5101/1961033",
        )
        val form = requireNotNull(NodeActionsParser.edit(conf))
        assertEquals("https://kyberia.sk/id/5101/", form.target.action)
        assertEquals("tok", form.target.anticsrf)
        assertTrue(form.target.multipart)
        assertEquals("Ahoj <b>svet</b>\ndruhý riadok", form.content)
        assertEquals(
            listOf("anticsrf" to "tok", "node_content" to "nový", "event" to "configure_content"),
            NodeActionsParser.editFields(form, "nový"),
        )
    }

    @Test
    fun noEditFormOnForeignNode() {
        assertNull(NodeActionsParser.edit(node))
    }

    @Test
    fun pageError() {
        assertEquals("datanode uz si k udelil", PageError.of(Jsoup.parse("<center><span class='most_important'>datanode uz si k udelil</span></center>")))
        assertNull(PageError.of(node))
    }
}
