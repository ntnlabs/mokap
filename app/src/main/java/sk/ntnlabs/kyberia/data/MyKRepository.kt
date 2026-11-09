package sk.ntnlabs.kyberia.data

import java.nio.charset.StandardCharsets
import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.parse.MyK
import sk.ntnlabs.kyberia.parse.MyKParser
import sk.ntnlabs.kyberia.parse.PageKind
import sk.ntnlabs.kyberia.parse.Session

/**
 * „K za N h“: /id/19/ s iným intervalom bloku „your K nodes“ – ako keď na webe zadáš počet hodín a stlačíš „Hour“.
 * Len na požiadanie (otvorenie záložky), nikdy v pozadí. Token sa vezme z poslednej /id/19/ v pamäti; ak nie je,
 * stiahne sa najprv obyčajná /id/19/. Potom „používa MOKAP“ ([Presence]).
 */
class MyKRepository(private val client: KyberiaClient, private val latest: LatestPage, private val presence: Presence? = null) {

    suspend fun load(hours: Int): List<MyK> {
        val token = latest.flow.value?.page?.anticsrf
            ?: MyKParser.token(client.loggedInPage(KyberiaUrls.BOOKMARKS))
            ?: throw UnexpectedPageException(200)
        // Len prekreslenie stránky (bez event) – opakovanie po chybe spojenia nič nezdvojí.
        val page = client.postForm(KyberiaUrls.BOOKMARKS, MyKParser.fields(hours, token), StandardCharsets.UTF_8, multipart = false)
        when (Session.classify(page.document)) {
            PageKind.LOGGED_IN -> Unit
            PageKind.LOGGED_OUT -> throw SessionExpiredException(LogoutDetail.of(KyberiaUrls.BOOKMARKS, page.url))
            PageKind.UNKNOWN -> throw UnexpectedPageException(page.code)
        }
        presence?.mark()
        return MyKParser.parse(page.document)
    }
}
