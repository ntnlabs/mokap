package sk.ntnlabs.kyberia.data

import android.content.Context
import androidx.core.content.edit
import sk.ntnlabs.kyberia.parse.Bookmark

/** Pamätá si posledný videný stav bookmarkov, aby notifikácie hlásili len to, čo je naozaj nové. */
class BookmarkTracker(context: Context) {

    private val prefs = context.getSharedPreferences("check_state", Context.MODE_PRIVATE)

    /**
     * Uloží aktuálny stav a vráti, čo pribudlo od minula.
     * Úplne prvé volanie (napr. po prihlásení) iba nastaví základ a nehlási nič.
     */
    @Synchronized
    fun update(bookmarks: List<Bookmark>): List<Increase> {
        val previous = prefs.getString(KEY_SNAPSHOT, null)?.let(BookmarkNews::decode)
        prefs.edit { putString(KEY_SNAPSHOT, BookmarkNews.encode(BookmarkNews.snapshot(bookmarks))) }
        return previous?.let { BookmarkNews.increases(it, bookmarks) }.orEmpty()
    }

    fun reset() {
        prefs.edit { clear() }
    }

    private companion object {
        const val KEY_SNAPSHOT = "bookmarks_snapshot"
    }
}
