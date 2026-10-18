package sk.ntnlabs.kyberia.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sk.ntnlabs.kyberia.parse.LastPost

class NsfwTest {

    @Test
    fun recognizesStandaloneWordOnly() {
        assertTrue(Nsfw.matches("Archív NSFW"))
        assertTrue(Nsfw.matches("[nsfw] obrázky"))
        assertTrue(Nsfw.matches("NSFW: fotky"))
        assertTrue(Nsfw.matches("foto_NSFW"))
        assertFalse(Nsfw.matches("K SFW"))
        assertFalse(Nsfw.matches("nsfwácky"))
        assertFalse(Nsfw.matches(null, ""))
    }

    @Test
    fun labelReplacesWordWithEmoji() {
        assertEquals("Archív 🔞", Nsfw.label("Archív NSFW"))
        assertEquals("[🔞] obrázky", Nsfw.label("[nsfw] obrázky"))
        assertEquals("K SFW", Nsfw.label("K SFW"))
    }

    @Test
    fun lastHidesOnlyByOwnTitle() {
        fun post(id: Long, title: String, parent: String?, snippet: String = "s") =
            LastPost(id, title, null, "a", null, null, parent, null, snippet)
        val posts = listOf(
            post(1, "Re: ahoj", "Archív NSFW"),
            post(2, "NSFW vlákno", null),
            post(3, "vlaky", "doprava", snippet = "toto je nsfw text"),
        )
        // Rodič ani obsah sa nekontrolujú – len názov samotného nodu.
        assertEquals(listOf(1L, 3L), Nsfw.last(posts, hide = true).map { it.nodeId })
        assertEquals(3, Nsfw.last(posts, hide = false).size)
    }
}
