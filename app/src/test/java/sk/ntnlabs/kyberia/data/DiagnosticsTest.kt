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
    fun historyIsCappedAndRemembersOkBeforeLogout() {
        var diagnostics = Diagnostics()
        repeat(Diagnostics.HISTORY_SIZE + 5) { i ->
            diagnostics = diagnostics.record(CheckEntry(i * 1_000L, CheckResult.OK, CheckTrigger.SCHEDULED))
        }
        assertEquals(Diagnostics.HISTORY_SIZE, diagnostics.history.size)
        assertEquals(5_000L, diagnostics.history.first().atMillis)

        val expired = diagnostics.record(CheckEntry(99_000, CheckResult.EXPIRED, CheckTrigger.APP, "login"))
        assertEquals(diagnostics.lastOkMillis, expired.okBeforeExpiredMillis)
        // Ďalšie OK po prihlásení údaj pred odhlásením neprepíše.
        assertEquals(expired.okBeforeExpiredMillis, expired.record(CheckEntry(100_000, CheckResult.OK, CheckTrigger.APP)).okBeforeExpiredMillis)
    }

    @Test
    fun historyCodecRoundTripsAndCleansDetail() {
        val history = listOf(
            CheckEntry(1_000, CheckResult.OK, CheckTrigger.SCHEDULED),
            CheckEntry(2_000, CheckResult.ERROR, CheckTrigger.APP, "timeout\nna\tsieti"),
        )
        val decoded = CheckHistoryCodec.decode(CheckHistoryCodec.encode(history))
        assertEquals(history[0], decoded[0])
        assertEquals("timeout na sieti", decoded[1].detail)
        assertEquals(emptyList<CheckEntry>(), CheckHistoryCodec.decode(""))
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
