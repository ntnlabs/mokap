package sk.ntnlabs.kyberia.data

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sk.ntnlabs.kyberia.parse.MailParser

class MailOutcomesTest {

    private val url = "https://kyberia.sk/id/24/"
    private val html = requireNotNull(javaClass.getResourceAsStream("/fixtures/mail.html")).use { it.readBytes().toString(Charsets.UTF_8) }
    private val before = MailParser.parse(Jsoup.parse(html, url))

    /** Pošta s novou správou navrchu (ako ju kyberia vráti po odoslaní). */
    private fun withNewMail(fromId: Long, from: String) = Jsoup.parse(
        html.replaceFirst(
            "<div class='message'",
            """<div class='message'><img class='mail_avatar' src='/images/nodes/7/7/777.gif' /><div class='header'>
               <a href="javascript:chngto('$from','$fromId')">$from</a> --> <a href="javascript:chngto('alfa','601')">alfa</a> --
               <input type='checkbox' name='message[9100]' class="mail_checkbox"><br /> 21:00:00 - 11.11.2026
               <span class='most_important'> UNREAD </span></div><div class='content'>Nová</div><hr /></div>
               <div class='message'""",
        ),
        url,
    )

    @Test
    fun ownNewMailIsSent() {
        val result = MailOutcomes.send(before, withNewMail(777, "omega"))
        assertEquals(9100L, (result as SendResult.Sent).mailId)
    }

    @Test
    fun foreignNewMailDoesNotConfirm() {
        assertTrue(MailOutcomes.send(before, withNewMail(602, "beta gama")) is SendResult.Unconfirmed)
        assertTrue(MailOutcomes.send(before, Jsoup.parse(html, url)) is SendResult.Unconfirmed)
    }

    @Test
    fun recheckFindsMailLater() {
        // Odpoveď na odoslanie novú správu ešte neukázala, čerstvo načítaná pošta už áno.
        assertTrue(MailOutcomes.send(before, Jsoup.parse(html, url)) is SendResult.Unconfirmed)
        val later = MailParser.parse(withNewMail(777, "omega"))
        assertEquals(9100L, (MailOutcomes.recheck(before, later) as SendResult.Sent).mailId)
        assertTrue(MailOutcomes.recheck(before, before) is SendResult.Unconfirmed)
    }

    @Test
    fun kyberiaErrorIsRejected() {
        val page = Jsoup.parse(html.replace("<body>", "<body><center><span class='most_important'>Posta sa len tak nikomu neposiela.</span></center>"), url)
        assertEquals("Posta sa len tak nikomu neposiela.", (MailOutcomes.send(before, page) as SendResult.Rejected).message)
    }
}
