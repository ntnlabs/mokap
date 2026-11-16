package sk.ntnlabs.kyberia.data

import sk.ntnlabs.kyberia.parse.ContentBlock
import sk.ntnlabs.kyberia.parse.NodeChild

/**
 * Odpoveď, na ktorú sa má node po otvorení z notifikácie posunúť. /id/19/ dáva len odkaz na tvoj príspevok (nie ID
 * odpovede), preto autor a začiatok textu – podľa nich sa odpoveď nájde medzi odpoveďami príspevku.
 */
data class ReplyFocus(val authorId: Long?, val snippet: String)

/** Otvorenie nodu zvonka (notifikácia), voliteľne s odpoveďou, na ktorú posunúť. */
data class NodeRequest(val nodeId: Long, val focus: ReplyFocus? = null)

object ReplyFocusFinder {

    /** Koľko znakov úryvku stačí na zhodu – úryvok z /id/19/ býva skrátený. */
    private const val PREFIX = 40

    /** ID odpovede od autora, ktorej text obsahuje začiatok úryvku; inak jeho prvá nová odpoveď; inak null. */
    fun find(children: List<NodeChild>, focus: ReplyFocus): Long? {
        val byAuthor = children.filter { focus.authorId == null || it.authorId == focus.authorId }
        val wanted = normalize(focus.snippet).trimEnd('.', '…', ' ').take(PREFIX)
        if (wanted.isNotEmpty()) byAuthor.firstOrNull { wanted in normalize(text(it)) }?.let { return it.id }
        return byAuthor.firstOrNull { it.isNew }?.id
    }

    private fun text(child: NodeChild): String =
        child.content.filterIsInstance<ContentBlock.Text>().joinToString(" ") { block -> block.spans.joinToString("") { it.text } }

    private fun normalize(text: String): String = text.replace(Regex("""\s+"""), " ").trim().lowercase()
}
