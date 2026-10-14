package sk.ntnlabs.kyberia.data

import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.BookmarksParser
import sk.ntnlabs.kyberia.parse.RepliesParser
import sk.ntnlabs.kyberia.parse.Reply
import sk.ntnlabs.kyberia.parse.Session
import sk.ntnlabs.kyberia.parse.UserInfo
import sk.ntnlabs.kyberia.parse.UserInfoParser

/** Všetko, čo dáva jedna požiadavka na /id/19/. */
class BookmarksPage(val bookmarks: List<Bookmark>, val replies: List<Reply>, val user: UserInfo)

class SessionExpiredException : Exception("Session na kyberii vypršala")

class BookmarksRepository(private val client: KyberiaClient) {

    /** Stiahne /id/19/; hodí [SessionExpiredException], ak nás kyberia medzitým odhlásila. */
    suspend fun load(): BookmarksPage {
        val document = client.get(KyberiaUrls.BOOKMARKS).document
        if (!Session.isLoggedIn(document)) throw SessionExpiredException()
        return BookmarksPage(
            bookmarks = BookmarksParser.parse(document),
            replies = RepliesParser.parse(document),
            user = UserInfoParser.parse(document),
        )
    }
}
