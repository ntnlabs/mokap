package sk.ntnlabs.kyberia.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshTimerTest {

    private val minute = 60_000L
    private val interval = 15 * minute

    @Test
    fun progressGrowsToOne() {
        assertEquals(0f, RefreshTimer.progress(null, 0, interval))
        assertEquals(0f, RefreshTimer.progress(1_000, 1_000, interval))
        assertEquals(0.5f, RefreshTimer.progress(0, interval / 2, interval))
        assertEquals(1f, RefreshTimer.progress(0, interval * 3, interval))
    }

    @Test
    fun dueAfterInterval() {
        assertTrue(RefreshTimer.isDue(null, 0, interval))
        assertFalse(RefreshTimer.isDue(0, interval - 1, interval))
        assertTrue(RefreshTimer.isDue(0, interval, interval))
    }

    @Test
    fun backgroundCheckSkipsRecentLoadWithSlack() {
        assertFalse(RefreshTimer.recentlyLoaded(null, 0, interval))
        assertTrue(RefreshTimer.recentlyLoaded(0, 5 * minute, interval))
        // WorkManager môže prísť o chvíľu skôr – tesne pred koncom intervalu už nepreskakuje.
        assertFalse(RefreshTimer.recentlyLoaded(0, 14 * minute, interval))
        assertFalse(RefreshTimer.recentlyLoaded(10 * minute, 0, interval))
    }
}
