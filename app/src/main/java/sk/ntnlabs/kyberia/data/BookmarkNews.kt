package sk.ntnlabs.kyberia.data

import sk.ntnlabs.kyberia.parse.Bookmark

data class Increase(val bookmark: Bookmark, val added: Int)

/** Porovnanie počtov „NEW Child“ medzi dvoma kontrolami. Čistý Kotlin kvôli testom. */
object BookmarkNews {

    fun snapshot(bookmarks: List<Bookmark>): Map<Long, Int> =
        bookmarks.filter { it.newChildren > 0 }.associate { it.id to it.newChildren }

    /** Bookmarky, kde od minula pribudli odpovede. Keď počet klesol (prečítané), nič. */
    fun increases(previous: Map<Long, Int>, current: List<Bookmark>): List<Increase> =
        current.mapNotNull { bookmark ->
            val added = bookmark.newChildren - (previous[bookmark.id] ?: 0)
            if (added > 0) Increase(bookmark, added) else null
        }.sortedByDescending { it.added }

    fun encode(snapshot: Map<Long, Int>): String =
        snapshot.entries.joinToString(",") { "${it.key}:${it.value}" }

    fun decode(value: String): Map<Long, Int> =
        value.split(',').mapNotNull { entry ->
            val parts = entry.split(':')
            val id = parts.getOrNull(0)?.toLongOrNull()
            val count = parts.getOrNull(1)?.toIntOrNull()
            if (parts.size == 2 && id != null && count != null) id to count else null
        }.toMap()
}
