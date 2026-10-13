package sk.ntnlabs.kyberia.parse

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Test

class UserInfoParserTest {

    @Test
    fun readsSelfIdAndKWallet() {
        val info = UserInfoParser.parse(Fixtures.load("bookmarks.html", "https://kyberia.sk/id/19/"))
        assertEquals(UserInfo(userId = 4242, kWallet = 1234), info)
    }

    @Test
    fun missingPiecesAreNull() {
        val info = UserInfoParser.parse(Jsoup.parse("<p>nič</p>", "https://kyberia.sk/"))
        assertEquals(UserInfo(userId = null, kWallet = null), info)
    }
}
