package sk.ntnlabs.kyberia.parse

/**
 * Odpoveď `/ajax/check_new_mail.php` – to isté, čo web volá raz za minútu kvôli „[N]“ v titulku.
 * Formát: `<počet nových>;<nick posledného odosielateľa>;…`, alebo `offline`.
 * Posledné pole nepoužívame, jeho význam nie je overený.
 */
sealed interface MailCheck {
    /** [lastFrom] je nick toho, kto písal naposledy – kyberia ho posiela aj pri nule nových správ. */
    data class Unread(val count: Int, val lastFrom: String? = null) : MailCheck

    /** Kyberia nevidí session. O odhlásení ale rozhoduje len stránka s prihlasovacím formulárom. */
    data object Offline : MailCheck
}

object MailCheckParser {

    /** null = nečakaná odpoveď (chybová stránka a pod.) – berie sa ako dočasná chyba. */
    fun parse(body: String): MailCheck? {
        val fields = body.removePrefix("\uFEFF").trim().split(';')
        val first = fields.first().trim()
        if (first == "offline") return MailCheck.Offline
        val count = first.toIntOrNull()?.takeIf { it >= 0 } ?: return null
        return MailCheck.Unread(count, sender(fields))
    }

    /** Keby nick obsahoval „;“, patrí k nemu všetko medzi prvým a posledným poľom. */
    private fun sender(fields: List<String>): String? {
        val raw = if (fields.size >= 3) fields.subList(1, fields.size - 1).joinToString(";") else fields.getOrNull(1)
        return raw?.filterNot { it.isISOControl() }?.trim()?.take(MAX_NICK)?.takeIf { it.isNotEmpty() }
    }

    private const val MAX_NICK = 64
}
