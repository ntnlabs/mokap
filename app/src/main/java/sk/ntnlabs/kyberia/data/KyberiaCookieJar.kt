package sk.ntnlabs.kyberia.data

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import sk.ntnlabs.kyberia.UrlPolicy

/** Úložisko pre serializované cookies; abstrakcia kvôli JVM testom. */
interface CookieStorage {
    fun read(): String?
    fun write(value: String?)
}

/**
 * Drží cookies iba pre kyberia.sk a perzistuje ich cez [CookieStorage].
 * Cookies iných domén zahadzuje a kyberia cookies nikdy nepošle inam.
 */
class KyberiaCookieJar(
    private val storage: CookieStorage,
    private val clock: () -> Long = System::currentTimeMillis,
) : CookieJar {

    private val cookies = LinkedHashMap<String, Cookie>()

    init {
        restore()
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (!UrlPolicy.isKyberiaHost(url.host)) return
        cookies.forEach { this.cookies[key(it)] = it }
        prune()
        persist()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (!UrlPolicy.isKyberiaHost(url.host)) return emptyList()
        prune()
        return cookies.values.filter { it.matches(url) }
    }

    @Synchronized
    fun hasCookies(): Boolean {
        prune()
        return cookies.isNotEmpty()
    }

    @Synchronized
    fun clear() {
        cookies.clear()
        storage.write(null)
    }

    private fun prune() {
        val now = clock()
        cookies.values.removeAll { it.expiresAt <= now }
    }

    private fun persist() {
        storage.write(cookies.values.joinToString("\n") { "${it.domain}\t$it" })
    }

    private fun restore() {
        storage.read()?.lineSequence()?.forEach { line ->
            val parts = line.split('\t', limit = 2)
            if (parts.size != 2) return@forEach
            val url = "https://${parts[0]}/".toHttpUrlOrNull() ?: return@forEach
            Cookie.parse(url, parts[1])?.let { cookies[key(it)] = it }
        }
        prune()
    }

    private fun key(cookie: Cookie) = "${cookie.name}|${cookie.domain}|${cookie.path}"
}
