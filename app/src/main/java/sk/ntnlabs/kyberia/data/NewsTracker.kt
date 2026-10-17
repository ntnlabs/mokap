package sk.ntnlabs.kyberia.data

import android.content.Context
import androidx.core.content.edit
import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.Friend
import sk.ntnlabs.kyberia.parse.Reply

/**
 * Pamätá si naposledy videný stav, aby notifikácie hlásili len to, čo je naozaj nové.
 * Úplne prvé volanie (napr. po prihlásení) iba nastaví základ a nehlási nič.
 */
class NewsTracker(context: Context) {

    private val prefs = context.getSharedPreferences("check_state", Context.MODE_PRIVATE)

    @Synchronized
    fun updateBookmarks(bookmarks: List<Bookmark>): List<Increase> {
        val previous = prefs.getString(KEY_BOOKMARKS, null)?.let(BookmarkNews::decode)
        prefs.edit { putString(KEY_BOOKMARKS, BookmarkNews.encode(BookmarkNews.snapshot(bookmarks))) }
        return previous?.let { BookmarkNews.increases(it, bookmarks) }.orEmpty()
    }

    @Synchronized
    fun updateReplies(replies: List<Reply>, selfId: Long?): List<Reply> {
        val previous = prefs.getString(KEY_REPLIES, null)?.let(ReplyNews::decode)
        prefs.edit { putString(KEY_REPLIES, ReplyNews.encode(replies.map { it.key })) }
        return previous?.let { ReplyNews.fresh(it, replies, selfId) }.orEmpty()
    }

    @Synchronized
    fun updateFriends(friends: List<Friend>): List<Friend> {
        val previous = prefs.getString(KEY_FRIENDS, null)?.let(FriendNews::decode)
        prefs.edit { putString(KEY_FRIENDS, FriendNews.encode(friends.map { it.id })) }
        return previous?.let { FriendNews.cameOnline(it, friends) }.orEmpty()
    }

    /** Koľko neprečítaných správ pribudlo od minulej kontroly. */
    @Synchronized
    fun updateMail(unread: Int): Int {
        val previous = if (prefs.contains(KEY_MAIL)) prefs.getInt(KEY_MAIL, 0) else null
        prefs.edit { putInt(KEY_MAIL, unread) }
        return previous?.let { MailNews.added(it, unread) } ?: 0
    }

    fun reset() {
        prefs.edit { clear() }
    }

    private companion object {
        const val KEY_BOOKMARKS = "bookmarks_snapshot"
        const val KEY_REPLIES = "replies_seen"
        const val KEY_FRIENDS = "friends_online"
        const val KEY_MAIL = "mail_unread"
    }
}
