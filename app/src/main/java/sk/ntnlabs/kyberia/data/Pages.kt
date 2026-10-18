package sk.ntnlabs.kyberia.data

import java.io.IOException
import org.jsoup.nodes.Document
import sk.ntnlabs.kyberia.parse.PageKind
import sk.ntnlabs.kyberia.parse.Session

class SessionExpiredException : Exception("Session na kyberii vypršala")

/** Kyberia vrátila niečo, čo nie je ani prihlásená stránka, ani login – skúsime neskôr. */
class UnexpectedPageException(code: Int) : IOException("Neočakávaná stránka (HTTP $code)")

/**
 * Stiahne stránku, ktorá musí byť prihlásená. [SessionExpiredException] len keď kyberia ukáže
 * prihlasovací formulár; iná neočakávaná stránka je dočasná chyba ([UnexpectedPageException]).
 */
internal suspend fun KyberiaClient.loggedInPage(url: String): Document {
    val page = get(url)
    when (Session.classify(page.document)) {
        PageKind.LOGGED_IN -> Unit
        PageKind.LOGGED_OUT -> throw SessionExpiredException()
        PageKind.UNKNOWN -> throw UnexpectedPageException(page.code)
    }
    return page.document
}
