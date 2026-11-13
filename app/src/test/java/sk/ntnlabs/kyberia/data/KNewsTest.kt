package sk.ntnlabs.kyberia.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sk.ntnlabs.kyberia.parse.MyK

class KNewsTest {

    private fun k(id: Long, added: Int, total: Int?) = MyK(id, "post $id", added, total)

    @Test
    fun knownPostReportsGrowthOfTotal() {
        val gains = KNews.gains(mapOf(1L to 5), listOf(k(1, added = 3, total = 7)))
        assertEquals(listOf(KGain(1, "post 1", added = 2, total = 7)), gains)
    }

    @Test
    fun newPostReportsKInWindow() {
        val gains = KNews.gains(mapOf(1L to 5), listOf(k(2, added = 2, total = 9)))
        assertEquals(listOf(KGain(2, "post 2", added = 2, total = 9)), gains)
    }

    @Test
    fun unchangedOrLoweredTotalIsSilent() {
        assertTrue(KNews.gains(mapOf(1L to 5, 2L to 4), listOf(k(1, 1, 5), k(2, 1, 3))).isEmpty())
    }

    @Test
    fun knownPostWithoutTotalIsSilent() {
        assertTrue(KNews.gains(mapOf(1L to 5), listOf(k(1, 2, null))).isEmpty())
    }

    @Test
    fun snapshotKeepsTotalsAndRoundTrips() {
        val snapshot = KNews.snapshot(listOf(k(1, 1, 5), k(2, 1, null), k(3, 2, 12)))
        assertEquals(mapOf(1L to 5, 3L to 12), snapshot)
        assertEquals(snapshot, KNews.decode(KNews.encode(snapshot)))
    }

    @Test
    fun catchUpOnlyAfterLongGap() {
        val now = 100L * 3_600_000
        assertNull(KCatchUp.hours(null, now))
        assertNull(KCatchUp.hours(now - 30 * 60_000, now))
        assertNull(KCatchUp.hours(now - KCatchUp.TOLERANCE_MILLIS, now))
        assertNull(KCatchUp.hours(now + 60_000, now)) // posunuté hodiny
        assertEquals(2, KCatchUp.hours(now - 71 * 60_000, now))
        // Noc v tichom režime: 22:00 → 6:10 = 8 h 10 min → 9 h, nech blok siaha až po poslednú kontrolu.
        assertEquals(9, KCatchUp.hours(now - (8 * 60 + 10) * 60_000L, now))
        assertEquals(KCatchUp.MAX_HOURS, KCatchUp.hours(now - 72 * 3_600_000L, now))
    }
}
