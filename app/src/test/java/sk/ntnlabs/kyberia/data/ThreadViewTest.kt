package sk.ntnlabs.kyberia.data

import org.junit.Assert.assertEquals
import org.junit.Test
import sk.ntnlabs.kyberia.parse.NodeChild

class ThreadViewTest {

    private fun child(id: Long, depth: Int) =
        NodeChild(id, depth, null, "autor", null, "", null, null, false, emptyList())

    private fun List<ThreadItem>.shape() = map {
        when (it) {
            is ThreadItem.Reply -> "R${it.child.id}@${it.child.depth}"
            is ThreadItem.More -> "M${it.parentId}x${it.count}@${it.depth}"
        }
    }

    @Test
    fun deeperRepliesFoldUnderTheirAncestor() {
        val children = listOf(child(1, 0), child(2, 1), child(3, 2), child(4, 3), child(5, 4), child(6, 2), child(7, 3), child(8, 0))
        assertEquals(listOf("R1@0", "R2@1", "R3@2", "M3x2@3", "R6@2", "M6x1@3", "R8@0"), ThreadView.items(children).shape())
    }

    @Test
    fun shallowThreadUnchanged() {
        val children = listOf(child(1, 0), child(2, 1), child(3, 0))
        assertEquals(listOf("R1@0", "R2@1", "R3@0"), ThreadView.items(children).shape())
    }

    @Test
    fun orphanDeepRepliesAtPageStartAreShown() {
        // Stránka začína uprostred hlbokého vlákna: súrodenci na hĺbke 5 sa ukážu, ich potomok sa schová.
        val children = listOf(child(1, 5), child(2, 6), child(3, 5), child(4, 0))
        assertEquals(listOf("R1@2", "M1x1@3", "R3@2", "R4@0"), ThreadView.items(children).shape())
    }
}
