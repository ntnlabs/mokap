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

    /**
     * Node „používa MOKAP“: načíta sa na konci každého obnovenia, aby „kde je“ ukazovalo mokap
     * namiesto bookmarkov ([sk.ntnlabs.kyberia.data.Presence]). null = node zatiaľ nie je.
     */
    val PRESENCE_ID: Long? = 9346589L
    val PRESENCE: String? = PRESENCE_ID?.let(::node)

    fun node(id: Long): String = "$BASE/id/$id/"

    /** „conf“ stránka nodu (šablóna 1961033) – formulár s pôvodným textom na úpravu (`configure_content`). */
    fun configure(id: Long): String = "${node(id)}1961033"

    /** ID nodu z odkazu na kyberiu (`…/id/123/`), inak null – odkaz sa vtedy otvorí v prehliadači. */
    fun nodeId(url: String): Long? = NODE_PATH.find(url)?.groupValues?.get(1)?.toLongOrNull()

    private val NODE_PATH = Regex("""^https://(?:[a-z0-9-]+\.)*kyberia\.sk/id/(\d+)(?:[/?#]|$)""", RegexOption.IGNORE_CASE)

    /** Avatar používateľa ako v HTML kyberie: /images/nodes/<1. číslica>/<2. číslica>/<ID>.gif. */
    fun avatar(userId: Long): String? =
        userId.toString().takeIf { it.length >= 2 }?.let { "$BASE/images/nodes/${it[0]}/${it[1]}/$it.gif" }
}
