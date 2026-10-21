package sk.ntnlabs.kyberia.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.MainActivity
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.data.Increase
import sk.ntnlabs.kyberia.data.Nsfw
import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.Friend
import sk.ntnlabs.kyberia.parse.Reply
import sk.ntnlabs.kyberia.ui.common.Web
import sk.ntnlabs.kyberia.work.NetworkProbe

/**
 * Lokálne notifikácie – žiadny Firebase, žiadny server.
 * Každá odpoveď a každé fórum má vlastnú notifikáciu, ktorá rovno otvorí node na webe;
 * súhrn skupiny a ostatné notifikácie otvoria appku na správnej záložke.
 */
object Notifier {

    private const val CHANNEL_REPLIES = "replies"
    private const val CHANNEL_MAIL = "mail"
    private const val CHANNEL_BOOKMARKS = "bookmarks"
    private const val CHANNEL_FRIENDS = "friends"
    private const val CHANNEL_ACCOUNT = "account"
    private const val GROUP_REPLIES = "sk.ntnlabs.kyberia.REPLIES"
    private const val GROUP_BOOKMARKS = "sk.ntnlabs.kyberia.BOOKMARKS"
    private const val ID_BOOKMARKS = 1
    private const val ID_SESSION = 2
    private const val ID_REPLIES = 3
    private const val ID_FRIENDS = 4
    private const val ID_MAIL = 5
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
                NotificationChannelCompat.Builder(CHANNEL_MAIL, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName(context.getString(R.string.channel_mail))
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
                    .setSubText(Nsfw.label(reply.nodeTitle))
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

    /**
     * Jedna notifikácia s počtom neprečítaných a tým, kto písal naposledy; ťuknutie otvorí poštu na webe.
     * [silent] = časť pošty je prečítaná – len opraví číslo v notifikácii, ak ešte visí, bez zvuku.
     */
    fun mail(context: Context, unread: Int, lastFrom: String?, silent: Boolean = false) {
        if (silent && !isActive(context, ID_MAIL)) return
        val title = lastFrom?.let { context.getString(R.string.notify_mail_from, it) }
            ?: context.getString(R.string.notify_mail_title)
        post(
            context,
            ID_MAIL,
            builder(
                context,
                CHANNEL_MAIL,
                title,
                context.resources.getQuantityString(R.plurals.notify_mail_unread, unread, unread),
            ).setNumber(unread)
                .setSilent(silent)
                .setContentIntent(openWeb(context, ID_MAIL, KyberiaUrls.MAIL_ID)),
        )
    }

    fun cancelMail(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_MAIL)
    }

    fun bookmarks(context: Context, increases: List<Increase>) {
        increases.take(MAX_ITEMS).forEach { increase ->
            val bookmark = increase.bookmark
            val id = bookmarkId(bookmark.id)
            // Celkový počet nových vo fóre (ako „N NEW Child“ na webe), nie prírastok – notifikácia prepíše tú staršiu.
            val count = bookmark.newChildren
            val text = context.resources.getQuantityString(R.plurals.notify_bookmark_new, count, count)
            post(
                context,
                id,
                // Názov fóra je to hlavné, počet stačí pod ním.
                builder(context, CHANNEL_BOOKMARKS, Nsfw.label(bookmark.title), text)
                    .setGroup(GROUP_BOOKMARKS)
                    .setContentIntent(openWeb(context, id, bookmark.id)),
            )
        }
        if (increases.size > 1) {
            val total = increases.sumOf { it.bookmark.newChildren }
            val title = context.resources.getQuantityString(R.plurals.notify_bookmarks_title, total, total)
            val lines = increases.map { "${it.bookmark.newChildren}  ${Nsfw.label(it.bookmark.title)}" }
            post(
                context,
                ID_BOOKMARKS,
                builder(context, CHANNEL_BOOKMARKS, title, increases.joinToString(", ") { Nsfw.label(it.bookmark.title) })
                    .setStyle(inbox(lines))
                    .setGroup(GROUP_BOOKMARKS)
                    .setGroupSummary(true)
                    .setContentIntent(openApp(context, MainActivity.TAB_BOOKMARKS)),
            )
        }
    }

    /** Fórum prečítané na webe – jeho notifikácia už neplatí; keď neostane žiadna, zmizne aj súhrn. */
    fun cancelReadBookmarks(context: Context, bookmarks: List<Bookmark>) {
        val manager = NotificationManagerCompat.from(context)
        val active = activeIds(context)
        val unread = bookmarks.filter { it.newChildren > 0 }.map { bookmarkId(it.id) }.toSet()
        val read = bookmarks.filter { it.newChildren == 0 }.map { bookmarkId(it.id) }
            .filter { it in active && it !in unread }.toSet()
        if (read.isEmpty()) return
        read.forEach(manager::cancel)
        val left = active.any { it in ID_BOOKMARK_BASE until ID_BOOKMARK_BASE + ID_RANGE && it !in read }
        if (!left) manager.cancel(ID_BOOKMARKS)
    }

    fun friendsOnline(context: Context, friends: List<Friend>) {
        val notification = if (friends.size == 1) {
            // Nick je už v titulku; text povie len to, kde práve je.
            val friend = friends.single()
            builder(
                context,
                CHANNEL_FRIENDS,
                context.getString(R.string.notify_friend_online, friend.nick),
                friend.locationTitle?.let { context.getString(R.string.friends_location, Nsfw.label(it)) },
            )
        } else {
            val title = context.resources.getQuantityString(R.plurals.notify_friends_online, friends.size, friends.size)
            builder(context, CHANNEL_FRIENDS, title, friends.joinToString(", ") { it.nick })
                .setStyle(inbox(friends.map { friend -> friend.locationTitle?.let { "${friend.nick} – ${Nsfw.label(it)}" } ?: friend.nick }))
        }
        post(context, ID_FRIENDS, notification.setContentIntent(openApp(context, MainActivity.TAB_FRIENDS)))
    }

    /** [dataSaverBlocks] = šetrič dát appke na pozadí zakázal sieť – povie to a ponúkne povolenie. */
    fun sessionExpired(context: Context, dataSaverBlocks: Boolean) {
        val text = context.getString(
            if (dataSaverBlocks) R.string.notify_session_expired_data_saver else R.string.notify_session_expired_text,
        )
        val builder = builder(context, CHANNEL_ACCOUNT, context.getString(R.string.notify_session_expired_title), text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp(context, tab = null))
        if (dataSaverBlocks) {
            builder.addAction(
                0,
                context.getString(R.string.allow_unrestricted_data),
                PendingIntent.getActivity(
                    context,
                    ID_SESSION,
                    NetworkProbe.unrestrictedDataIntent(context),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        }
        post(context, ID_SESSION, builder)
    }

    /** Po prihlásení stará „Odhlásilo ťa“ už neplatí. */
    fun cancelSessionExpired(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_SESSION)
    }

    private fun bookmarkId(id: Long) = ID_BOOKMARK_BASE + id.mod(ID_RANGE)

    private fun activeIds(context: Context): Set<Int> =
        context.getSystemService(NotificationManager::class.java).activeNotifications.map { it.id }.toSet()

    private fun isActive(context: Context, id: Int) = id in activeIds(context)

    private fun builder(context: Context, channel: String, title: String, text: String?) =
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
            Web.intent(context, KyberiaUrls.node(nodeId)),
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
