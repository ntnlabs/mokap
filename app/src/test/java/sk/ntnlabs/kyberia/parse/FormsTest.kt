package sk.ntnlabs.kyberia.parse

import java.nio.charset.StandardCharsets
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormsTest {

    private val bookmarksPage = Fixtures.load("bookmarks.html", "https://kyberia.sk/id/19/")
    private val loginPage = Fixtures.load("login.html", "https://kyberia.sk/")

    @Test
    fun loggedInPageHasLogoutButton() {
        assertTrue(Session.isLoggedIn(bookmarksPage))
        assertFalse(Session.isLoggedIn(loginPage))
    }

    @Test
    fun logoutFormCarriesAnticsrf() {
        val form = requireNotNull(Forms.find(bookmarksPage) { Forms.submitButton(it, "logout") != null })
        assertEquals("https://kyberia.sk/id/19/", Forms.action(form, bookmarksPage))
        assertEquals(listOf("anticsrf" to "test-token"), Forms.fields(form))
        assertEquals("event" to "logout", Forms.submitButton(form, "logout"))
    }

    @Test
    fun findsLoginFormWithoutKnowingFieldNames() {
        val form = LoginFormParser.find(loginPage)!!
        assertEquals("https://kyberia.sk/id/1/", form.action)
        assertEquals("login", form.usernameField)
        assertEquals("password", form.passwordField)
        assertEquals(StandardCharsets.UTF_8, form.charset)
        assertEquals(
            setOf(
                "anticsrf" to "test-token",
                "remember" to "1",
                "event" to "login",
                "login" to "nick",
                "password" to "secret",
            ),
            form.fill("nick", "secret").toSet(),
        )
    }

    @Test
    fun refusesPasswordFormsSentByGet() {
        val doc = Jsoup.parse(
            """<form method="get"><input name="u"><input type="password" name="p"></form>""",
            "https://kyberia.sk/",
        )
        assertNull(LoginFormParser.find(doc))
    }

    @Test
    fun loggedInPageHasNoLoginForm() {
        assertNull(LoginFormParser.find(bookmarksPage))
    }
}
