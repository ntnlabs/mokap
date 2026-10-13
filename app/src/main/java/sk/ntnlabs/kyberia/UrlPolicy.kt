package sk.ntnlabs.kyberia

import java.net.URI

/**
 * Rozhoduje, čo sa otvorí vo WebView a čo pôjde von do prehliadača.
 * Zámerne používa java.net.URI (nie android.net.Uri), aby sa dalo testovať na JVM.
 */
object UrlPolicy {

    private const val DOMAIN = "kyberia.sk"

    fun isInternal(url: String): Boolean {
        val uri = parse(url) ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        val host = uri.host?.lowercase() ?: return false
        return host == DOMAIN || host.endsWith(".$DOMAIN")
    }

    /** Staré http:// odkazy na kyberiu prepíše na https://, všetko ostatné nechá tak. */
    fun normalize(url: String): String {
        val uri = parse(url) ?: return url
        val host = uri.host?.lowercase() ?: return url
        val isKyberia = host == DOMAIN || host.endsWith(".$DOMAIN")
        if (!isKyberia || !uri.scheme.equals("http", ignoreCase = true)) return url
        return "https" + url.substring(uri.scheme.length)
    }

    /** URL z deep linku, ak je naša; inak domovská stránka. */
    fun startUrl(deepLink: String?, fallback: String): String =
        deepLink?.let(::normalize)?.takeIf(::isInternal) ?: fallback

    private fun parse(url: String): URI? = runCatching { URI(url) }.getOrNull()
}
