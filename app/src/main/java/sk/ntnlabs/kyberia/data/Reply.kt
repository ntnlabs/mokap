package sk.ntnlabs.kyberia.data

import org.jsoup.nodes.Document
import sk.ntnlabs.kyberia.parse.NodePage
import sk.ntnlabs.kyberia.parse.NodeParser
import sk.ntnlabs.kyberia.parse.ReplyFormParser

/** Čo sa stalo s odoslanou odpoveďou; vždy s čerstvou stránkou nodu (a novým formulárom), ktorú kyberia vrátila. */
sealed interface ReplyResult {
    val page: NodePage

    /** Nová odpoveď je na stránke. */
    data class Added(override val page: NodePage, val childId: Long) : ReplyResult

    /** Kyberia príspevok odmietla a povedala prečo (napr. „Chyba HTML syntaxe!“). */
    data class Rejected(override val page: NodePage, val message: String) : ReplyResult

    /**
     * Chybu nehlásila, ale novú odpoveď na prvej stránke nevidno – mohla prejsť hlbšie vo vlákne, alebo nie.
     * Text sa nezahadzuje a appka neodosiela znova sama.
     */
    data class Unconfirmed(override val page: NodePage) : ReplyResult
}

object Replies {

    /**
     * Porovná odpovede pred a po odoslaní. Za novú berie odpoveď, ktorá predtým nebola – od [me], ak ho poznáme
     * (súbežná odpoveď niekoho iného tak nepotvrdí tú našu).
     */
    fun outcome(before: NodePage, response: Document, me: Long?): ReplyResult {
        val page = NodeParser.parse(response, before.id)
        ReplyFormParser.error(response)?.let { return ReplyResult.Rejected(page, it) }
        val known = before.children.mapTo(HashSet()) { it.id }
        val added = page.children.firstOrNull { it.id !in known && (me == null || it.authorId == me) }
        return if (added != null) ReplyResult.Added(page, added.id) else ReplyResult.Unconfirmed(page)
    }
}
