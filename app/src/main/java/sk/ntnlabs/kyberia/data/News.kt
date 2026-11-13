package sk.ntnlabs.kyberia.data

import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.Friend
import sk.ntnlabs.kyberia.parse.MyK
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

/** Nová pošta: hlási sa len nárast neprečítaných; keď počet klesne (prečítané), nič. */
object MailNews {

    fun added(previousUnread: Int, unread: Int): Int = (unread - previousUnread).coerceAtLeast(0)
}

/** Priatelia, ktorí sa od minulej kontroly objavili online. */
object FriendNews {

    fun cameOnline(previousIds: Set<Long>, current: List<Friend>): List<Friend> =
        current.filter { it.id !in previousIds }

    fun encode(ids: Collection<Long>): String = ids.joinToString(",")

    fun decode(value: String): Set<Long> = value.split(',').mapNotNull { it.trim().toLongOrNull() }.toSet()
}

/** K, ktoré pribudlo tvojmu príspevku od minulej kontroly; [total] = koľko K má príspevok teraz (null = nevedno). */
data class KGain(val nodeId: Long, val title: String, val added: Int, val total: Int?)

/**
 * K na tvoje príspevky medzi dvoma kontrolami – z bloku „your K nodes“ na /id/19/. Snímka = celkové K príspevkov
 * z posledného bloku. Príspevok, ktorý v nej bol, hlási nárast celkového počtu. Príspevok, ktorý v nej nebol, hlási
 * K za okno bloku: okno siaha najviac po minulú kontrolu ([KCatchUp]) a staršie K by ho bolo dostalo do snímky.
 */
object KNews {

    fun snapshot(current: List<MyK>): Map<Long, Int> =
        current.mapNotNull { k -> k.total?.let { k.nodeId to it } }.toMap()

    /** Pokles (odobraté K) sa nehlási. */
    fun gains(previous: Map<Long, Int>, current: List<MyK>): List<KGain> =
        current.mapNotNull { k ->
            val before = previous[k.nodeId]
            val added = when {
                before == null -> k.added
                k.total != null -> k.total - before
                else -> 0
            }
            if (added > 0) KGain(k.nodeId, k.title, added, k.total) else null
        }

    fun encode(snapshot: Map<Long, Int>): String = BookmarkNews.encode(snapshot)

    fun decode(value: String): Map<Long, Int> = BookmarkNews.decode(value)
}

/**
 * Dobiehanie K: bežná /id/19/ ukazuje K len za poslednú hodinu. Keď od minulého spracovania K ubehlo viac
 * (tichý režim, Doze, vypnutý telefón), kontrola si vypýta blok za celý výpadok – tou istou požiadavkou, nie navyše.
 */
object KCatchUp {

    /** Do 70 min stačí hodinový blok – inak by kontrola v 60-min intervale (WorkManager mešká) dobiehala vždy. */
    const val TOLERANCE_MILLIS = 70 * 60_000L

    /** Kyberia pri dlhšom intervale prechádza viac K – výpadok dlhší ako deň sa dobehne len za posledných 24 h. */
    const val MAX_HOURS = 24

    private const val HOUR_MILLIS = 60 * 60_000L

    /** Null = stačí bežná /id/19/; inak počet hodín, aby blok siahal aspoň po [lastMillis]. */
    fun hours(lastMillis: Long?, nowMillis: Long): Int? {
        if (lastMillis == null) return null
        val gap = nowMillis - lastMillis
        if (gap <= TOLERANCE_MILLIS) return null
        return ((gap + HOUR_MILLIS - 1) / HOUR_MILLIS).toInt().coerceAtMost(MAX_HOURS)
    }
}
