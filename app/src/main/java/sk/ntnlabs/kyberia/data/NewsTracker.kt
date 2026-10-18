package sk.ntnlabs.kyberia.data

import android.content.Context
import androidx.core.content.edit
import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.Friend
import sk.ntnlabs.kyberia.parse.LastPost
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

    /**
     * Na ktorých záložkách je niečo nové od poslednej návštevy. Prvé volanie len nastaví základ.
     */
    @Synchronized
    fun tabChanges(bookmarks: List<Bookmark>, replies: List<Reply>, last: List<LastPost>): Set<SeenArea> {
        val changed = mutableSetOf<SeenArea>()
        val seenBookmarks = prefs.getString(SEEN_BOOKMARKS, null)?.let(BookmarkNews::decode)
        if (seenBookmarks == null) {
            markSeen(SeenArea.BOOKMARKS, bookmarks, replies, last)
        } else {
            val lowered = TabChanges.lowerBookmarkBaseline(seenBookmarks, bookmarks)
            prefs.edit { putString(SEEN_BOOKMARKS, BookmarkNews.encode(lowered)) }
            if (TabChanges.bookmarksChanged(lowered, bookmarks)) changed += SeenArea.BOOKMARKS
        }
        val seenReplies = prefs.getString(SEEN_REPLIES, null)?.let(ReplyNews::decode)
        if (seenReplies == null) {
            markSeen(SeenArea.REPLIES, bookmarks, replies, last)
        } else if (TabChanges.repliesChanged(seenReplies, replies)) {
            changed += SeenArea.REPLIES
        }
        val seenLast = prefs.getString(SEEN_LAST, null)?.let(FriendNews::decode)
        if (seenLast == null) {
            markSeen(SeenArea.LAST, bookmarks, replies, last)
        } else if (TabChanges.lastChanged(seenLast, last)) {
            changed += SeenArea.LAST
        }
        return changed
    }

    /** Záložka je otvorená – čo na nej je, je videné. */
    @Synchronized
    fun markSeen(area: SeenArea, bookmarks: List<Bookmark>, replies: List<Reply>, last: List<LastPost>) {
        prefs.edit {
            when (area) {
                SeenArea.BOOKMARKS -> putString(SEEN_BOOKMARKS, BookmarkNews.encode(BookmarkNews.snapshot(bookmarks)))
                SeenArea.REPLIES -> putString(SEEN_REPLIES, ReplyNews.encode(replies.map { it.key }))
                SeenArea.LAST -> putString(SEEN_LAST, FriendNews.encode(last.map { it.nodeId }))
            }
        }
    }

    fun reset() {
        prefs.edit { clear() }
    }

    private companion object {
        const val KEY_BOOKMARKS = "bookmarks_snapshot"
        const val KEY_REPLIES = "replies_seen"
        const val KEY_FRIENDS = "friends_online"
        const val KEY_MAIL = "mail_unread"
        const val SEEN_BOOKMARKS = "seen_bookmarks"
        const val SEEN_REPLIES = "seen_replies"
        const val SEEN_LAST = "seen_last"
    }
}
