package sk.ntnlabs.kyberia.data

import sk.ntnlabs.kyberia.parse.NodeChild

/** Položka vlákna na obrazovke nodu. */
sealed interface ThreadItem {
    data class Reply(val child: NodeChild) : ThreadItem

    /** [count] hlbších odpovedí pod odpoveďou [parentId] – ťuknutie otvorí jej node; [depth] na odsadenie. */
    data class More(val parentId: Long, val count: Int, val depth: Int) : ThreadItem
}

/**
 * Vlákno do [MAX_DEPTH] – na mobile je hlbšie odsadzovanie neprehľadné. Kyberia posiela odpovede
 * v poradí vlákna (rodič, potom jeho potomkovia), takže hlbšie odpovede idú hneď za svojím predkom.
 */
object ThreadView {

    /** 0 = priama odpoveď; 2 = tri úrovne. */
    const val MAX_DEPTH = 2

    fun items(children: List<NodeChild>, maxDepth: Int = MAX_DEPTH): List<ThreadItem> {
        val items = mutableListOf<ThreadItem>()
        // Posledná zobrazená odpoveď na najhlbšej úrovni a jej skutočná hĺbka; hlbšie pod ňou sa schovajú.
        var anchor: NodeChild? = null
        var anchorDepth = 0
        var hidden = 0
        fun flush() {
            val parent = anchor
            if (parent != null && hidden > 0) items += ThreadItem.More(parent.id, hidden, maxDepth + 1)
            hidden = 0
        }
        for (child in children) {
            if (anchor != null && child.depth > anchorDepth) {
                hidden++
                continue
            }
            flush()
            if (child.depth >= maxDepth) {
                // Aj hlboká odpoveď bez predka na stránke (kyberia stránkuje) sa ukáže – na najhlbšej úrovni.
                items += ThreadItem.Reply(child.copy(depth = maxDepth))
                anchor = child
                anchorDepth = child.depth
            } else {
                items += ThreadItem.Reply(child)
                anchor = null
            }
        }
        flush()
        return items
    }
}
