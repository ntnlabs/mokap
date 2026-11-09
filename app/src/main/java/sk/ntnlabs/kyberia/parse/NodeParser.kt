package sk.ntnlabs.kyberia.parse

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import sk.ntnlabs.kyberia.UrlPolicy

/** Odpoveď vo vlákne pod nodom; [depth] 0 = priama odpoveď. */
data class NodeChild(
    val id: Long,
    val depth: Int,
    val authorId: Long?,
    val author: String,
    val avatarUrl: String?,
    val title: String,
    /** Čas tak, ako ho ukazuje kyberia („06.11.2026 - 13:06:35“). */
    val created: String?,
    val k: Int?,
    /** Kyberia ju označila „New“ – pribudla od tvojej poslednej návštevy. */
    val isNew: Boolean,
    val content: List<ContentBlock>,
)

/** Jeden node (príspevok, fórum, log…) s prvou stránkou odpovedí. */
data class NodePage(
    val id: Long,
    val title: String,
    /** Šablóna nodu (`tr#sidebar_template`): 3 fórum, 4 príspevok (submission), 2 nodeshell, 5 článok… */
    val templateId: Int? = null,
    val ownerId: Long?,
    val owner: String?,
    val created: String?,
    val parentId: Long?,
    val parentTitle: String?,
    val k: Int?,
    val content: List<ContentBlock>,
    val children: List<NodeChild>,
    /** Počet priamych odpovedí na kyberii – na stránke ich býva menej (kyberia ich stránkuje). */
    val childrenTotal: Int?,
    /** Formulár na odpoveď; null = bez práva zápisu (alebo ho kyberia neukázala). */
    val replyForm: ReplyForm? = null,
    /** Kam poslať K pre tento node; null = kyberia ho tu neponúka (alebo si ho už dal – [kGiven]). */
    val kNode: PostTarget? = null,
    /** K tomuto nodu si už dal (kyberia namiesto K ponúka „-❤️“ = `revoKe`). Pri odpovediach to stránka neukazuje. */
    val kGiven: Boolean = false,
    /** Kam poslať K pre odpoveď (`formular` + `node_chosen[]`). */
    val kChild: PostTarget? = null,
)

/**
 * Stránka nodu: názov v `<title>` („názov :: motto“), údaje v ľavom paneli (`tr#sidebar_*`),
 * K a počty v `span.node_info2`, obsah v `div#topic` a odpovede ako vnorené `li.lvl.levelN`.
 */
object NodeParser {

    private val LEVEL = Regex("""\blevel(\d+)\b""")
    private val K = Regex("""(\d+)\s*K""", RegexOption.IGNORE_CASE)
    private val LEADING_NUMBER = Regex("""^\s*(\d+)""")
    private val TOTAL_CHILDREN = Regex("""total children::\s*(\d+)""")

    fun parse(document: Document, nodeId: Long): NodePage {
        val owner = document.select("tr#sidebar_owner")
            .firstOrNull { it.selectFirst("td")?.text()?.trim()?.startsWith("owner") == true }
            ?.select("td")?.getOrNull(1)?.selectFirst("a[href]")
        val parent = document.selectFirst("tr#sidebar_parent")?.select("td")?.getOrNull(1)?.selectFirst("a[href]")
        val info = document.selectFirst("span.node_info2")
        return NodePage(
            id = nodeId,
            title = document.title().substringBefore(" :: ").trim(),
            templateId = cell(document, "tr#sidebar_template")?.let { LEADING_NUMBER.find(it) }?.groupValues?.get(1)?.toIntOrNull(),
            ownerId = owner?.attr("href")?.let(NodeText::nodeId),
            owner = owner?.text()?.trim()?.ifEmpty { null },
            created = cell(document, "tr#sidebar_created"),
            parentId = parent?.attr("href")?.let(NodeText::nodeId),
            parentTitle = parent?.text()?.trim()?.ifEmpty { null },
            k = info?.selectFirst("span.most_important")?.text()?.let { LEADING_NUMBER.find(it) }?.groupValues?.get(1)?.toIntOrNull(),
            content = document.selectFirst("div#topic")?.let { ContentParser.parse(it) }.orEmpty(),
            children = document.select("li.lvl").mapNotNull(::child),
            childrenTotal = info?.text()?.let { TOTAL_CHILDREN.find(it) }?.groupValues?.get(1)?.toIntOrNull(),
            replyForm = ReplyFormParser.parse(document),
            kNode = NodeActionsParser.kNode(document),
            kGiven = NodeActionsParser.kGiven(document),
            kChild = NodeActionsParser.kChild(document),
        )
    }

    private fun cell(document: Document, row: String): String? =
        document.selectFirst(row)?.select("td")?.getOrNull(1)?.text()?.trim()?.ifEmpty { null }

    private fun child(item: Element): NodeChild? {
        val box = item.children().firstOrNull { it.hasClass("node_content") } ?: return null
        val header = box.children().firstOrNull { it.hasClass("node_header") } ?: return null
        val body = box.children().firstOrNull { it.hasClass("node_body") }
        val id = item.id().removePrefix("sub").toLongOrNull()
            ?: header.selectFirst("input.node_chosen")?.attr("value")?.toLongOrNull()
            ?: return null
        val login = header.selectFirst("a.node_login")
        return NodeChild(
            id = id,
            depth = (LEVEL.find(item.className())?.groupValues?.get(1)?.toIntOrNull() ?: 1).minus(1).coerceAtLeast(0),
            authorId = login?.attr("href")?.let(NodeText::nodeId),
            author = login?.text()?.trim().orEmpty(),
            avatarUrl = header.selectFirst("img.node_avatar")?.absUrl("src")?.takeIf(UrlPolicy::isInternal),
            title = header.selectFirst("a.node_header_title_nodename")?.text()?.trim().orEmpty(),
            created = header.selectFirst("span.node_header_created")?.text()?.trim()?.ifEmpty { null },
            k = header.selectFirst("span.node_header_k")?.text()?.let { K.find(it) }?.groupValues?.get(1)?.toIntOrNull(),
            isNew = header.selectFirst("span.node_header_new") != null,
            // „mood“ je podpis pod príspevkom, nie jeho obsah.
            content = body?.let { ContentParser.parse(it) { el -> el.normalName() == "small" && el.hasClass("mood") } }.orEmpty(),
        )
    }
}
