package sk.ntnlabs.kyberia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlPolicyTest {

    @Test
    fun kyberiaHttpsIsInternal() {
        assertTrue(UrlPolicy.isInternal("https://kyberia.sk/"))
        assertTrue(UrlPolicy.isInternal("https://kyberia.sk/id/123"))
        assertTrue(UrlPolicy.isInternal("https://www.kyberia.sk/id/123?x=1#a"))
        assertTrue(UrlPolicy.isInternal("HTTPS://KYBERIA.SK/"))
    }

    @Test
    fun foreignHostsAreExternal() {
        assertFalse(UrlPolicy.isInternal("https://example.com/"))
        assertFalse(UrlPolicy.isInternal("https://kyberia.sk.evil.com/"))
        assertFalse(UrlPolicy.isInternal("https://evilkyberia.sk/"))
        assertFalse(UrlPolicy.isInternal("https://kyberia.sk@evil.com/"))
    }

    @Test
    fun nonHttpsIsExternal() {
        assertFalse(UrlPolicy.isInternal("http://kyberia.sk/"))
        assertFalse(UrlPolicy.isInternal("mailto:someone@kyberia.sk"))
        assertFalse(UrlPolicy.isInternal("javascript:alert(1)"))
        assertFalse(UrlPolicy.isInternal("not a url"))
    }

    @Test
    fun normalizeUpgradesOnlyKyberiaHttp() {
        assertEquals("https://kyberia.sk/id/1", UrlPolicy.normalize("http://kyberia.sk/id/1"))
        assertEquals("https://www.kyberia.sk/", UrlPolicy.normalize("http://www.kyberia.sk/"))
        assertEquals("http://example.com/", UrlPolicy.normalize("http://example.com/"))
        assertEquals("https://kyberia.sk/", UrlPolicy.normalize("https://kyberia.sk/"))
    }

    @Test
    fun startUrlFallsBackForForeignLinks() {
        val home = "https://kyberia.sk/"
        assertEquals(home, UrlPolicy.startUrl(null, home))
        assertEquals(home, UrlPolicy.startUrl("https://example.com/", home))
        assertEquals("https://kyberia.sk/id/42", UrlPolicy.startUrl("http://kyberia.sk/id/42", home))
    }
}
