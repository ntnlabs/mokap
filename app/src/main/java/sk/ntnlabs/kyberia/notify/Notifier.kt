package sk.ntnlabs.kyberia.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.MainActivity
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.data.Increase
import sk.ntnlabs.kyberia.parse.Friend
import sk.ntnlabs.kyberia.parse.Reply

/**
 * Lokálne notifikácie – žiadny Firebase, žiadny server.
 * Každá odpoveď a každé fórum má vlastnú notifikáciu, ktorá rovno otvorí node na webe;
 * súhrn skupiny a ostatné notifikácie otvoria appku na správnej záložke.
 */
object Notifier {

    private const val CHANNEL_REPLIES = "replies"
    private const val CHANNEL_BOOKMARKS = "bookmarks"
    private const val CHANNEL_FRIENDS = "friends"
    private const val CHANNEL_ACCOUNT = "account"
    private const val GROUP_REPLIES = "sk.ntnlabs.kyberia.REPLIES"
    private const val GROUP_BOOKMARKS = "sk.ntnlabs.kyberia.BOOKMARKS"
    private const val ID_BOOKMARKS = 1
    private const val ID_SESSION = 2
    private const val ID_REPLIES = 3
    private const val ID_FRIENDS = 4
    private const val ID_REPLY_BASE = 10_000
    private const val ID_BOOKMARK_BASE = 20_000
    private const val ID_RANGE = 10_000
    private const val MAX_ITEMS = 5

    fun createChannels(context: Context) {
        NotificationManagerCompat.from(context).createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(CHANNEL_REPLIES, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName(context.getString(R.string.channel_replies))
                    .build(),
                NotificationChannelCompat.Builder(CHANNEL_BOOKMARKS, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName(context.getString(R.string.channel_bookmarks))
                    .build(),
                NotificationChannelCompat.Builder(CHANNEL_FRIENDS, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName(context.getString(R.string.channel_friends))
                    .build(),
                NotificationChannelCompat.Builder(CHANNEL_ACCOUNT, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName(context.getString(R.string.channel_account))
                    .build(),
            ),
        )
    }

    fun replies(context: Context, replies: List<Reply>) {
        replies.take(MAX_ITEMS).forEach { reply ->
            val id = ID_REPLY_BASE + reply.key.hashCode().mod(ID_RANGE)
            post(
                context,
                id,
                builder(context, CHANNEL_REPLIES, context.getString(R.string.notify_reply_from, reply.author), reply.snippet)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(reply.snippet))
                    .setSubText(reply.nodeTitle)
                    .setGroup(GROUP_REPLIES)
                    // Otvorí môj príspevok – odpoveď visí pod ním, takže je vidieť aj kontext.
                    .setContentIntent(openWeb(context, id, reply.nodeId)),
            )
        }
        if (replies.size > 1) {
            val title = context.resources.getQuantityString(R.plurals.notify_replies_title, replies.size, replies.size)
            val lines = replies.map { "${it.author}: ${it.snippet}" }
            post(
                context,
                ID_REPLIES,
                builder(context, CHANNEL_REPLIES, title, lines.first())
                    .setStyle(inbox(lines))
                    .setGroup(GROUP_REPLIES)
                    .setGroupSummary(true)
                    .setContentIntent(openApp(context, MainActivity.TAB_REPLIES)),
            )
        }
    }

    fun bookmarks(context: Context, increases: List<Increase>) {
        increases.take(MAX_ITEMS).forEach { increase ->
            val bookmark = increase.bookmark
            val id = ID_BOOKMARK_BASE + bookmark.id.mod(ID_RANGE)
            val title = context.resources.getQuantityString(R.plurals.notify_bookmark_new, increase.added, increase.added)
            post(
                context,
                id,
                builder(context, CHANNEL_BOOKMARKS, title, bookmark.title)
                    .setGroup(GROUP_BOOKMARKS)
                    .setContentIntent(openWeb(context, id, bookmark.id)),
            )
        }
        if (increases.size > 1) {
            val total = increases.sumOf { it.added }
            val title = context.resources.getQuantityString(R.plurals.notify_bookmarks_title, total, total)
            val lines = increases.map { "+${it.added}  ${it.bookmark.title}" }
            post(
                context,
                ID_BOOKMARKS,
                builder(context, CHANNEL_BOOKMARKS, title, increases.joinToString(", ") { it.bookmark.title })
                    .setStyle(inbox(lines))
                    .setGroup(GROUP_BOOKMARKS)
                    .setGroupSummary(true)
                    .setContentIntent(openApp(context, MainActivity.TAB_BOOKMARKS)),
            )
        }
    }

    fun friendsOnline(context: Context, friends: List<Friend>) {
        val title = if (friends.size == 1) {
            context.getString(R.string.notify_friend_online, friends.single().nick)
        } else {
            context.resources.getQuantityString(R.plurals.notify_friends_online, friends.size, friends.size)
        }
        post(
            context,
            ID_FRIENDS,
            builder(context, CHANNEL_FRIENDS, title, friends.joinToString(", ") { it.nick })
                .setContentIntent(openApp(context, MainActivity.TAB_FRIENDS)),
        )
    }

    fun sessionExpired(context: Context) {
        post(
            context,
            ID_SESSION,
            builder(
                context,
                CHANNEL_ACCOUNT,
                context.getString(R.string.notify_session_expired_title),
                context.getString(R.string.notify_session_expired_text),
            ).setContentIntent(openApp(context, tab = null)),
        )
    }

    private fun builder(context: Context, channel: String, title: String, text: String) =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_mokap)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)

    private fun inbox(lines: List<String>) =
        NotificationCompat.InboxStyle().also { style -> lines.take(MAX_ITEMS + 1).forEach(style::addLine) }

    /** Node na webe – kým appka nevie čítať nody sama. */
    private fun openWeb(context: Context, requestCode: Int, nodeId: Long): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(Intent.ACTION_VIEW, KyberiaUrls.node(nodeId).toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun openApp(context: Context, tab: String?): PendingIntent =
        PendingIntent.getActivity(
            context,
            // Rôzne requestCode, aby sa intenty jednotlivých záložiek neprepisovali.
            tab?.hashCode() ?: 0,
            MainActivity.intent(context, tab),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    @SuppressLint("MissingPermission") // oprávnenie sa kontroluje hneď na začiatku
    private fun post(context: Context, id: Int, builder: NotificationCompat.Builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }
}
