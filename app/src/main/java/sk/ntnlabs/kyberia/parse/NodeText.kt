package sk.ntnlabs.kyberia.parse

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/** Spoločné kúsky pre bloky, kde je text voľne medzi `<br>` (odpovede, last). */
internal object NodeText {

    private val NODE_ID = Regex("""/id/(\d+)""")
    private val WHITESPACE = Regex("""\s+""")
    private val NBSP = Char(0xA0)

    fun nodeId(href: String): Long? = NODE_ID.find(href)?.groupValues?.get(1)?.toLongOrNull()

    /** Priami potomkovia rozdelení na riadky podľa `<br>`; samotné `<br>` vypadnú. */
    fun lines(item: Element): List<List<Node>> {
        val lines = mutableListOf(mutableListOf<Node>())
        item.childNodes().forEach { node ->
            if (node is Element && node.normalName() == "br") lines.add(mutableListOf()) else lines.last().add(node)
        }
        return lines
    }

    /** Text uzlov v jednom riadku – zalomenia a nedeliteľné medzery ako obyčajná medzera. */
    fun join(nodes: List<Node>): String =
        nodes.joinToString(" ") { node ->
            when (node) {
                is TextNode -> node.text()
                is Element -> node.text()
                else -> ""
            }
        }
            .replace(NBSP, ' ')
            .replace(WHITESPACE, " ")
            .trim()
}
