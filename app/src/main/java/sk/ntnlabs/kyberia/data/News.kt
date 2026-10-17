package sk.ntnlabs.kyberia.data

import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.Friend
import sk.ntnlabs.kyberia.parse.Reply

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

/** Nové odpovede na tvoje príspevky medzi dvoma kontrolami. */
object ReplyNews {

    /** Odpovede, ktoré v minulom zozname neboli; vlastné odpovede (od [selfId]) sa nehlásia. */
    fun fresh(previousKeys: Set<String>, current: List<Reply>, selfId: Long?): List<Reply> =
        current.filter { it.key !in previousKeys && (selfId == null || it.authorId != selfId) }

    fun encode(keys: Collection<String>): String = keys.joinToString("\n")

    fun decode(value: String): Set<String> = value.lineSequence().filter { it.isNotBlank() }.toSet()
}

/** Priatelia, ktorí sa od minulej kontroly objavili online. */
object FriendNews {

    fun cameOnline(previousIds: Set<Long>, current: List<Friend>): List<Friend> =
        current.filter { it.id !in previousIds }

    fun encode(ids: Collection<Long>): String = ids.joinToString(",")

    fun decode(value: String): Set<Long> = value.split(',').mapNotNull { it.trim().toLongOrNull() }.toSet()
}
