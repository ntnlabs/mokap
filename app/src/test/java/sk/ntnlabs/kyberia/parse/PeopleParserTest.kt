package sk.ntnlabs.kyberia.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeopleParserTest {

    private val people = PeopleParser.parse(Fixtures.load("people.html", "https://kyberia.sk/id/27/"))

    @Test
    fun takesUsersListWithoutDuplicates() {
        assertEquals(listOf(602L, 603L, 601L), people.map { it.id })
        assertEquals(listOf("epsilon", "zeta", "gama delta"), people.map { it.nick })
    }

    @Test
    fun idleComesFromDataAttribute() {
        assertEquals(listOf(120L, 5L, 3600L), people.map { it.timeSeconds })
        assertEquals("2min 0sec", people[0].time)
    }

    @Test
    fun locationUsesFullTitleAndAvatarIsAbsolute() {
        val first = people[0]
        assertEquals(7000L, first.locationId)
        assertEquals("nejaké veľmi dlhé fórum o vlakoch", first.locationTitle)
        assertEquals("https://kyberia.sk/images/nodes/6/0/602.gif", first.avatarUrl)
    }

    @Test
    fun otherPagesGiveEmptyList() {
        assertTrue(PeopleParser.parse(Fixtures.load("login.html", "https://kyberia.sk/")).isEmpty())
    }
}
