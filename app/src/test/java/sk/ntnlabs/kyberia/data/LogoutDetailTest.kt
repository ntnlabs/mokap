package sk.ntnlabs.kyberia.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LogoutDetailTest {

    @Test
    fun idleLogoutRedirectsToFrontPage() {
        assertEquals(
            "na stránke bol login, presmerovanie na /",
            LogoutDetail.of("https://kyberia.sk/id/19/", "https://kyberia.sk/"),
        )
    }

    @Test
    fun sessionGoneWithoutRedirect() {
        val expected = "na stránke bol login, bez presmerovania"
        assertEquals(expected, LogoutDetail.of("https://kyberia.sk/id/19/", "https://kyberia.sk/id/19/"))
        assertEquals(expected, LogoutDetail.of("https://kyberia.sk/id/19/", "https://kyberia.sk/id/19"))
    }

    @Test
    fun otherRedirectNamesThePath() {
        assertEquals(
            "na stránke bol login, presmerovanie na /id/1",
            LogoutDetail.of("https://kyberia.sk/id/27/", "https://kyberia.sk/id/1/?x=1"),
        )
    }
}
