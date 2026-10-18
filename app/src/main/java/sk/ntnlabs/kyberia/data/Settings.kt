package sk.ntnlabs.kyberia.data

enum class ThemeMode { SYSTEM, LIGHT, DARK, DIM }

enum class BookmarkSort { ALPHABET, NEWS }

/** Používateľské nastavenia. Čistý Kotlin – ukladanie rieši [SettingsStore]. */
data class Settings(
    val intervalMinutes: Int = DEFAULT_INTERVAL,
    val notifyReplies: Boolean = true,
    val notifyMail: Boolean = true,
    val notifyBookmarks: Boolean = true,
    val notifyFriends: Boolean = true,
    val quietEnabled: Boolean = false,
    val quietFromHour: Int = 22,
    val quietToHour: Int = 6,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val bookmarkSort: BookmarkSort = BookmarkSort.ALPHABET,
    val bookmarksOnlyNews: Boolean = false,
    val refreshOnOpen: Boolean = true,
    val animateAvatars: Boolean = true,
    val avatarsOnlyWifi: Boolean = false,
    /** Posledný spôsob prihlásenia (nick/ID) – len voľba, nick ani ID sa neukladajú. */
    val loginById: Boolean = false,
) {
    fun isQuiet(hour: Int): Boolean = quietEnabled && QuietHours.contains(quietFromHour, quietToHour, hour)

    /** Či smie avatar ísť po sieti; na meranej sieti (mobilné dáta, hotspot) len ak to nie je zakázané. */
    fun avatarDownloadAllowed(metered: Boolean): Boolean = !avatarsOnlyWifi || !metered

    companion object {
        /** Android pustí periodickú prácu najčastejšie raz za 15 minút. */
        val INTERVALS = listOf(15, 20, 30, 45, 60)
        const val DEFAULT_INTERVAL = 15

        fun sanitizeInterval(minutes: Int): Int = INTERVALS.firstOrNull { it >= minutes } ?: INTERVALS.last()
    }
}

object QuietHours {
    /** Či hodina patrí do intervalu [from, to); funguje aj cez polnoc (22 → 6). Rovnaké hodnoty = vypnuté. */
    fun contains(from: Int, to: Int, hour: Int): Boolean = when {
        from == to -> false
        from < to -> hour in from until to
        else -> hour >= from || hour < to
    }
}

/** Krátky zápis dĺžky trvania: „< 1 min“, „84 min“, „2 h 5 min“. */
object Durations {
    fun short(seconds: Long): String {
        val minutes = seconds / 60
        val hours = minutes / 60
        return when {
            minutes < 1 -> "< 1 min"
            hours < 2 -> "$minutes min"
            minutes % 60 == 0L -> "$hours h"
            else -> "$hours h ${minutes % 60} min"
        }
    }
}
