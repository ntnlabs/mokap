package sk.ntnlabs.kyberia.data

import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.Hotspot
import sk.ntnlabs.kyberia.parse.LastPost
import sk.ntnlabs.kyberia.parse.Reply

/**
 * Nody s „NSFW“ **v názve** – slovo sa ukáže ako 🔞 a na želanie sa node skryje. Obsah ani rodič sa
 * nekontrolujú: NSFW sa na kyberii dedí, takže všetko aj tak nechytíme, a v textoch by bolo priveľa omylov.
 */
object Nsfw {

    const val MARK = "🔞"

    // Samostatné slovo – aj „[NSFW]“ či „nsfw:“, ale nie časť iného slova; „K SFW“ nie je NSFW.
    private val WORD = Regex("""(?<!\p{L})nsfw(?!\p{L})""", RegexOption.IGNORE_CASE)

    fun matches(vararg texts: String?): Boolean = texts.any { it != null && WORD.containsMatchIn(it) }

    /** „Archív NSFW“ → „Archív 🔞“. */
    fun label(text: String): String = WORD.replace(text, MARK)

    fun bookmarks(list: List<Bookmark>, hide: Boolean): List<Bookmark> =
        if (hide) list.filterNot { matches(it.title) } else list

    fun replies(list: List<Reply>, hide: Boolean): List<Reply> =
        if (hide) list.filterNot { matches(it.nodeTitle) } else list

    fun last(list: List<LastPost>, hide: Boolean): List<LastPost> =
        if (hide) list.filterNot { matches(it.title) } else list

    fun hotspots(list: List<Hotspot>, hide: Boolean): List<Hotspot> =
        if (hide) list.filterNot { matches(it.title) } else list

    fun increases(list: List<Increase>, hide: Boolean): List<Increase> =
        if (hide) list.filterNot { matches(it.bookmark.title) } else list
}
