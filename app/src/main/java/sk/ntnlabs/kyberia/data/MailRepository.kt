package sk.ntnlabs.kyberia.data

import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.parse.MailCheck
import sk.ntnlabs.kyberia.parse.MailCheckParser

/** Počet novej pošty cez ľahký AJAX endpoint – pár bajtov namiesto celej stránky. */
class MailRepository(private val client: KyberiaClient) {

    /** null = nečakaná odpoveď; pri sieťovej chybe IOException. */
    suspend fun check(): MailCheck? = MailCheckParser.parse(client.getText(KyberiaUrls.MAIL_CHECK))
}
