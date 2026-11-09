package sk.ntnlabs.kyberia.data

import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.BookmarksParser
import sk.ntnlabs.kyberia.parse.Friend
import sk.ntnlabs.kyberia.parse.FriendsParser
import sk.ntnlabs.kyberia.parse.LastParser
import sk.ntnlabs.kyberia.parse.LastPost
import sk.ntnlabs.kyberia.parse.MyK
import sk.ntnlabs.kyberia.parse.MyKParser
import sk.ntnlabs.kyberia.parse.RepliesParser
import sk.ntnlabs.kyberia.parse.Reply
import sk.ntnlabs.kyberia.parse.UserInfo
import sk.ntnlabs.kyberia.parse.UserInfoParser

/** Všetko, čo dáva jedna požiadavka na /id/19/. */
class BookmarksPage(
    val bookmarks: List<Bookmark>,
    val replies: List<Reply>,
    val friends: List<Friend>,
    val last: List<LastPost>,
    val user: UserInfo,
    /** Tvoje príspevky, ktoré za poslednú hodinu dostali K. */
    val myK: List<MyK> = emptyList(),
    /** Token session z tej istej stránky – len v pamäti (na „K za N h“). */
    val anticsrf: String? = null,
)

class BookmarksRepository(private val client: KyberiaClient, private val presence: Presence? = null) {

    /** Stiahne /id/19/; o odhlásení rozhoduje [loggedInPage]. Potom „používa MOKAP“ ([Presence]). */
    suspend fun load(): BookmarksPage {
        val document = client.loggedInPage(KyberiaUrls.BOOKMARKS)
        presence?.mark()
        return BookmarksPage(
            bookmarks = BookmarksParser.parse(document),
            replies = RepliesParser.parse(document),
            friends = FriendsParser.parse(document),
            last = LastParser.parse(document),
            user = UserInfoParser.parse(document),
            myK = MyKParser.parse(document),
            anticsrf = MyKParser.token(document),
        )
    }
}
