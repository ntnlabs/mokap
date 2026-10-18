package sk.ntnlabs.kyberia.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PopulatedParserTest {

    private val populated = PopulatedParser.parse(Fixtures.load("populated.html", "https://kyberia.sk/id/4830026/"))

    @Test
    fun countsPeoplePerNodeMostFirst() {
        assertEquals(
            listOf(Hotspot(7000, "nejaké fórum", 3), Hotspot(35, "live:last", 2)),
            populated.hotspots.take(2),
        )
        assertEquals(4, populated.hotspots.size)
    }

    @Test
    fun titlesAreUnescaped() {
        assertTrue(populated.hotspots.any { it.nodeId == 7001L && it.title == "it's & ok" })
    }

    @Test
    fun readsPlacedAndOnlineCounts() {
        assertEquals(7, populated.placed)
        assertEquals(12, populated.online)
    }

    @Test
    fun pageWithoutScriptIsEmpty() {
        val empty = PopulatedParser.parse(Fixtures.load("login.html", "https://kyberia.sk/"))
        assertTrue(empty.hotspots.isEmpty())
        assertNull(empty.online)
    }
}
