package sk.ntnlabs.kyberia.data

import java.io.IOException
import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.BookmarksParser
import sk.ntnlabs.kyberia.parse.Friend
import sk.ntnlabs.kyberia.parse.FriendsParser
import sk.ntnlabs.kyberia.parse.LastParser
import sk.ntnlabs.kyberia.parse.LastPost
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
)

class SessionExpiredException : Exception("Session na kyberii vypršala")

/** Kyberia vrátila niečo, čo nie je ani prihlásená stránka, ani login – skúsime neskôr. */
class UnexpectedPageException(code: Int) : IOException("Neočakávaná stránka (HTTP $code)")

class BookmarksRepository(private val client: KyberiaClient) {

    /**
     * Stiahne /id/19/. [SessionExpiredException] len keď kyberia ukáže prihlasovací formulár;
     * iná neočakávaná stránka je dočasná chyba ([UnexpectedPageException]).
     */
    suspend fun load(): BookmarksPage {
        val page = client.get(KyberiaUrls.BOOKMARKS)
        val document = page.document
        when (Session.classify(document)) {
            PageKind.LOGGED_IN -> Unit
            PageKind.LOGGED_OUT -> throw SessionExpiredException()
            PageKind.UNKNOWN -> throw UnexpectedPageException(page.code)
        }
        return BookmarksPage(
            bookmarks = BookmarksParser.parse(document),
            replies = RepliesParser.parse(document),
            friends = FriendsParser.parse(document),
            last = LastParser.parse(document),
            user = UserInfoParser.parse(document),
        )
    }
}
