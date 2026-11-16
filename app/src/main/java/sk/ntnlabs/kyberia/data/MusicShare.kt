package sk.ntnlabs.kyberia.data

import java.net.URI
import java.net.URLEncoder
import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.UrlPolicy

/**
 * Skladba, ktorú používateľ poslal mokapu cez Zdieľať. Z YouTube (aj YouTube Music) je to ID videa – do príspevku ide
 * ako embed (fórum ho prehrá); iný odkaz ostane, ako prišiel, a appka ponúkne „Nájsť na YouTube“. Nič sa nesťahuje.
 */
data class SharedMusic(
    val youTubeId: String?,
    /** Iný ako YouTube odkaz (Spotify, Shazam…); null pri YouTube alebo keď odkaz nebol. */
    val otherLink: String?,
    /** Názov z predmetu zdieľania (ak ho appka poslala) – do názvu príspevku, dá sa prepísať. */
    val title: String?,
    /** Text bez odkazov alebo názov – na hľadanie na YouTube. */
    val query: String?,
) {
    /** Text príspevku: embed z YouTube, inak odkaz alebo aspoň text. */
    val post: String get() = youTubeId?.let(MusicShare::embed) ?: otherLink ?: query.orEmpty()
}

object MusicShare {

    private val URL = Regex("""https?://\S+""")
    private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

    /** [text] = `EXTRA_TEXT` zdieľania, [subject] = `EXTRA_SUBJECT` (často názov), ak ho appka poslala. */
    fun parse(text: String, subject: String?): SharedMusic {
        val urls = URL.findAll(text).map { it.value.trimEnd('.', ',', ')', '"') }.toList()
        val youTube = urls.firstNotNullOfOrNull(::youTubeId)
        val title = subject?.let(::withoutPrefix)?.let(::clean)?.takeUnless { URL.containsMatchIn(it) }
        return SharedMusic(
            youTubeId = youTube,
            otherLink = if (youTube == null) urls.firstOrNull() else null,
            title = title,
            query = title ?: clean(URL.replace(text, " ")),
        )
    }

    /** ID videa z youtu.be, youtube.com (watch, shorts, live, embed), m.youtube.com aj music.youtube.com. */
    fun youTubeId(url: String): String? {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
        val host = uri.host?.lowercase()?.removePrefix("www.")?.removePrefix("m.") ?: return null
        val path = uri.path.orEmpty()
        val id = when (host) {
            "youtu.be" -> path.trim('/').substringBefore('/')
            "youtube.com", "music.youtube.com" -> when {
                path == "/watch" -> uri.rawQuery?.split('&')?.firstOrNull { it.startsWith("v=") }?.removePrefix("v=")
                path.startsWith("/shorts/") || path.startsWith("/live/") || path.startsWith("/embed/") ->
                    path.split('/').getOrNull(2)
                else -> null
            }
            else -> null
        }
        return id?.takeIf { VIDEO_ID.matches(it) }
    }

    /** Embed, ako ho dáva YouTube („Zdieľať → Vložiť“), len bez sledovacieho `si`. */
    fun embed(id: String): String =
        """<iframe width="560" height="315" src="https://www.youtube.com/embed/$id" title="YouTube video player" """ +
            """frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; """ +
            """picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>"""

    /** Vyhľadanie na YouTube – otvorí ho prehliadač či appka YouTube, mokap nič nesťahuje. */
    fun searchUrl(query: String): String =
        // encode(String, Charset) je až od API 33 – verzia s názvom kódovania ide všade (minSdk 29).
        "https://www.youtube.com/results?search_query=" + URLEncoder.encode(query, "UTF-8")

    private fun clean(text: String): String? = text.replace(Regex("""\s+"""), " ").trim().ifEmpty { null }

    /**
     * Fráza, ktorú appka pridá pred názov do predmetu – YouTube Music „Check out OMG“. Ďalšie (iný jazyk telefónu)
     * sa pridajú, keď sa ukážu.
     */
    private val SUBJECT_PREFIXES = listOf("Check out ")

    private fun withoutPrefix(subject: String): String {
        val text = subject.trimStart()
        val prefix = SUBJECT_PREFIXES.firstOrNull { text.startsWith(it, ignoreCase = true) } ?: return text
        return text.substring(prefix.length)
    }
}

/** Cieľ na kyberii z nastavení: číslo nodu alebo odkaz naň; prázdne = vypnuté. */
object KyberiaTarget {

    // Pred hostom nesmie byť písmeno ani bodka – „notkyberia.sk“ nie je kyberia.
    private val LINK = Regex("""(?<![\w.-])(?:https?://)?(?:[\w-]+\.)*kyberia\.sk/id/(\d+)""", RegexOption.IGNORE_CASE)

    /** Node z odkazu na kyberiu kdekoľvek v texte (vložený či zdieľaný z prehliadača); samé číslo odkaz nie je. */
    fun linkIn(text: String): Long? = LINK.find(text)?.groupValues?.get(1)?.toLongOrNull()?.takeIf { it > 0 }

    fun parse(input: String): Long? {
        val text = input.trim()
        if (text.isEmpty()) return null
        // Odkaz aj bez https:// („kyberia.sk/id/…“), ako ho ľudia skopírujú.
        val url = if ("://" in text) text else "https://$text"
        val id = text.toLongOrNull() ?: if (UrlPolicy.isInternal(url)) KyberiaUrls.nodeId(url) else null
        return id?.takeIf { it > 0 }
    }
}
