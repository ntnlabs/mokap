package sk.ntnlabs.kyberia.data

import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jsoup.nodes.Document
import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.parse.Forms
import sk.ntnlabs.kyberia.parse.LoginFormParser
import sk.ntnlabs.kyberia.parse.LoginType
import sk.ntnlabs.kyberia.parse.PeopleParser
import sk.ntnlabs.kyberia.parse.Session
import sk.ntnlabs.kyberia.parse.UserInfoParser
import sk.ntnlabs.kyberia.work.CheckScheduler

enum class LoginResult { Success, Rejected, FormNotFound }

class AuthRepository(
    private val client: KyberiaClient,
    private val cookies: KyberiaCookieJar,
    private val tracker: NewsTracker,
    private val scheduler: CheckScheduler,
    private val latest: LatestPage,
    private val ownIds: OwnIdStore? = null,
) {
    private val _loggedIn = MutableStateFlow(cookies.hasCookies())
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    /** Heslo sa použije len v tomto volaní na odoslanie formulára, nikam sa neukladá. */
    suspend fun login(username: String, password: String, type: LoginType = LoginType.NAME): LoginResult {
        cookies.clear()
        latest.clear()
        // Odhlásený /id/19/ ukáže čistý prihlasovací formulár (hlavná stránka je na parsovanie peklo).
        val page = client.get(KyberiaUrls.BOOKMARKS)
        val form = LoginFormParser.find(page.document)?.takeIf { it.supports(type) } ?: return LoginResult.FormNotFound
        val response = client.postForm(form.action, form.fill(username, password, type), form.charset, form.multipart)
        val loggedIn = response.document.takeIf(Session::isLoggedIn)
            ?: client.get(KyberiaUrls.BOOKMARKS).document.takeIf(Session::isLoggedIn)
        if (loggedIn == null) {
            cookies.clear()
            return LoginResult.Rejected
        }
        ownIds?.writeOwnId(ownId(username, type, loggedIn))
        _loggedIn.value = true
        scheduler.schedule()
        // Prihlásenie je čerstvá požiadavka – strážca rátá od nej.
        scheduler.touch()
        return LoginResult.Success
    }

    /**
     * Vlastné ID raz za session ([OwnId]): napísané ID, odkaz SELF, inak people podľa nicku (jedna požiadavka, len pri
     * prihlásení). Neúspech prihlásenie nepokazí – appka si poradí aj bez ID.
     */
    private suspend fun ownId(login: String, type: LoginType, page: Document): Long? {
        OwnId.typed(login, type)?.let { return it }
        UserInfoParser.parse(page).userId?.let { return it }
        return try {
            OwnId.fromPeople(PeopleParser.parse(client.loggedInPage(KyberiaUrls.PEOPLE)), login)
        } catch (e: IOException) {
            null
        } catch (e: SessionExpiredException) {
            null
        }
    }

    /** Odhlási aj na serveri (ak sa dá), lokálne vždy. */
    suspend fun logout() {
        try {
            val page = client.get(KyberiaUrls.BOOKMARKS)
            val form = Forms.find(page.document) { Forms.submitButton(it, LOGOUT) != null }
            if (form != null) {
                val fields = Forms.fields(form) + listOfNotNull(Forms.submitButton(form, LOGOUT))
                client.postForm(
                    Forms.action(form, page.document),
                    fields,
                    page.document.charset(),
                    Forms.isMultipart(form),
                )
            }
        } catch (e: IOException) {
            // Bez siete stačí zahodiť cookies lokálne.
        }
        sessionEnded()
    }

    /** Server nás odhlásil alebo sa používateľ odhlásil. */
    fun sessionEnded() {
        cookies.endSession()
        latest.clear()
        tracker.reset()
        scheduler.cancel()
        _loggedIn.value = false
    }

    private companion object {
        const val LOGOUT = "logout"
    }
}
