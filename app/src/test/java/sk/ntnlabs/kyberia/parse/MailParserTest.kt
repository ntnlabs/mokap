package sk.ntnlabs.kyberia.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MailParserTest {

    private val page = MailParser.parse(Fixtures.load("mail.html", "https://kyberia.sk/id/24/"))

    @Test
    fun messages() {
        assertEquals(listOf(9001L, 9000L, 8999L), page.mails.map { it.id })
        assertEquals(777L, page.selfId)
        val first = page.mails[0]
        assertEquals(601L, first.fromId)
        assertEquals("alfa", first.from)
        assertEquals(777L, first.toId)
        assertEquals("20:04:25 - 11.11.2026", first.time)
        assertTrue(first.isNew)
        assertFalse(first.unreadByRecipient)
        assertEquals("https://kyberia.sk/images/nodes/6/0/601.gif", first.avatarUrl)
        // Odkaz na node v texte správy – v appke sa otvorí node.
        val spans = (first.content.single() as ContentBlock.Text).spans
        assertEquals("https://kyberia.sk/id/5001/", spans.last().link)
    }

    @Test
    fun directionAndCounterpart() {
        val (incoming, outgoing, older) = page.mails
        assertFalse(page.outgoing(incoming))
        assertTrue(page.outgoing(outgoing))
        assertTrue(outgoing.unreadByRecipient)
        assertFalse(outgoing.isNew)
        // Nick s medzerou, druhá strana konverzácie podľa smeru.
        assertEquals(602L to "beta gama", page.counterpart(outgoing))
        assertEquals(602L to "beta gama", page.counterpart(older))
        assertEquals(601L to "alfa", page.counterpart(incoming))
        assertFalse(older.isNew || older.unreadByRecipient)
    }

    @Test
    fun selfWithoutSelfLink() {
        // Šablóna hlavičky bez odkazu SELF: vlastné ID zo správ, smer a odpoveď ostanú správne.
        val raw = requireNotNull(javaClass.getResourceAsStream("/fixtures/mail.html")).use { it.readBytes().toString(Charsets.UTF_8) }
        val html = raw.replace(Regex("""<a name="SELF[^>]*>self</a>"""), "")
        assertTrue(html != raw)
        val noSelf = MailParser.parse(org.jsoup.Jsoup.parse(html, "https://kyberia.sk/id/24/"))
        assertNull(UserInfoParser.parse(org.jsoup.Jsoup.parse(html)).userId)
        assertEquals(777L, noSelf.selfId)
        assertEquals(602L to "beta gama", noSelf.counterpart(noSelf.mails[1]))
        // ID zistené pri prihlásení má prednosť pred hádaním zo správ, SELF na stránke pred ním.
        assertEquals(778L, MailParser.parse(org.jsoup.Jsoup.parse(html, "https://kyberia.sk/id/24/"), knownSelf = 778).selfId)
        assertEquals(777L, MailParser.parse(Fixtures.load("mail.html", "https://kyberia.sk/id/24/"), knownSelf = 778).selfId)
    }

    @Test
    fun selfFromMails() {
        fun mail(fromId: Long, toId: Long, isNew: Boolean = false, unread: Boolean = false) =
            Mail(fromId * 10 + toId, fromId, "", toId, "", null, isNew, unread, null, emptyList())
        assertEquals(777L, MailParser.selfFromMails(listOf(mail(777, 601, unread = true))))
        assertEquals(777L, MailParser.selfFromMails(listOf(mail(601, 777, isNew = true))))
        // Bez značiek: ten, kto je pri každej správe.
        assertEquals(777L, MailParser.selfFromMails(listOf(mail(601, 777), mail(777, 602))))
        // Jedna konverzácia bez značiek – nedá sa povedať, kto je kto.
        assertNull(MailParser.selfFromMails(listOf(mail(601, 777), mail(777, 601))))
        assertNull(MailParser.selfFromMails(emptyList()))
    }

    @Test
    fun sendOnlyNeededFields() {
        val form = requireNotNull(page.form)
        assertEquals("https://kyberia.sk/id/24/", form.action)
        assertEquals("mail-token", form.anticsrf)
        assertTrue(form.multipart)
        assertFalse("token nesmie byť vo výpise", form.toString().contains("mail-token"))
        // Odpoveď na ID, bez delete_mail, stránkovania a hľadania z formulára.
        assertEquals(
            listOf(
                "anticsrf" to "mail-token",
                "mail_to" to "601",
                "mail_to_type" to "id",
                "mail_text" to "Ahoj\n<b>ty</b>",
                "event" to "send",
            ),
            MailParser.sendFields(form, "601", byId = true, text = "Ahoj\n<b>ty</b>", noHtml = false),
        )
        val byNick = MailParser.sendFields(form, " beta gama ", byId = false, text = "x", noHtml = true)
        assertEquals("beta gama", byNick.first { it.first == "mail_to" }.second)
        assertEquals("name", byNick.first { it.first == "mail_to_type" }.second)
        assertEquals("yes", byNick.first { it.first == "no_html" }.second)
    }

    @Test
    fun noMailOnOtherPage() {
        val other = MailParser.parse(Fixtures.load("node.html", "https://kyberia.sk/id/5001/"))
        assertTrue(other.mails.isEmpty())
        assertNull(other.form)
        // Hlavička pošty nie je chyba (NEW/UNREAD sú v správach, nie v center).
        assertNull(PageError.of(Fixtures.load("mail.html", "https://kyberia.sk/id/24/")))
    }
}
