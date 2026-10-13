package sk.ntnlabs.kyberia.data

import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.BookmarksParser
import sk.ntnlabs.kyberia.parse.Session
import sk.ntnlabs.kyberia.parse.UserInfo
import sk.ntnlabs.kyberia.parse.UserInfoParser

class BookmarksPage(val bookmarks: List<Bookmark>, val user: UserInfo)

class SessionExpiredException : Exception("Session na kyberii vypršala")

class BookmarksRepository(private val client: KyberiaClient) {

    /** Stiahne /id/19/; hodí [SessionExpiredException], ak nás kyberia medzitým odhlásila. */
    suspend fun load(): BookmarksPage {
        val document = client.get(KyberiaUrls.BOOKMARKS).document
        if (!Session.isLoggedIn(document)) throw SessionExpiredException()
        return BookmarksPage(BookmarksParser.parse(document), UserInfoParser.parse(document))
    }
}
