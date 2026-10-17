package sk.ntnlabs.kyberia.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MailCheckParserTest {

    @Test
    fun unreadCountIsFirstField() {
        assertEquals(MailCheck.Unread(0), MailCheckParser.parse("0;alfa;5"))
        assertEquals(MailCheck.Unread(3), MailCheckParser.parse("3;beta 1/2;1\n"))
        assertEquals(MailCheck.Unread(2), MailCheckParser.parse("\uFEFF 2 "))
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
