package sk.ntnlabs.kyberia.data

import org.jsoup.nodes.Document
import sk.ntnlabs.kyberia.parse.NodePage
import sk.ntnlabs.kyberia.parse.NodeParser
import sk.ntnlabs.kyberia.parse.PageError

/** Výsledok K; vždy s čerstvou stránkou nodu, ktorú kyberia vrátila. */
sealed interface KResult {
    val page: NodePage

    data class Given(override val page: NodePage) : KResult

    /** Napr. „datanode uz si k udelil“ alebo „nemas k“. */
    data class Rejected(override val page: NodePage, val message: String) : KResult

    /** Chybu nehlásila, ale počet K sa nezmenil. */
    data class Unconfirmed(override val page: NodePage) : KResult

    /**
     * Počet K klesol – živá kyberia K podľa používateľa prepína: druhé stlačenie K odoberie (a to je penalizované).
     * Appka to nikdy nerobí zámerne; toto je hlásenie, že sa to stalo.
     */
    data class Removed(override val page: NodePage) : KResult

    /** Počet sa nezmenil, ale kyberia ponúka „-❤️“ (`revoKe`) – K od teba tam už bolo (Top K to neukazuje). */
    data class Already(override val page: NodePage) : KResult
}

/** Výsledok úpravy; [page] = čerstvá stránka nodu, v ktorom sa upravovalo. */
sealed interface EditResult {
    val page: NodePage?

    data class Saved(override val page: NodePage?) : EditResult

    data class Rejected(override val page: NodePage?, val message: String) : EditResult
}

object NodeOutcomes {

    /**
     * K pribudlo, ak počet K narástol – pri samotnom node ([childId] null) aj ak kyberia namiesto K ponúka „-❤️“
     * (`revoKe`), teda K tam už je od teba.
     */
    fun k(before: NodePage, childId: Long?, response: Document): KResult {
        val page = NodeParser.parse(response, before.id)
        PageError.of(response)?.let { return KResult.Rejected(page, it) }
        if (childId == null && page.kGiven && !before.kGiven) return KResult.Given(page)
        val was = kOf(before, childId) ?: 0
        val now = kOf(page, childId)
        return when {
            now != null && now > was -> KResult.Given(page)
            (now ?: 0) < was -> KResult.Removed(page)
            else -> KResult.Unconfirmed(page)
        }
    }

    /**
     * K z Top K: POST na samotný node, odpoveď je jeho stránka. [beforeK] = počet K zo zoznamu (môže byť zastaraný).
     * Narástol = dané; klesol = odobraté (hlásiť); rovnaký s „-❤️“ = K od teba tam už bolo.
     */
    fun kFromList(nodeId: Long, beforeK: Int?, response: Document): KResult {
        val page = NodeParser.parse(response, nodeId)
        PageError.of(response)?.let { return KResult.Rejected(page, it) }
        val was = beforeK ?: 0
        val now = page.k ?: 0
        return when {
            now > was -> KResult.Given(page)
            now < was -> KResult.Removed(page)
            page.kGiven -> KResult.Already(page)
            else -> KResult.Unconfirmed(page)
        }
    }

    private fun kOf(page: NodePage, childId: Long?): Int? =
        if (childId == null) page.k else page.children.firstOrNull { it.id == childId }?.k
}
