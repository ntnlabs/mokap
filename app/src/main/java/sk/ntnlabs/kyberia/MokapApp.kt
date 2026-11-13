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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import sk.ntnlabs.kyberia.data.AuthRepository
import sk.ntnlabs.kyberia.data.BookmarksRepository
import sk.ntnlabs.kyberia.data.DiagnosticsStore
import sk.ntnlabs.kyberia.data.ExternalHttp
import sk.ntnlabs.kyberia.data.KyberiaClient
import sk.ntnlabs.kyberia.data.KyberiaCookieJar
import sk.ntnlabs.kyberia.data.LatestPage
import sk.ntnlabs.kyberia.data.MailRepository
import sk.ntnlabs.kyberia.data.MyKRepository
import sk.ntnlabs.kyberia.data.NewsTracker
import sk.ntnlabs.kyberia.data.NodeRepository
import sk.ntnlabs.kyberia.data.PeopleRepository
import sk.ntnlabs.kyberia.data.TopKRepository
import sk.ntnlabs.kyberia.data.Presence
import sk.ntnlabs.kyberia.data.SessionStore
import sk.ntnlabs.kyberia.data.SettingsStore
import sk.ntnlabs.kyberia.notify.Notifier
import sk.ntnlabs.kyberia.ui.common.ImageSaver
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
        // Notifikácie ukazujú stav: fórum prečítané na webe a priatelia, ktorí už odišli, zrušia svoju notifikáciu –
        // pri každej /id/19/, z appky aj z pozadia.
        MainScope().launch {
            container.latestPage.flow.filterNotNull().collect {
                Notifier.cancelReadBookmarks(this@MokapApp, it.page.bookmarks)
                Notifier.cancelGoneFriends(this@MokapApp, it.page.friends)
            }
        }
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
    private val appContext = context.applicationContext
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
    private val presence = Presence(client) { settings.current.showPresence }
    val bookmarks = BookmarksRepository(client, presence)
    val mail = MailRepository(client)
    val people = PeopleRepository(client, presence)
    val nodes = NodeRepository(client)
    val topK = TopKRepository(client, presence)
    val myK = MyKRepository(client, latestPage, presence)

    /** Klient na cudzie obrázky – bez cookies, len https; vzniká až keď ho treba. */
    private val externalHttp by lazy { ExternalHttp.client() }

    /** Cudzie obrázky a náhľady videí v príspevkoch – len so zapnutým nastavením; vlastný klient bez cookies. */
    val externalImages: ImageLoader by lazy {
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { externalHttp }))
                add(AnimatedImageDecoder.Factory())
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("external_images"))
                    .maxSizeBytes(EXTERNAL_IMAGE_CACHE_BYTES)
                    .build()
            }
            .crossfade(true)
            .build()
    }
    val checker = NewsChecker(context, auth, bookmarks, mail, newsTracker, settings, diagnostics, latestPage, scheduler)

    /** Uloženie obrázka do galérie – z cache, inak tým istým klientom ako pri zobrazení. */
    val imageSaver by lazy {
        ImageSaver(appContext, imageCallFactory, { SingletonImageLoader.get(appContext) }, { externalHttp }, { externalImages })
    }

    /** Pošta otvorená v appke: kyberia ju označila ako prečítanú – notifikácia a číslo na záložke preč. */
    fun mailRead() {
        newsTracker.updateMail(0)
        Notifier.cancelMail(appContext)
    }
}

private const val EXTERNAL_IMAGE_CACHE_BYTES = 50L * 1024 * 1024
