package sk.ntnlabs.kyberia.parse

import java.nio.charset.Charset
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.FormElement

/**
 * Generické čítanie formulárov. Kyberia posiela akcie ako `event=<názov>` spolu
 * so skrytým `anticsrf` tokenom, takže stačí odoslať to, čo by odoslal prehliadač.
 */
object Forms {

    /** Polia, ktoré by prehliadač odoslal – bez tlačidiel, súborov a hesiel. */
    fun fields(form: FormElement): List<Pair<String, String>> = form.elements().mapNotNull { el ->
        val name = el.attr("name")
        if (name.isEmpty() || el.hasAttr("disabled")) return@mapNotNull null
        when (el.normalName()) {
            "input" -> when (el.attr("type").lowercase()) {
                "password", "file", "submit", "button", "image", "reset" -> null
                "checkbox", "radio" ->
                    if (el.hasAttr("checked")) name to el.attr("value").ifEmpty { "on" } else null
                else -> name to el.attr("value")
            }
            "textarea" -> name to el.`val`()
            "select" -> {
                val option = el.selectFirst("option[selected]") ?: el.selectFirst("option")
                option?.let { name to (if (it.hasAttr("value")) it.attr("value") else it.text()) }
            }
            else -> null
        }
    }

    /** Pomenované submit tlačidlo; s [event] len to, ktoré má danú hodnotu. */
    fun submitButton(form: FormElement, event: String? = null): Pair<String, String>? =
        form.elements()
            .firstOrNull { it.isSubmit() && it.attr("name").isNotEmpty() && (event == null || it.attr("value") == event) }
            ?.let { it.attr("name") to it.attr("value") }

    fun find(document: Document, predicate: (FormElement) -> Boolean): FormElement? =
        document.select("form").filterIsInstance<FormElement>().firstOrNull(predicate)

    fun action(form: FormElement, document: Document): String =
        form.absUrl("action").ifEmpty { document.location() }

    private fun Element.isSubmit(): Boolean = when (normalName()) {
        "button" -> attr("type").lowercase().let { it.isEmpty() || it == "submit" }
        "input" -> attr("type").equals("submit", ignoreCase = true)
        else -> false
    }
}

object Session {
    /** Prihlásená stránka má vždy tlačidlo `event=logout`. */
    fun isLoggedIn(document: Document): Boolean =
        document.selectFirst("[name=event][value=logout]") != null
}

class LoginForm(
    val action: String,
    val fields: List<Pair<String, String>>,
    val usernameField: String,
    val passwordField: String,
    val charset: Charset,
) {
    fun fill(username: String, password: String): List<Pair<String, String>> =
        fields.filterNot { it.first == usernameField } +
            (usernameField to username) +
            (passwordField to password)
}

object LoginFormParser {

    private val USERNAME_TYPES = setOf("", "text", "email")

    /** Nájde POST formulár s heslom. Názvy polí nepredpokladá, číta ich zo stránky. */
    fun find(document: Document): LoginForm? {
        val form = Forms.find(document) { form -> form.elements().any { it.isPassword() } } ?: return null
        // Heslo cez GET by skončilo v URL a logoch – také niečo neodošleme.
        if (!form.attr("method").equals("post", ignoreCase = true)) return null
        val controls = form.elements()
        val passwordField = controls.firstOrNull { it.isPassword() }?.attr("name")
            ?.takeIf { it.isNotEmpty() } ?: return null
        val usernameField = controls.firstOrNull {
            it.normalName() == "input" &&
                it.attr("type").lowercase() in USERNAME_TYPES &&
                it.attr("name").isNotEmpty()
        }?.attr("name") ?: return null
        val fields = Forms.fields(form) + listOfNotNull(Forms.submitButton(form))
        return LoginForm(Forms.action(form, document), fields, usernameField, passwordField, document.charset())
    }

    private fun Element.isPassword() =
        normalName() == "input" && attr("type").equals("password", ignoreCase = true)
}
