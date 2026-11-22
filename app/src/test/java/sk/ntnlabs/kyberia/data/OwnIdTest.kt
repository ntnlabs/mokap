package sk.ntnlabs.kyberia.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import sk.ntnlabs.kyberia.parse.Fixtures
import sk.ntnlabs.kyberia.parse.LoginType
import sk.ntnlabs.kyberia.parse.PeopleParser

class OwnIdTest {

    private val people = PeopleParser.parse(Fixtures.load("people.html", "https://kyberia.sk/id/27/"))

    @Test
    fun typedIdIsOwnId() {
        assertEquals(777L, OwnId.typed(" 777 ", LoginType.ID))
        assertNull(OwnId.typed("777", LoginType.NAME))
        assertNull(OwnId.typed("omega", LoginType.ID))
    }

    @Test
    fun nickFoundInPeople() {
        assertEquals(603L, OwnId.fromPeople(people, "zeta"))
        // Nick s medzerou a iná veľkosť písmen ako pri prihlásení.
        assertEquals(601L, OwnId.fromPeople(people, " Gama Delta "))
        assertNull(OwnId.fromPeople(people, "gama"))
        assertNull(OwnId.fromPeople(people, ""))
        assertNull(OwnId.fromPeople(emptyList(), "zeta"))
    }

    @Test
    fun ambiguousNickGivesNothing() {
        val twice = people + people.first { it.nick == "zeta" }.copy(id = 999)
        assertNull(OwnId.fromPeople(twice, "zeta"))
        // Ten istý človek dvakrát (priatelia aj users) nie je nejednoznačný.
        assertEquals(603L, OwnId.fromPeople(people + people.first { it.nick == "zeta" }, "zeta"))
    }
}
