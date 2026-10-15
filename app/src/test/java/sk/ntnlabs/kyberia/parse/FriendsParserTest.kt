package sk.ntnlabs.kyberia.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendsParserTest {

    private val friends = FriendsParser.parse(Fixtures.load("bookmarks.html", "https://kyberia.sk/id/19/"))

    @Test
    fun parsesOnlineFriends() {
        assertEquals(
            listOf(
                Friend(
                    id = 601,
                    nick = "gama delta",
                    avatarUrl = "https://kyberia.sk/images/nodes/6/0/601.gif",
                    locationId = 24,
                    locationTitle = "mail",
                    time = "84m39s",
                    timeSeconds = 84 * 60 + 39,
                ),
                Friend(
                    id = 602,
                    nick = "epsilon",
                    avatarUrl = "https://kyberia.sk/images/nodes/6/0/602.gif",
                    locationId = 7000,
                    locationTitle = "nejaké fórum",
                    time = "1h2m3s",
                    timeSeconds = 3723,
                ),
            ),
            friends,
        )
    }

    @Test
    fun parsesTimeUnits() {
        assertEquals(39L, FriendsParser.seconds("39s"))
        assertEquals(90_061L, FriendsParser.seconds("1d1h1m1s"))
        assertNull(FriendsParser.seconds(""))
    }

    @Test
    fun noFriendsOnlineGivesEmptyList() {
        assertTrue(FriendsParser.parse(Fixtures.load("login.html", "https://kyberia.sk/id/19/")).isEmpty())
    }
}
