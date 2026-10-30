package sk.ntnlabs.kyberia.data

import org.jsoup.nodes.Document
import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.BookmarksParser
import sk.ntnlabs.kyberia.parse.Friend
import sk.ntnlabs.kyberia.parse.FriendsParser
import sk.ntnlabs.kyberia.parse.LastParser
import sk.ntnlabs.kyberia.parse.LastPost
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
)

class BookmarksRepository(
    private val client: KyberiaClient,
    /** Sťahovať node „používa MOKAP“ namiesto /id/19/ (nastavenie). */
    private val presence: () -> Boolean = { false },
) {

    /** Node mokapu nevyrenderoval bookmarky – do konca behu appky už len /id/19/, nie dve požiadavky pri každej kontrole. */
    @Volatile private var presenceBroken = false

    /** Stiahne /id/19/ (alebo node mokapu s rovnakou šablónou); o odhlásení rozhoduje [loggedInPage]. */
    suspend fun load(): BookmarksPage {
        val document = presencePage() ?: client.loggedInPage(KyberiaUrls.BOOKMARKS)
        return BookmarksPage(
            bookmarks = BookmarksParser.parse(document),
            replies = RepliesParser.parse(document),
            friends = FriendsParser.parse(document),
            last = LastParser.parse(document),
            user = UserInfoParser.parse(document),
        )
    }

    /**
     * Node mokapu, ak je zapnutý a naozaj vyrenderoval bookmarky. Inak null a v tej istej kontrole sa stiahne
     * /id/19/ – prázdne zoznamy by pri ďalšej kontrole spustili záplavu notifikácií o „nových“ veciach.
     */
    private suspend fun presencePage(): Document? {
        val url = KyberiaUrls.PRESENCE ?: return null
        if (presenceBroken || !presence()) return null
        val document = client.loggedInPage(url)
        if (BookmarksParser.isBookmarksPage(document)) return document
        presenceBroken = true
        return null
    }
}
