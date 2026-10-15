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
            putBoolean(NOTIFY_BOOKMARKS, updated.notifyBookmarks)
            putBoolean(NOTIFY_FRIENDS, updated.notifyFriends)
            putBoolean(QUIET, updated.quietEnabled)
            putInt(QUIET_FROM, updated.quietFromHour)
            putInt(QUIET_TO, updated.quietToHour)
        }
        state.value = updated
    }

    private fun read(): Settings {
        val defaults = Settings()
        return Settings(
            intervalMinutes = Settings.sanitizeInterval(prefs.getInt(INTERVAL, defaults.intervalMinutes)),
            notifyReplies = prefs.getBoolean(NOTIFY_REPLIES, defaults.notifyReplies),
            notifyBookmarks = prefs.getBoolean(NOTIFY_BOOKMARKS, defaults.notifyBookmarks),
            notifyFriends = prefs.getBoolean(NOTIFY_FRIENDS, defaults.notifyFriends),
            quietEnabled = prefs.getBoolean(QUIET, defaults.quietEnabled),
            quietFromHour = prefs.getInt(QUIET_FROM, defaults.quietFromHour),
            quietToHour = prefs.getInt(QUIET_TO, defaults.quietToHour),
        )
    }

    private companion object {
        const val INTERVAL = "interval_minutes"
        const val NOTIFY_REPLIES = "notify_replies"
        const val NOTIFY_BOOKMARKS = "notify_bookmarks"
        const val NOTIFY_FRIENDS = "notify_friends"
        const val QUIET = "quiet_enabled"
        const val QUIET_FROM = "quiet_from"
        const val QUIET_TO = "quiet_to"
    }
}
