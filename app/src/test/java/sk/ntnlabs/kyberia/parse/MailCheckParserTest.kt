package sk.ntnlabs.kyberia.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MailCheckParserTest {

    @Test
    fun unreadCountAndLastSender() {
        assertEquals(MailCheck.Unread(0, "alfa"), MailCheckParser.parse("0;alfa;5"))
        assertEquals(MailCheck.Unread(3, "beta 1/2"), MailCheckParser.parse("3;beta 1/2;1\n"))
        assertEquals(MailCheck.Unread(1, "Ďatelina"), MailCheckParser.parse("1;Ďatelina"))
        assertEquals(MailCheck.Unread(2), MailCheckParser.parse("\uFEFF 2 "))
    }

    @Test
    fun senderIsSanitized() {
        assertEquals(MailCheck.Unread(1, "ga;ma"), MailCheckParser.parse("1;ga;ma;7"))
        assertEquals(MailCheck.Unread(1, "delta"), MailCheckParser.parse("1; del\u0007ta ;7"))
        assertEquals(MailCheck.Unread(1), MailCheckParser.parse("1;  ;7"))
        assertEquals(64, (MailCheckParser.parse("1;" + "x".repeat(200) + ";7") as MailCheck.Unread).lastFrom?.length)
    }

    @Test
    fun offline() {
        assertEquals(MailCheck.Offline, MailCheckParser.parse("offline"))
        assertEquals(MailCheck.Offline, MailCheckParser.parse("offline;"))
    }

    @Test
    fun anythingElseIsUnexpected() {
        assertNull(MailCheckParser.parse(""))
        assertNull(MailCheckParser.parse("<html><body>chyba</body></html>"))
        assertNull(MailCheckParser.parse("-1;x"))
    }
}
