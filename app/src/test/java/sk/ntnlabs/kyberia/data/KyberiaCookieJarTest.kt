package sk.ntnlabs.kyberia.data

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KyberiaCookieJarTest {

    private class MemoryStorage(var value: String? = null) : CookieStorage {
        override fun read() = value
        override fun write(value: String?) {
            this.value = value
        }
    }

    private val kyberia = "https://kyberia.sk/id/19/".toHttpUrl()
    private val foreign = "https://example.com/".toHttpUrl()

    private fun cookie(url: okhttp3.HttpUrl, header: String) = requireNotNull(Cookie.parse(url, header))

    @Test
    fun persistsAndRestoresKyberiaCookies() {
        val storage = MemoryStorage()
        KyberiaCookieJar(storage).saveFromResponse(kyberia, listOf(cookie(kyberia, "sid=abc; Path=/; Secure; HttpOnly")))

        val restored = KyberiaCookieJar(storage)
        assertTrue(restored.hasCookies())
        assertEquals(listOf("sid" to "abc"), restored.loadForRequest(kyberia).map { it.name to it.value })
    }

    @Test
    fun ignoresCookiesFromOtherSites() {
        val jar = KyberiaCookieJar(MemoryStorage())
        jar.saveFromResponse(foreign, listOf(cookie(foreign, "track=1")))
        assertFalse(jar.hasCookies())
    }

    @Test
    fun neverSendsKyberiaCookiesElsewhere() {
        val jar = KyberiaCookieJar(MemoryStorage())
        jar.saveFromResponse(kyberia, listOf(cookie(kyberia, "sid=abc; Path=/")))
        assertTrue(jar.loadForRequest(foreign).isEmpty())
    }

    @Test
    fun expiryReportsEarliestDatedCookieOnly() {
        val now = System.currentTimeMillis()
        val jar = KyberiaCookieJar(MemoryStorage()) { now }
        assertEquals(CookieExpiry(0, null), jar.expiry())
        jar.saveFromResponse(kyberia, listOf(cookie(kyberia, "sid=abc; Path=/")))
        assertEquals(CookieExpiry(1, null), jar.expiry())
        jar.saveFromResponse(kyberia, listOf(cookie(kyberia, "pref=1; Max-Age=3600")))
        val expiry = jar.expiry()
        assertEquals(2, expiry.count)
        assertTrue(requireNotNull(expiry.earliestMillis) > now)
    }

    @Test
    fun dropsExpiredCookies() {
        var now = System.currentTimeMillis()
        val jar = KyberiaCookieJar(MemoryStorage()) { now }
        jar.saveFromResponse(kyberia, listOf(cookie(kyberia, "sid=abc; Max-Age=60")))
        assertTrue(jar.hasCookies())
        now += 120_000
        assertFalse(jar.hasCookies())
    }

    @Test
    fun clearWipesStorage() {
        val storage = MemoryStorage()
        val jar = KyberiaCookieJar(storage)
        jar.saveFromResponse(kyberia, listOf(cookie(kyberia, "sid=abc")))
        jar.clear()
        assertFalse(jar.hasCookies())
        assertNull(storage.value)
    }
}
