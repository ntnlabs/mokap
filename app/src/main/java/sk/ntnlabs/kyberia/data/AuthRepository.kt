package sk.ntnlabs.kyberia.data

import android.content.Context
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.parse.Forms
import sk.ntnlabs.kyberia.parse.LoginFormParser
import sk.ntnlabs.kyberia.parse.Session
import sk.ntnlabs.kyberia.work.CheckScheduler

enum class LoginResult { Success, Rejected, FormNotFound }

class AuthRepository(
    private val context: Context,
    private val client: KyberiaClient,
    private val cookies: KyberiaCookieJar,
    private val tracker: BookmarkTracker,
) {
    private val _loggedIn = MutableStateFlow(cookies.hasCookies())
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    /** Heslo sa použije len v tomto volaní na odoslanie formulára, nikam sa neukladá. */
    suspend fun login(username: String, password: String): LoginResult {
        cookies.clear()
        val page = client.get(KyberiaUrls.HOME)
        val form = LoginFormParser.find(page.document) ?: return LoginResult.FormNotFound
        val response = client.postForm(form.action, form.fill(username, password), form.charset)
        val ok = Session.isLoggedIn(response.document) ||
            Session.isLoggedIn(client.get(KyberiaUrls.BOOKMARKS).document)
        if (!ok) {
            cookies.clear()
            return LoginResult.Rejected
        }
        _loggedIn.value = true
        CheckScheduler.schedule(context)
        return LoginResult.Success
    }

    /** Odhlási aj na serveri (ak sa dá), lokálne vždy. */
    suspend fun logout() {
        try {
            val page = client.get(KyberiaUrls.BOOKMARKS)
            val form = Forms.find(page.document) { Forms.submitButton(it, LOGOUT) != null }
            if (form != null) {
                val fields = Forms.fields(form) + listOfNotNull(Forms.submitButton(form, LOGOUT))
                client.postForm(Forms.action(form, page.document), fields, page.document.charset())
            }
        } catch (e: IOException) {
            // Bez siete stačí zahodiť cookies lokálne.
        }
        sessionEnded()
    }

    /** Server nás odhlásil alebo sa používateľ odhlásil. */
    fun sessionEnded() {
        cookies.clear()
        tracker.reset()
        CheckScheduler.cancel(context)
        _loggedIn.value = false
    }

    private companion object {
        const val LOGOUT = "logout"
    }
}
