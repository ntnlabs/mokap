package sk.ntnlabs.kyberia.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Nastavenia v SharedPreferences; UI ich sleduje cez [settings]. */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    val settings: StateFlow<Settings> = state.asStateFlow()

    val current: Settings get() = state.value

    @Synchronized
    fun update(change: (Settings) -> Settings) {
        val updated = change(state.value).let { it.copy(intervalMinutes = Settings.sanitizeInterval(it.intervalMinutes)) }
        prefs.edit {
            putInt(INTERVAL, updated.intervalMinutes)
            putBoolean(NOTIFY_REPLIES, updated.notifyReplies)
            putBoolean(NOTIFY_MAIL, updated.notifyMail)
            putBoolean(NOTIFY_BOOKMARKS, updated.notifyBookmarks)
            putBoolean(NOTIFY_FRIENDS, updated.notifyFriends)
            putBoolean(QUIET, updated.quietEnabled)
            putInt(QUIET_FROM, updated.quietFromHour)
            putInt(QUIET_TO, updated.quietToHour)
            putString(THEME, updated.theme.name)
            putString(BOOKMARK_SORT, updated.bookmarkSort.name)
            putBoolean(ONLY_NEWS, updated.bookmarksOnlyNews)
            putBoolean(REFRESH_ON_OPEN, updated.refreshOnOpen)
            putBoolean(ANIMATE_AVATARS, updated.animateAvatars)
            putBoolean(AVATARS_ONLY_WIFI, updated.avatarsOnlyWifi)
            putBoolean(LOGIN_BY_ID, updated.loginById)
        }
        state.value = updated
    }

    private fun read(): Settings {
        val defaults = Settings()
        return Settings(
            intervalMinutes = Settings.sanitizeInterval(prefs.getInt(INTERVAL, defaults.intervalMinutes)),
            notifyReplies = prefs.getBoolean(NOTIFY_REPLIES, defaults.notifyReplies),
            notifyMail = prefs.getBoolean(NOTIFY_MAIL, defaults.notifyMail),
            notifyBookmarks = prefs.getBoolean(NOTIFY_BOOKMARKS, defaults.notifyBookmarks),
            notifyFriends = prefs.getBoolean(NOTIFY_FRIENDS, defaults.notifyFriends),
            quietEnabled = prefs.getBoolean(QUIET, defaults.quietEnabled),
            quietFromHour = prefs.getInt(QUIET_FROM, defaults.quietFromHour),
            quietToHour = prefs.getInt(QUIET_TO, defaults.quietToHour),
            theme = enumOrDefault(prefs.getString(THEME, null), defaults.theme),
            bookmarkSort = enumOrDefault(prefs.getString(BOOKMARK_SORT, null), defaults.bookmarkSort),
            bookmarksOnlyNews = prefs.getBoolean(ONLY_NEWS, defaults.bookmarksOnlyNews),
            refreshOnOpen = prefs.getBoolean(REFRESH_ON_OPEN, defaults.refreshOnOpen),
            animateAvatars = prefs.getBoolean(ANIMATE_AVATARS, defaults.animateAvatars),
            avatarsOnlyWifi = prefs.getBoolean(AVATARS_ONLY_WIFI, defaults.avatarsOnlyWifi),
            loginById = prefs.getBoolean(LOGIN_BY_ID, defaults.loginById),
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        const val INTERVAL = "interval_minutes"
        const val NOTIFY_REPLIES = "notify_replies"
        const val NOTIFY_MAIL = "notify_mail"
        const val NOTIFY_BOOKMARKS = "notify_bookmarks"
        const val NOTIFY_FRIENDS = "notify_friends"
        const val QUIET = "quiet_enabled"
        const val QUIET_FROM = "quiet_from"
        const val QUIET_TO = "quiet_to"
        const val THEME = "theme"
        const val BOOKMARK_SORT = "bookmark_sort"
        const val ONLY_NEWS = "bookmarks_only_news"
        const val REFRESH_ON_OPEN = "refresh_on_open"
        const val ANIMATE_AVATARS = "animate_avatars"
        const val AVATARS_ONLY_WIFI = "avatars_only_wifi"
        const val LOGIN_BY_ID = "login_by_id"
    }
}
