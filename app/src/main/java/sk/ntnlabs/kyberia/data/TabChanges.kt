package sk.ntnlabs.kyberia.data

import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.LastPost
import sk.ntnlabs.kyberia.parse.Reply

/** Záložky, ktoré vedia ukázať „od tvojej poslednej návštevy sa tu niečo zmenilo“. */
enum class SeenArea { BOOKMARKS, REPLIES, LAST }

/** Porovnanie s tým, čo si naposledy videl na záložke. Čistý Kotlin kvôli testom. */
object TabChanges {

    /**
     * Základ pre bookmarky sa len znižuje: čo si medzitým prečítal na webe, už nie je „videné“,
     * takže nové príspevky v tom istom fóre sa znova ukážu.
     */
    fun lowerBookmarkBaseline(seen: Map<Long, Int>, current: List<Bookmark>): Map<Long, Int> {
        val now = BookmarkNews.snapshot(current)
        return seen.mapNotNull { (id, count) -> now[id]?.let { id to minOf(count, it) } }.toMap()
    }

    fun bookmarksChanged(seen: Map<Long, Int>, current: List<Bookmark>): Boolean =
        BookmarkNews.increases(seen, current).isNotEmpty()

    fun repliesChanged(seen: Set<String>, current: List<Reply>): Boolean = current.any { it.key !in seen }

    fun lastChanged(seen: Set<Long>, current: List<LastPost>): Boolean = current.any { it.nodeId !in seen }
}
