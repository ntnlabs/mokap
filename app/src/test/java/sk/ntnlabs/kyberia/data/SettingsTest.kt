package sk.ntnlabs.kyberia.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sk.ntnlabs.kyberia.parse.Friend

class SettingsTest {

    @Test
    fun quietHoursAcrossMidnight() {
        assertTrue(QuietHours.contains(22, 6, 22))
        assertTrue(QuietHours.contains(22, 6, 23))
        assertTrue(QuietHours.contains(22, 6, 0))
        assertTrue(QuietHours.contains(22, 6, 5))
        assertFalse(QuietHours.contains(22, 6, 6))
        assertFalse(QuietHours.contains(22, 6, 21))
        assertFalse(QuietHours.contains(22, 6, 12))
    }

    @Test
    fun quietHoursSameDay() {
        assertTrue(QuietHours.contains(1, 9, 1))
        assertTrue(QuietHours.contains(1, 9, 8))
        assertFalse(QuietHours.contains(1, 9, 9))
        assertFalse(QuietHours.contains(1, 9, 0))
        assertFalse(QuietHours.contains(5, 5, 5))
    }

    @Test
    fun quietOnlyWhenEnabled() {
        assertFalse(Settings(quietEnabled = false).isQuiet(23))
        assertTrue(Settings(quietEnabled = true).isQuiet(23))
        assertFalse(Settings(quietEnabled = true).isQuiet(12))
    }

    @Test
    fun avatarsOnlyWifiBlocksMeteredNetwork() {
        assertTrue(Settings().avatarDownloadAllowed(metered = true))
        assertTrue(Settings(avatarsOnlyWifi = true).avatarDownloadAllowed(metered = false))
        assertFalse(Settings(avatarsOnlyWifi = true).avatarDownloadAllowed(metered = true))
        assertTrue(Settings().animateAvatars)
    }

    @Test
    fun intervalIsClampedToWhatAndroidAllows() {
        assertEquals(15, Settings.sanitizeInterval(10))
        assertEquals(15, Settings.sanitizeInterval(15))
        assertEquals(30, Settings.sanitizeInterval(25))
        assertEquals(60, Settings.sanitizeInterval(240))
    }

    @Test
    fun shortDurations() {
        assertEquals("< 1 min", Durations.short(39))
        assertEquals("84 min", Durations.short(84 * 60 + 39))
        assertEquals("2 h", Durations.short(2 * 3600))
        assertEquals("2 h 5 min", Durations.short(2 * 3600 + 5 * 60 + 10))
    }

    @Test
    fun friendsCameOnline() {
        fun friend(id: Long) = Friend(id, "f$id", null, null, null, "1m", 60)
        val fresh = FriendNews.cameOnline(setOf(1L, 2L), listOf(friend(2), friend(3)))
        assertEquals(listOf(3L), fresh.map { it.id })
        assertEquals(setOf(1L, 2L, 3L), FriendNews.decode(FriendNews.encode(listOf(1L, 2L, 3L))))
        assertEquals(emptySet<Long>(), FriendNews.decode(""))
    }
}
