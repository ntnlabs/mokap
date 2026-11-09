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

    /** Chybu nehlásila, ale K nepribudlo – K sa dvakrát dať nedá, takže skúsiť znova je bezpečné. */
    data class Unconfirmed(override val page: NodePage) : KResult
}

/** Výsledok úpravy; [page] = čerstvá stránka nodu, v ktorom sa upravovalo. */
sealed interface EditResult {
    val page: NodePage?

    data class Saved(override val page: NodePage?) : EditResult

    data class Rejected(override val page: NodePage?, val message: String) : EditResult
}

object NodeOutcomes {

    /** K pribudlo, ak počet K pri node ([childId] null) alebo pri odpovedi narástol. */
    fun k(before: NodePage, childId: Long?, response: Document): KResult {
        val page = NodeParser.parse(response, before.id)
        PageError.of(response)?.let { return KResult.Rejected(page, it) }
        val was = kOf(before, childId) ?: 0
        val now = kOf(page, childId)
        return if (now != null && now > was) KResult.Given(page) else KResult.Unconfirmed(page)
    }

    private fun kOf(page: NodePage, childId: Long?): Int? =
        if (childId == null) page.k else page.children.firstOrNull { it.id == childId }?.k
}
