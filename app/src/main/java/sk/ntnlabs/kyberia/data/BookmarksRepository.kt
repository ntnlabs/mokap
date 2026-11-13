package sk.ntnlabs.kyberia.data

import java.io.IOException
import java.nio.charset.StandardCharsets
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
    /** Tvoje príspevky, ktoré za poslednú hodinu dostali K. */
    val myK: List<MyK> = emptyList(),
    /** Token session z tej istej stránky – len v pamäti (na „K za N h“). */
    val anticsrf: String? = null,
    /**
     * Blok K pre notifikácie – siaha aspoň po minulé spracovanie K: hodinový [myK], po výpadku blok za celý výpadok
     * ([KCatchUp]). Null = dobiehanie zlyhalo; K sa spracujú pri ďalšej kontrole, nech sa nestratia.
     */
    val kNews: List<MyK>? = myK,
)

class BookmarksRepository(private val client: KyberiaClient, private val presence: Presence? = null) {

    /**
     * Stiahne /id/19/; o odhlásení rozhoduje [loggedInPage]. S [kHours] ešte blok K za toľko hodín ([KCatchUp]) – ten
     * istý POST ako „Za N h“, raz po výpadku. Až potom „používa MOKAP“ ([Presence]): aj POST na /id/19/ nastaví „kde je“.
     */
    suspend fun load(kHours: Int? = null): BookmarksPage {
        val document = client.loggedInPage(KyberiaUrls.BOOKMARKS)
        val myK = MyKParser.parse(document)
        val kNews = if (kHours == null) myK else MyKParser.token(document)?.let { catchUpK(kHours, it) }
        presence?.mark()
        return BookmarksPage(
            bookmarks = BookmarksParser.parse(document),
            replies = RepliesParser.parse(document),
            friends = FriendsParser.parse(document),
            last = LastParser.parse(document),
            user = UserInfoParser.parse(document),
            myK = myK,
            anticsrf = MyKParser.token(document),
            kNews = kNews,
        )
    }

    /** Blok K za [hours] hodín; null pri chybe – stránka sama je stiahnutá, K počkajú na ďalšiu kontrolu. */
    private suspend fun catchUpK(hours: Int, token: String): List<MyK>? = try {
        val page = client.postForm(KyberiaUrls.BOOKMARKS, MyKParser.fields(hours, token), StandardCharsets.UTF_8, multipart = false)
        if (Session.classify(page.document) == PageKind.LOGGED_IN) MyKParser.parse(page.document) else null
    } catch (e: IOException) {
        null
    }
}
