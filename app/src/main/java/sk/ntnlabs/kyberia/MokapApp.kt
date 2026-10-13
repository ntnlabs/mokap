package sk.ntnlabs.kyberia

import android.app.Application
import android.content.Context
import sk.ntnlabs.kyberia.data.AuthRepository
import sk.ntnlabs.kyberia.data.BookmarkTracker
import sk.ntnlabs.kyberia.data.BookmarksRepository
import sk.ntnlabs.kyberia.data.KyberiaClient
import sk.ntnlabs.kyberia.data.KyberiaCookieJar
import sk.ntnlabs.kyberia.data.SessionStore
import sk.ntnlabs.kyberia.notify.Notifier

class MokapApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
    }
}

/** Ručné DI – na pár tried netreba framework. */
class AppContainer(context: Context) {
    private val cookieJar = KyberiaCookieJar(SessionStore(context))

    // Nech admini kyberie vidia, čo k nim chodí a kde to nájdu.
    private val client = KyberiaClient(
        cookieJar,
        "mokap/${BuildConfig.VERSION_NAME} (Android; +https://github.com/ntnlabs/mokap)",
    )

    val bookmarkTracker = BookmarkTracker(context)
    val auth = AuthRepository(context, client, cookieJar, bookmarkTracker)
    val bookmarks = BookmarksRepository(client)
}
