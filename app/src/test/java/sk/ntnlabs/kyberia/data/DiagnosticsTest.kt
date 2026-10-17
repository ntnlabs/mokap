package sk.ntnlabs.kyberia.data

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import sk.ntnlabs.kyberia.parse.Fixtures
import sk.ntnlabs.kyberia.parse.PageKind
import sk.ntnlabs.kyberia.parse.Session

class DiagnosticsTest {

    @Test
    fun onlyLoginFormMeansLoggedOut() {
        assertEquals(PageKind.LOGGED_IN, Session.classify(Fixtures.load("bookmarks.html", "https://kyberia.sk/id/19/")))
        assertEquals(PageKind.LOGGED_OUT, Session.classify(Fixtures.load("login.html", "https://kyberia.sk/id/19/")))
        // Údržba, ochrana proti botom, chyba servera – session sa kvôli tomu nezahadzuje.
        val maintenance = Jsoup.parse("<html><body><h1>503 Service Unavailable</h1></body></html>", "https://kyberia.sk/")
        assertEquals(PageKind.UNKNOWN, Session.classify(maintenance))
    }

    @Test
    fun recordKeepsLastOkAndLastExpired() {
        val ok = CheckEntry(1_000, CheckResult.OK, CheckTrigger.SCHEDULED)
        val error = CheckEntry(2_000, CheckResult.ERROR, CheckTrigger.APP, "timeout")
        val expired = CheckEntry(3_000, CheckResult.EXPIRED, CheckTrigger.SCHEDULED, "login")

        val afterOk = Diagnostics().record(ok)
        assertEquals(1_000L, afterOk.lastOkMillis)
        assertNull(afterOk.lastExpired)

        val afterError = afterOk.record(error)
        assertEquals(error, afterError.last)
        assertEquals(1_000L, afterError.lastOkMillis)

        val afterExpired = afterError.record(expired)
        assertEquals(expired, afterExpired.lastExpired)
        assertEquals(1_000L, afterExpired.lastOkMillis)
    }
}
