package sk.ntnlabs.kyberia.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmbedsTest {

    @Test
    fun youtubeEmbedOpensAsVideoPage() {
        val embed = "https://www.youtube-nocookie.com/embed/abc123XYZ_-?si=x"
        assertEquals("abc123XYZ_-", Embeds.youtubeId(embed))
        assertEquals("https://www.youtube.com/watch?v=abc123XYZ_-", Embeds.openUrl(embed))
        assertEquals("https://img.youtube.com/vi/abc123XYZ_-/hqdefault.jpg", Embeds.thumbnail(embed))
        assertEquals("abc123XYZ", Embeds.youtubeId("https://youtu.be/abc123XYZ"))
    }

    @Test
    fun otherEmbedsUnchanged() {
        val vimeo = "https://player.vimeo.com/video/12345"
        assertNull(Embeds.youtubeId(vimeo))
        assertEquals(vimeo, Embeds.openUrl(vimeo))
        assertNull(Embeds.thumbnail(vimeo))
    }
}
