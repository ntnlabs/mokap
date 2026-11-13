package sk.ntnlabs.kyberia.data

import java.nio.charset.StandardCharsets
import org.jsoup.nodes.Document
import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.BookmarksParser
import sk.ntnlabs.kyberia.parse.Friend
import sk.ntnlabs.kyberia.parse.FriendsParser
import sk.ntnlabs.kyberia.parse.LastParser
import sk.ntnlabs.kyberia.parse.LastPost
import sk.ntnlabs.kyberia.parse.MyK
import sk.ntnlabs.kyberia.parse.MyKParser
import sk.ntnlabs.kyberia.parse.PageKind
import sk.ntnlabs.kyberia.parse.RepliesParser
import sk.ntnlabs.kyberia.parse.Reply
import sk.ntnlabs.kyberia.parse.Session
import sk.ntnlabs.kyberia.parse.UserInfo
import sk.ntnlabs.kyberia.parse.UserInfoParser

/** Všetko, čo dáva jedna požiadavka na /id/19/. */
class BookmarksPage(
    val bookmarks: List<Bookmark>,
    val replies: List<Reply>,
    val friends: List<Friend>,
    val last: List<LastPost>,
    val user: UserInfo,
    /** Tvoje príspevky, ktoré za posledných [myKHours] hodín dostali K. */
    val myK: List<MyK> = emptyList(),
    /** Za koľko hodín je blok [myK]: bežne 1, po dobiehaní ([KCatchUp]) celý výpadok. */
    val myKHours: Int = 1,
    /** Token session z tej istej stránky – len v pamäti (na „K za N h“). */
    val anticsrf: String? = null,
)

class BookmarksRepository(
    private val client: KyberiaClient,
    private val presence: Presence? = null,
    private val tokens: SessionTokenStore? = null,
) {

    /**
     * Stiahne /id/19/; o odhlásení rozhoduje [KyberiaClient.loggedInPage]. S [kHours] (dobiehanie K, [KCatchUp]) tú istú
     * stránku POST-om ako „Za N h“ – blok K za celý výpadok bez požiadavky navyše. Token na to je uložený so session;
     * bez neho alebo keď odpoveď nie je /id/19/ s blokom K, obyčajná /id/19/. Potom „používa MOKAP“ ([Presence]).
     */
    suspend fun load(kHours: Int? = null): BookmarksPage {
        val catchUp = kHours?.let { hours -> tokens?.readToken()?.let { token -> withInterval(hours, token)?.let { hours to it } } }
        val document = catchUp?.second ?: client.loggedInPage(KyberiaUrls.BOOKMARKS)
        val token = MyKParser.token(document)
        token?.let { tokens?.writeToken(it) }
        presence?.mark()
        return BookmarksPage(
            bookmarks = BookmarksParser.parse(document),
            replies = RepliesParser.parse(document),
            friends = FriendsParser.parse(document),
            last = LastParser.parse(document),
            user = UserInfoParser.parse(document),
            myK = MyKParser.parse(document),
            myKHours = catchUp?.first ?: 1,
            anticsrf = token,
        )
    }

    /** /id/19/ s blokom K za [hours] hodín; null = odpoveď nie je /id/19/ s blokom K (napr. zmena na kyberii). */
    private suspend fun withInterval(hours: Int, token: String): Document? {
        val page = client.postForm(KyberiaUrls.BOOKMARKS, MyKParser.fields(hours, token), StandardCharsets.UTF_8, multipart = false)
        return when (Session.classify(page.document)) {
            PageKind.LOGGED_IN -> page.document.takeIf(MyKParser::hasBlock)
            PageKind.LOGGED_OUT -> throw SessionExpiredException(LogoutDetail.of(KyberiaUrls.BOOKMARKS, page.url))
            PageKind.UNKNOWN -> null
        }
    }
}
