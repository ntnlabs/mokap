package sk.ntnlabs.kyberia.parse

/**
 * Odpoveď `/ajax/check_new_mail.php` – to isté, čo web volá raz za minútu kvôli „[N]“ v titulku.
 * Formát: `<počet nových>;…;…`, alebo `offline`. Ďalšie polia nepoužívame, ich význam nie je overený.
 */
sealed interface MailCheck {
    data class Unread(val count: Int) : MailCheck

    /** Kyberia nevidí session. O odhlásení ale rozhoduje len stránka s prihlasovacím formulárom. */
    data object Offline : MailCheck
}

object MailCheckParser {

    /** null = nečakaná odpoveď (chybová stránka a pod.) – berie sa ako dočasná chyba. */
    fun parse(body: String): MailCheck? {
        val first = body.removePrefix("\uFEFF").trim().substringBefore(';').trim()
        if (first == "offline") return MailCheck.Offline
        return first.toIntOrNull()?.takeIf { it >= 0 }?.let(MailCheck::Unread)
    }
}
