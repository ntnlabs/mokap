package sk.ntnlabs.kyberia.parse

import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import sk.ntnlabs.kyberia.UrlPolicy

/** Kus textu s jednoduchým formátovaním; [link] je absolútna http(s) URL. */
data class TextSpan(val text: String, val bold: Boolean = false, val italic: Boolean = false, val link: String? = null)

/**
 * Obsah príspevku ako bloky. Appka HTML nevykresľuje (žiadny WebView, žiadne skripty) – len text,
 * odkazy a obrázky z kyberie; cudzie obrázky a vložené videá sú iba odkazy do prehliadača.
 */
sealed interface ContentBlock {
    data class Text(val spans: List<TextSpan>) : ContentBlock

    /** [internal] = obrázok z kyberie (zobrazí sa); cudzí sa nesťahuje – požiadavky idú len na kyberiu. */
    data class Image(val url: String, val internal: Boolean) : ContentBlock

    /** Vložené video a pod. (iframe). */
    data class Embed(val url: String) : ContentBlock
}

object ContentParser {

    private val BLOCK_TAGS = setOf(
        "p", "div", "center", "blockquote", "pre", "ul", "ol", "li", "table", "tr", "hr",
        "h1", "h2", "h3", "h4", "h5", "h6",
    )
    private val SKIP_TAGS = setOf("script", "style", "noscript", "form", "input", "button", "select", "textarea")

    /** [skip] vynechá prvky, ktoré nie sú obsahom (napr. „mood“ podpis pod príspevkom). */
    fun parse(root: Element, skip: (Element) -> Boolean = { false }): List<ContentBlock> =
        Builder(skip).also { it.walk(root, Style()) }.finish()

    /** http(s) URL, staré http:// na kyberiu ako https://; javascript:, data: a pod. vypadnú. */
    internal fun url(raw: String): String? {
        val url = raw.trim().takeIf { it.isNotEmpty() }?.let(UrlPolicy::normalize) ?: return null
        return url.takeIf { it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true) }
    }

    private data class Style(val bold: Boolean = false, val italic: Boolean = false, val link: String? = null)

    private class Builder(private val skip: (Element) -> Boolean) {
        private val blocks = mutableListOf<ContentBlock>()
        private val spans = mutableListOf<TextSpan>()

        fun walk(element: Element, style: Style) {
            element.childNodes().forEach { node ->
                when (node) {
                    is TextNode -> text(node.text(), style)
                    is Element -> element(node, style)
                }
            }
        }

        fun finish(): List<ContentBlock> {
            flush()
            return blocks
        }

        private fun element(el: Element, style: Style) {
            val tag = el.normalName()
            if (tag in SKIP_TAGS || skip(el)) return
            when (tag) {
                "br" -> lineBreak()
                "img" -> url(el.absUrl("src").ifEmpty { el.attr("src") })?.let { media(ContentBlock.Image(it, UrlPolicy.isInternal(it))) }
                "iframe" -> url(el.absUrl("src").ifEmpty { el.attr("src") })?.let { media(ContentBlock.Embed(it)) }
                else -> {
                    val block = tag in BLOCK_TAGS
                    if (block) blockBoundary()
                    if (tag == "li") append("• ", style)
                    walk(el, styleOf(el, tag, style))
                    if (block) blockBoundary()
                }
            }
        }

        private fun styleOf(el: Element, tag: String, style: Style): Style = when (tag) {
            "b", "strong" -> style.copy(bold = true)
            "i", "em" -> style.copy(italic = true)
            "a" -> style.copy(link = url(el.absUrl("href").ifEmpty { el.attr("href") }) ?: style.link)
            else -> style
        }

        private fun text(raw: String, style: Style) {
            var text = raw.replace(NBSP, ' ')
            // HTML zlučuje medzery – aj cez hranice prvkov a na začiatku riadku.
            if (atLineStart() || lastChar() == ' ') text = text.trimStart()
            if (text.isNotEmpty()) append(text, style)
        }

        private fun append(text: String, style: Style) {
            val last = spans.lastOrNull()
            if (last != null && last.bold == style.bold && last.italic == style.italic && last.link == style.link) {
                spans[spans.lastIndex] = last.copy(text = last.text + text)
            } else {
                spans += TextSpan(text, style.bold, style.italic, style.link)
            }
        }

        /** `<br>` – nový riadok, ale najviac jeden prázdny riadok za sebou. */
        private fun lineBreak() {
            if (spans.isEmpty()) return
            trimTrailingSpaces()
            if (!endsWith("\n\n")) append("\n", Style())
        }

        /** Hranica blokového prvku – nový riadok, ak už nie je. */
        private fun blockBoundary() {
            if (spans.isEmpty()) return
            trimTrailingSpaces()
            if (!atLineStart()) append("\n", Style())
        }

        private fun media(block: ContentBlock) {
            flush()
            blocks += block
        }

        private fun flush() {
            val text = spans.trimmed()
            spans.clear()
            if (text.isNotEmpty()) blocks += ContentBlock.Text(text)
        }

        private fun lastChar(): Char? = spans.lastOrNull()?.text?.lastOrNull()

        private fun atLineStart(): Boolean = spans.isEmpty() || lastChar() == '\n'

        private fun endsWith(suffix: String): Boolean = spans.joinToString("") { it.text }.endsWith(suffix)

        private fun trimTrailingSpaces() {
            val last = spans.lastOrNull() ?: return
            val trimmed = last.text.trimEnd(' ')
            if (trimmed.isEmpty()) spans.removeAt(spans.lastIndex) else spans[spans.lastIndex] = last.copy(text = trimmed)
        }

        /** Bez prázdnych riadkov a medzier na začiatku a konci bloku. */
        private fun List<TextSpan>.trimmed(): List<TextSpan> {
            val list = toMutableList()
            while (list.isNotEmpty()) {
                val first = list.first().text.trimStart()
                if (first.isEmpty()) list.removeAt(0) else { list[0] = list[0].copy(text = first); break }
            }
            while (list.isNotEmpty()) {
                val last = list.last().text.trimEnd()
                if (last.isEmpty()) list.removeAt(list.lastIndex) else { list[list.lastIndex] = list.last().copy(text = last); break }
            }
            return list
        }
    }

    private val NBSP = Char(0xA0)
}
