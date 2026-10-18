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
        assertEquals("https://kyberia.sk/id/5000/", form.action)
        assertEquals("login", form.usernameField)
        assertEquals("password", form.passwordField)
        assertEquals(StandardCharsets.UTF_8, form.charset)
        assertTrue(form.multipart)
        assertEquals(
            setOf(
                "login_type" to "name",
                "event" to "login",
                "login" to "nick",
                "password" to "secret",
            ),
            form.fill("nick", "secret").toSet(),
        )
    }

    @Test
    fun loginByIdSwitchesTheRadio() {
        val form = LoginFormParser.find(loginPage)!!
        assertEquals("login_type", form.loginTypeField)
        assertEquals(setOf(LoginType.NAME, LoginType.ID), form.loginTypes)
        assertTrue(form.supports(LoginType.ID))
        val fields = form.fill("4242", "secret", LoginType.ID)
        assertEquals(listOf("id"), fields.filter { it.first == "login_type" }.map { it.second })
        assertTrue("login" to "4242" in fields)
    }

    @Test
    fun formWithoutTypeRadiosSupportsOnlyNick() {
        val doc = Jsoup.parse(
            """<form method="post" action="/x"><input name="user"><input type="password" name="pw"></form>""",
            "https://kyberia.sk/",
        )
        val form = LoginFormParser.find(doc)!!
        assertNull(form.loginTypeField)
        assertFalse(form.supports(LoginType.ID))
        assertEquals(setOf("user" to "u", "pw" to "p"), form.fill("u", "p", LoginType.ID).toSet())
    }

    @Test
    fun genericLoginFormKeepsHiddenAndCheckedFields() {
        val doc = Jsoup.parse(
            """
            <form method="post" action="/x">
              <input type="hidden" name="anticsrf" value="t">
              <input name="user"><input type="password" name="pw">
              <input type="checkbox" name="remember" value="1" checked>
              <input type="checkbox" name="ignored" value="1">
              <button name="event" value="login">go</button>
            </form>
            """.trimIndent(),
            "https://kyberia.sk/",
        )
        val form = LoginFormParser.find(doc)!!
        assertFalse(form.multipart)
        assertEquals(
            setOf("anticsrf" to "t", "remember" to "1", "event" to "login", "user" to "u", "pw" to "p"),
            form.fill("u", "p").toSet(),
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
