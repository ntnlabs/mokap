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
import sk.ntnlabs.kyberia.MainActivity
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.data.Increase
import sk.ntnlabs.kyberia.parse.Reply

/** Lokálne notifikácie – žiadny Firebase, žiadny server. */
object Notifier {

    private const val CHANNEL_REPLIES = "replies"
    private const val CHANNEL_BOOKMARKS = "bookmarks"
    private const val CHANNEL_ACCOUNT = "account"
    private const val ID_BOOKMARKS = 1
    private const val ID_SESSION = 2
    private const val ID_REPLIES = 3
    private const val MAX_LINES = 6

    fun createChannels(context: Context) {
        NotificationManagerCompat.from(context).createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(CHANNEL_REPLIES, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName(context.getString(R.string.channel_replies))
                    .build(),
                NotificationChannelCompat.Builder(CHANNEL_BOOKMARKS, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName(context.getString(R.string.channel_bookmarks))
                    .build(),
                NotificationChannelCompat.Builder(CHANNEL_ACCOUNT, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName(context.getString(R.string.channel_account))
                    .build(),
            ),
        )
    }

    fun bookmarks(context: Context, increases: List<Increase>) {
        val total = increases.sumOf { it.added }
        val title = context.resources.getQuantityString(R.plurals.notify_bookmarks_title, total, total)
        val lines = increases.map { "+${it.added}  ${it.bookmark.title}" }
        val style = NotificationCompat.InboxStyle().also { style -> lines.take(MAX_LINES).forEach(style::addLine) }
        post(context, ID_BOOKMARKS, CHANNEL_BOOKMARKS, title, increases.joinToString(", ") { it.bookmark.title }, style)
    }

    fun replies(context: Context, replies: List<Reply>) {
        val title = if (replies.size == 1) {
            context.getString(R.string.notify_reply_from, replies.single().author)
        } else {
            context.resources.getQuantityString(R.plurals.notify_replies_title, replies.size, replies.size)
        }
        val lines = replies.map { "${it.author}: ${it.snippet}" }
        val style = NotificationCompat.InboxStyle().also { style -> lines.take(MAX_LINES).forEach(style::addLine) }
        val text = if (replies.size == 1) replies.single().snippet else lines.first()
        post(context, ID_REPLIES, CHANNEL_REPLIES, title, text, style)
    }

    fun sessionExpired(context: Context) {
        post(
            context,
            ID_SESSION,
            CHANNEL_ACCOUNT,
            context.getString(R.string.notify_session_expired_title),
            context.getString(R.string.notify_session_expired_text),
            style = null,
        )
    }

    @SuppressLint("MissingPermission") // oprávnenie sa kontroluje hneď na začiatku
    private fun post(
        context: Context,
        id: Int,
        channel: String,
        title: String,
        text: String,
        style: NotificationCompat.Style?,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_mokap)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(style)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
