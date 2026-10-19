package sk.ntnlabs.kyberia

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.gif.AnimatedImageDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import sk.ntnlabs.kyberia.data.AuthRepository
import sk.ntnlabs.kyberia.data.BookmarksRepository
import sk.ntnlabs.kyberia.data.DiagnosticsStore
import sk.ntnlabs.kyberia.data.KyberiaClient
import sk.ntnlabs.kyberia.data.KyberiaCookieJar
import sk.ntnlabs.kyberia.data.LatestPage
import sk.ntnlabs.kyberia.data.MailRepository
import sk.ntnlabs.kyberia.data.NewsTracker
import sk.ntnlabs.kyberia.data.PeopleRepository
import sk.ntnlabs.kyberia.data.SessionStore
import sk.ntnlabs.kyberia.data.SettingsStore
import sk.ntnlabs.kyberia.notify.Notifier
import sk.ntnlabs.kyberia.work.CheckScheduler
import sk.ntnlabs.kyberia.work.NewsChecker

class MokapApp : Application(), SingletonImageLoader.Factory {

    val container: AppContainer by lazy { AppContainer(this) }

    // Aj notifikácie z pozadia musia počítať po slovensky („2 nové príspevky“).
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
    }

    /** Avatary: animované GIFy a disková cache, aby sa 2MB GIF neťahal pri každom zobrazení. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { container.imageCallFactory }))
                add(AnimatedImageDecoder.Factory())
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("images"))
                    .maxSizeBytes(IMAGE_CACHE_BYTES)
                    .build()
            }
            .crossfade(true)
            .build()

    private companion object {
        const val IMAGE_CACHE_BYTES = 100L * 1024 * 1024
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

    val imageCallFactory = client.imageCallFactory

    /** Len na diagnostiku – dokedy platia cookies, nie ich obsah. */
    fun cookieExpiry() = cookieJar.expiry()

    val settings = SettingsStore(context)
    val diagnostics = DiagnosticsStore(context)
    val scheduler = CheckScheduler(context, settings, diagnostics)
    val newsTracker = NewsTracker(context)
    val latestPage = LatestPage()
    val auth = AuthRepository(client, cookieJar, newsTracker, scheduler, latestPage)
    val bookmarks = BookmarksRepository(client)
    val mail = MailRepository(client)
    val people = PeopleRepository(client)
    val checker = NewsChecker(context, auth, bookmarks, mail, newsTracker, settings, diagnostics, latestPage, scheduler)
}
