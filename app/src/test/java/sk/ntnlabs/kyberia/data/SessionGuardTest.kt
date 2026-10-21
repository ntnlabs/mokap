package sk.ntnlabs.kyberia.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionGuardTest {

    @Test
    fun guardLeavesRoomForDelayedAlarmAndOneRetry() {
        // Android môže nepresný budík odložiť; aj s jedným opakovaním musíme stihnúť pred odhlásením.
        val androidSlack = 5 * 60_000L
        assertTrue(SessionGuard.DELAY_MILLIS + SessionGuard.RETRY_MILLIS + androidSlack < SessionGuard.SERVER_IDLE_LOGOUT_MILLIS)
    }

    @Test
    fun delayCountsFromLastRequestNotFromNow() {
        val minute = 60_000L
        assertEquals(SessionGuard.DELAY_MILLIS, SessionGuard.delayAfter(null, 0))
        assertEquals(SessionGuard.DELAY_MILLIS - 30 * minute, SessionGuard.delayAfter(0, 30 * minute))
        // Termín už prešiel – zasiahni hneď (o minútu), nie o ďalších 100 minút.
        assertEquals(minute, SessionGuard.delayAfter(0, 5 * 60 * minute))
    }

    @Test
    fun retriesOnlyWhileTheSessionCanStillBeSaved() {
        val minute = 60_000L
        val lastOk = 0L
        // Strážca o 100 min: do odhlásenia zostáva 20 min, skúsi o 10.
        assertEquals(SessionGuard.RETRY_MILLIS, SessionGuard.retryAfter(lastOk, 100 * minute))
        // Tesne pred termínom ešte jeden pokus pred koncom.
        assertEquals(4 * minute, SessionGuard.retryAfter(lastOk, 115 * minute))
        // Po 2 h už kyberia odhlásila – žiadne ďalšie budenie telefónu.
        assertNull(SessionGuard.retryAfter(lastOk, 119 * minute + 30_000))
        assertNull(SessionGuard.retryAfter(lastOk, 5 * 60 * minute))
        assertEquals(SessionGuard.RETRY_MILLIS, SessionGuard.retryAfter(null, 0))
    }
}
