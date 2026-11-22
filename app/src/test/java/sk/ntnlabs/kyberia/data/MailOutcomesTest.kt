package sk.ntnlabs.kyberia.data

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sk.ntnlabs.kyberia.parse.Mail
import sk.ntnlabs.kyberia.parse.MailPage
import sk.ntnlabs.kyberia.parse.MailParser

class MailOutcomesTest {

    private val url = "https://kyberia.sk/id/24/"
    private val html = requireNotNull(javaClass.getResourceAsStream("/fixtures/mail.html")).use { it.readBytes().toString(Charsets.UTF_8) }
    private val before = MailParser.parse(Jsoup.parse(html, url))

    /** Pošta bez odkazu SELF v hlavičke – niektoré šablóny ho nemajú. */
    private val noSelf = html.replace(Regex("""<a name="SELF[^>]*>self</a>"""), "")

    /** Pošta s novou správou navrchu (ako ju kyberia vráti po odoslaní). */
    private fun withNewMail(
        fromId: Long,
        from: String,
        toId: Long = 601,
        to: String = "alfa",
        mark: String = "UNREAD",
        source: String = html,
    ) = Jsoup.parse(
        source.replaceFirst(
            "<div class='message'",
            """<div class='message'><img class='mail_avatar' src='/images/nodes/7/7/777.gif' /><div class='header'>
               <a href="javascript:chngto('$from','$fromId')">$from</a> --> <a href="javascript:chngto('$to','$toId')">$to</a> --
               <input type='checkbox' name='message[9100]' class="mail_checkbox"><br /> 21:00:00 - 11.11.2026
               <span class='most_important'> $mark </span></div><div class='content'>Nová</div><hr /></div>
               <div class='message'""",
        ),
        url,
    )

    private fun mail(id: Long, fromId: Long, from: String, toId: Long, to: String) =
        Mail(id, fromId, from, toId, to, time = null, isNew = false, unreadByRecipient = false, avatarUrl = null, content = emptyList())

    @Test
    fun ownNewMailIsSent() {
        val result = MailOutcomes.send(before, withNewMail(777, "omega"), "601", byId = true)
        assertEquals(9100L, (result as SendResult.Sent).mailId)
    }

    @Test
    fun foreignNewMailDoesNotConfirm() {
        val incoming = withNewMail(602, "beta gama", toId = 777, to = "omega", mark = "NEW")
        assertTrue(MailOutcomes.send(before, incoming, "601", byId = true) is SendResult.Unconfirmed)
        assertTrue(MailOutcomes.send(before, Jsoup.parse(html, url), "601", byId = true) is SendResult.Unconfirmed)
    }

    @Test
    fun withoutSelfLinkStillConfirms() {
        val beforeNoSelf = MailParser.parse(Jsoup.parse(noSelf, url))
        val sent = MailOutcomes.send(beforeNoSelf, withNewMail(777, "omega", source = noSelf), "alfa", byId = false)
        assertEquals(9100L, (sent as SendResult.Sent).mailId)
        val incoming = withNewMail(602, "beta gama", toId = 777, to = "omega", mark = "NEW", source = noSelf)
        assertTrue(MailOutcomes.send(beforeNoSelf, incoming, "alfa", byId = false) is SendResult.Unconfirmed)
    }

    @Test
    fun recipientConfirmsWithoutOwnId() {
        // Vlastné ID sa nedá zistiť a adresát správu už stihol prečítať (bez UNREAD) – rozhodne adresát.
        val empty = MailPage(emptyList(), form = null, selfId = null)
        val page = MailPage(listOf(mail(9100, 777, "omega", 601, "alfa")), form = null, selfId = null)
        assertEquals(9100L, (MailOutcomes.recheck(empty, page, " 601 ", byId = true) as SendResult.Sent).mailId)
        assertEquals(9100L, (MailOutcomes.recheck(empty, page, "ALFA ", byId = false) as SendResult.Sent).mailId)
        assertTrue(MailOutcomes.recheck(empty, page, "602", byId = true) is SendResult.Unconfirmed)
        assertTrue(MailOutcomes.recheck(empty, page, "beta gama", byId = false) is SendResult.Unconfirmed)
        // Prichádzajúca od toho, komu píšeš, nie je tvoja odoslaná.
        val reply = MailPage(listOf(mail(9101, 601, "alfa", 777, "omega")), form = null, selfId = null)
        assertTrue(MailOutcomes.recheck(empty, reply, "601", byId = true) is SendResult.Unconfirmed)
    }

    @Test
    fun recheckFindsMailLater() {
        // Odpoveď na odoslanie novú správu ešte neukázala, čerstvo načítaná pošta už áno.
        assertTrue(MailOutcomes.send(before, Jsoup.parse(html, url), "601", byId = true) is SendResult.Unconfirmed)
        val later = MailParser.parse(withNewMail(777, "omega"))
        assertEquals(9100L, (MailOutcomes.recheck(before, later, "601", byId = true) as SendResult.Sent).mailId)
        assertTrue(MailOutcomes.recheck(before, before, "601", byId = true) is SendResult.Unconfirmed)
    }

    @Test
    fun kyberiaErrorIsRejected() {
        val page = Jsoup.parse(html.replace("<body>", "<body><center><span class='most_important'>Posta sa len tak nikomu neposiela.</span></center>"), url)
        assertEquals("Posta sa len tak nikomu neposiela.", (MailOutcomes.send(before, page, "601", byId = true) as SendResult.Rejected).message)
    }
}
