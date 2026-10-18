package sk.ntnlabs.kyberia

import java.net.URI

/**
 * Rozhoduje, ktoré URL patria kyberii. Kyberia cookies a požiadavky nesmú ísť nikam inam.
 * Zámerne používa java.net.URI (nie android.net.Uri), aby sa dalo testovať na JVM.
 */
object UrlPolicy {

    private const val DOMAIN = "kyberia.sk"

    fun isKyberiaHost(host: String): Boolean {
        val h = host.lowercase()
        return h == DOMAIN || h.endsWith(".$DOMAIN")
    }

    fun isInternal(url: String): Boolean {
        val uri = parse(url) ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        return uri.host?.let(::isKyberiaHost) ?: false
    }

    /** Staré http:// odkazy na kyberiu prepíše na https://, všetko ostatné nechá tak. */
    fun normalize(url: String): String {
        val uri = parse(url) ?: return url
        val host = uri.host ?: return url
        if (!isKyberiaHost(host) || !uri.scheme.equals("http", ignoreCase = true)) return url
        return "https" + url.substring(uri.scheme.length)
    }

    private fun parse(url: String): URI? = runCatching { URI(url) }.getOrNull()
}

/** Pevné stránky kyberie. Všetko je node s ID. */
object KyberiaUrls {
    const val BASE = "https://kyberia.sk"
    val BOOKMARKS = node(19)
    const val MAIL_ID = 24L
    val MAIL = node(MAIL_ID)
    val TOP_K = node(8340566)
    val LAST = node(35)
    val PEOPLE = node(27)
    val POPULATED = node(4830026)

    /** Ľahký endpoint, ktorý web volá kvôli počtu novej pošty. */
    const val MAIL_CHECK = "$BASE/ajax/check_new_mail.php"

    fun node(id: Long): String = "$BASE/id/$id/"
}
