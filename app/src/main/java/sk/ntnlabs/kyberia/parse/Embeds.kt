package sk.ntnlabs.kyberia.parse

/** Vložené videá z príspevkov: kam ich otvoriť a ich náhľad (len pri zapnutých cudzích obrázkoch). */
object Embeds {

    private val YOUTUBE = Regex(
        """^https?://(?:www\.|m\.)?(?:youtube(?:-nocookie)?\.com/(?:embed/|watch\?v=|shorts/)|youtu\.be/)([A-Za-z0-9_-]{6,})""",
        RegexOption.IGNORE_CASE,
    )

    fun youtubeId(url: String): String? = YOUTUBE.find(url)?.groupValues?.get(1)

    /** YouTube ako bežná stránka videa (telefón ju otvorí v appke YouTube), nie „embed“; ostatné bez zmeny. */
    fun openUrl(url: String): String = youtubeId(url)?.let { "https://www.youtube.com/watch?v=$it" } ?: url

    fun thumbnail(url: String): String? = youtubeId(url)?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
}
