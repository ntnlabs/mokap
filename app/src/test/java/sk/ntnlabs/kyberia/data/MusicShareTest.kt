package sk.ntnlabs.kyberia.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Vymyslené ID videí a parametre – žiadne skutočné zdieľania. */
class MusicShareTest {

    @Test
    fun youTubeLinksOfAllShapes() {
        assertEquals("abcDEF12345", MusicShare.youTubeId("https://youtu.be/abcDEF12345?is=xyzTRACK"))
        assertEquals("abcDEF12345", MusicShare.youTubeId("https://music.youtube.com/watch?v=abcDEF12345&si=xyzTRACK"))
        assertEquals("abcDEF12345", MusicShare.youTubeId("https://www.youtube.com/watch?feature=share&v=abcDEF12345"))
        assertEquals("abcDEF12345", MusicShare.youTubeId("https://m.youtube.com/watch?v=abcDEF12345"))
        assertEquals("Zy-_9876543", MusicShare.youTubeId("https://youtube.com/shorts/Zy-_9876543?si=q"))
        assertEquals("abcDEF12345", MusicShare.youTubeId("https://www.youtube.com/embed/abcDEF12345?si=q"))
        assertNull(MusicShare.youTubeId("https://www.youtube.com/@channel"))
        assertNull(MusicShare.youTubeId("https://youtu.be/kratke"))
        assertNull(MusicShare.youTubeId("https://example.com/watch?v=abcDEF12345"))
    }

    @Test
    fun youTubeShareBecomesEmbedWithoutTracking() {
        val share = MusicShare.parse("https://youtu.be/abcDEF12345?is=xyzTRACK", subject = "Interpret – Skladba")
        assertEquals("abcDEF12345", share.youTubeId)
        assertEquals("Interpret – Skladba", share.title)
        assertTrue(share.post.startsWith("<iframe "))
        assertTrue(share.post.contains("""src="https://www.youtube.com/embed/abcDEF12345""""))
        assertTrue("xyzTRACK" !in share.post && "si=" !in share.post)
        assertTrue(share.post.endsWith("allowfullscreen></iframe>"))
    }

    @Test
    fun otherServiceKeepsLinkAndOffersSearch() {
        val share = MusicShare.parse("Skladba od Interpreta https://open.example/track/123?si=t", subject = null)
        assertNull(share.youTubeId)
        assertEquals("https://open.example/track/123?si=t", share.otherLink)
        assertEquals("https://open.example/track/123?si=t", share.post)
        assertEquals("Skladba od Interpreta", share.query)
        assertEquals(
            "https://www.youtube.com/results?search_query=Skladba+od+Interpreta",
            MusicShare.searchUrl(share.query!!),
        )
    }

    @Test
    fun subjectThatIsJustLinkIsNoTitle() {
        val share = MusicShare.parse("https://youtu.be/abcDEF12345", subject = "https://youtu.be/abcDEF12345")
        assertNull(share.title)
        assertNull(share.query)
    }

    @Test
    fun targetFromNumberOrKyberiaLink() {
        assertEquals(1234567L, KyberiaTarget.parse(" 1234567 "))
        assertEquals(1234567L, KyberiaTarget.parse("https://kyberia.sk/id/1234567/"))
        assertEquals(1234567L, KyberiaTarget.parse("kyberia.sk/id/1234567"))
        assertNull(KyberiaTarget.parse(""))
        assertNull(KyberiaTarget.parse("0"))
        assertNull(KyberiaTarget.parse("https://example.com/id/1234567/"))
        assertNull(KyberiaTarget.parse("fórum"))
    }
}
