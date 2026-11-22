package sk.ntnlabs.kyberia.data

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sk.ntnlabs.kyberia.parse.NodeParser

class RepliesTest {

    private val url = "https://kyberia.sk/id/5001/"
    private val html = requireNotNull(javaClass.getResourceAsStream("/fixtures/node.html")).use { it.readBytes().toString(Charsets.UTF_8) }
    private val before = NodeParser.parse(Jsoup.parse(html, url), 5001)

    private fun withNewChild(authorId: Long) = withNewChildren(5999L to authorId)

    /** Nové odpovede navrchu vlákna: (ID, autor); s [conf] má odpoveď v hlavičke odkaz „conf“ (tvoja). */
    private fun withNewChildren(vararg children: Pair<Long, Long>, conf: Set<Long> = emptySet()) = Jsoup.parse(
        html.replace(
            "<ul class='node_children'>",
            "<ul class='node_children'>" + children.joinToString("") { (id, authorId) ->
                val confLink = if (id in conf) "<a href='/id/$id/4242'>conf</a>" else ""
                """
              <li id='sub$id' class='level1 lvl u$authorId'>
                <div class='node_content'>
                  <div class='node_header'>
                    <a href='/id/$authorId' class='node_login'>alfa</a>
                    <span class='node_header_created'>08.11.2026 - 13:00:00</span> $confLink
                    <div class='node_header_title'><a class='node_header_title_nodename' href='/id/$id'>Re: Vlaky a cestovanie</a></div>
                  </div>
                  <div class='node_body'>Nová odpoveď</div>
                </div>
              </li>"""
            },
        ),
        url,
    )

    @Test
    fun newChildFromMeIsAdded() {
        val result = Replies.outcome(before, withNewChild(601), me = 601)
        assertEquals(5999L, (result as ReplyResult.Added).childId)
        assertTrue(result.page.children.any { it.id == 5999L })
    }

    @Test
    fun someoneElsesChildDoesNotConfirm() {
        assertTrue(Replies.outcome(before, withNewChild(602), me = 601) is ReplyResult.Unconfirmed)
        // Vlastné ID nepoznáme – stačí nová odpoveď.
        assertTrue(Replies.outcome(before, withNewChild(602), me = null) is ReplyResult.Added)
    }

    @Test
    fun confMarksOwnReplyWithoutOwnId() {
        // Šablóna bez SELF (me = null): súbežná cudzia odpoveď navrchu, tvoja spoznaná podľa „conf“.
        val page = withNewChildren(5999L to 602L, 5998L to 601L, conf = setOf(5998L))
        assertEquals(5998L, (Replies.outcome(before, page, me = null) as ReplyResult.Added).childId)
        assertEquals(5998L, (Replies.outcome(before, page, me = 601) as ReplyResult.Added).childId)
    }

    @Test
    fun samePageIsUnconfirmed() {
        assertTrue(Replies.outcome(before, Jsoup.parse(html, url), me = 601) is ReplyResult.Unconfirmed)
    }

    @Test
    fun kyberiaErrorIsRejected() {
        val page = Jsoup.parse(html.replace("<body>", "<body><center><span class='most_important'>Chyba HTML syntaxe!</span></center>"), url)
        assertEquals("Chyba HTML syntaxe!", (Replies.outcome(before, page, me = 601) as ReplyResult.Rejected).message)
    }
}
