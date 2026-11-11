package sk.ntnlabs.kyberia.parse

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import sk.ntnlabs.kyberia.UrlPolicy

/** Správa v pošte (`/id/24/`). Appka ju drží len v pamäti, nikam neukladá. */
data class Mail(
    val id: Long,
    val fromId: Long?,
    val from: String,
    val toId: Long?,
    val to: String,
    /** „20:04:25 - 11.11.2026“ */
    val time: String?,
    /** „NEW“ – prichádzajúca, ktorú si ešte nevidel (kyberia ju otvorením pošty označí ako prečítanú). */
    val isNew: Boolean,
    /** „UNREAD“ – tvoja odoslaná, ktorú adresát ešte nečítal. */
    val unreadByRecipient: Boolean,
    val avatarUrl: String?,
    val content: List<ContentBlock>,
)

/** Kam poslať správu (`event=send`). Token len v pamäti, nie vo výpise. */
class MailForm(val action: String, val anticsrf: String, val multipart: Boolean) {
    override fun toString(): String = "MailForm(action=$action)"
}

/** Prvá stránka pošty (kyberia ukáže 32 správ, najnovšie hore) a vlastné ID na rozlíšenie odoslaných. */
class MailPage(val mails: List<Mail>, val form: MailForm?, val selfId: Long?) {
    /** Druhá strana konverzácie – pri prichádzajúcej odosielateľ, pri odoslanej adresát. */
    fun counterpart(mail: Mail): Pair<Long?, String> =
        if (selfId != null && mail.fromId == selfId) mail.toId to mail.to else mail.fromId to mail.from

    fun outgoing(mail: Mail): Boolean = selfId != null && mail.fromId == selfId

    override fun toString(): String = "MailPage(${mails.size} mails)"
}

/**
 * Pošta: každá správa je `div.message` – `img.mail_avatar` (odosielateľ), `div.header` s
 * `<a href="javascript:chngto('nick','ID')">` odosielateľa, „-->“, adresáta, voliteľne `[location:: …]`,
 * checkbox `message[ID]`, čas a `span.most_important` „NEW“ / „UNREAD“; text v `div.content`.
 * Formulár `formular` je urlencoded: `mail_to`, rádio `mail_to_type` (`name`/`id`), `mail_text`, `no_html`, `event=send`.
 */
object MailParser {

    private val CHNGTO = Regex("""chngto\(\s*'(.*?)'\s*,\s*'?(\d+)'?\s*\)""")
    private val MESSAGE_ID = Regex("""message\[(\d+)]""")
    private val TIME = Regex("""\d{1,2}:\d{2}:\d{2}\s*-\s*\d{1,2}\.\d{1,2}\.\d{4}""")
    private val SPACES = Regex("""\s+""")

    fun parse(document: Document): MailPage = MailPage(
        mails = document.select("div.message").mapNotNull(::mail),
        form = form(document),
        selfId = UserInfoParser.parse(document).userId,
    )

    /**
     * Len polia na odoslanie – nie celý formulár (má aj `delete_mail`, stránkovanie, hľadanie). [byId] = adresát je
     * číslo profilu (odpoveď), inak nick. Viac adresátov oddeľuje kyberia „;“ – appka posiela jednému.
     */
    fun sendFields(form: MailForm, to: String, byId: Boolean, text: String, noHtml: Boolean): List<Pair<String, String>> =
        buildList {
            add("anticsrf" to form.anticsrf)
            add("mail_to" to to.trim())
            add("mail_to_type" to if (byId) "id" else "name")
            add("mail_text" to text)
            if (noHtml) add("no_html" to "yes")
            add("event" to "send")
        }

    private fun form(document: Document): MailForm? {
        val form = Forms.find(document) { form ->
            form.elements().any { it.attr("name") == "mail_text" } && Forms.submitButton(form, "send") != null
        } ?: return null
        val token = form.elements().firstOrNull { it.attr("name") == "anticsrf" }?.attr("value")?.ifEmpty { null }
            ?: Forms.anticsrf(document)
            ?: return null
        return MailForm(Forms.action(form, document), token, Forms.isMultipart(form))
    }

    private fun mail(message: Element): Mail? {
        val header = message.selectFirst("div.header") ?: return null
        val id = header.select("input[name]").firstNotNullOfOrNull { MESSAGE_ID.find(it.attr("name"))?.groupValues?.get(1)?.toLongOrNull() }
            ?: return null
        val people = header.select("a[href]").mapNotNull { a ->
            CHNGTO.find(a.attr("href"))?.let { match ->
                (match.groupValues[2].toLongOrNull()) to a.text().trim().ifEmpty { match.groupValues[1] }
            }
        }
        val from = people.getOrNull(0) ?: return null
        val to = people.getOrNull(1)
        val marks = header.select("span.most_important").map { it.text().trim().uppercase() }
        return Mail(
            id = id,
            fromId = from.first,
            from = from.second,
            toId = to?.first,
            to = to?.second.orEmpty(),
            time = TIME.find(header.text())?.value?.replace(SPACES, " "),
            isNew = "NEW" in marks,
            unreadByRecipient = "UNREAD" in marks,
            avatarUrl = message.selectFirst("img.mail_avatar")?.absUrl("src")?.takeIf(UrlPolicy::isInternal),
            content = message.selectFirst("div.content")?.let { ContentParser.parse(it) }.orEmpty(),
        )
    }
}
