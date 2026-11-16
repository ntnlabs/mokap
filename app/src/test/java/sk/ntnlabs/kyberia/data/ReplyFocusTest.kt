package sk.ntnlabs.kyberia.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import sk.ntnlabs.kyberia.parse.ContentBlock
import sk.ntnlabs.kyberia.parse.NodeChild
import sk.ntnlabs.kyberia.parse.TextSpan

/** Vymyslení autori a texty. */
class ReplyFocusTest {

    private fun child(id: Long, authorId: Long, text: String, isNew: Boolean = false) = NodeChild(
        id = id, depth = 0, authorId = authorId, author = "u$authorId", avatarUrl = null, title = "Re: x",
        created = null, k = null, isNew = isNew, content = listOf(ContentBlock.Text(listOf(TextSpan(text)))),
    )

    private val children = listOf(
        child(1, 10, "Prvá odpoveď od niekoho iného"),
        child(2, 20, "Ahoj,   toto je moja dlhá   odpoveď na tvoj príspevok", isNew = true),
        child(3, 20, "Ešte jedna vec navyše", isNew = true),
    )

    @Test
    fun findsReplyByAuthorAndSnippet() {
        assertEquals(3L, ReplyFocusFinder.find(children, ReplyFocus(20, "Ešte jedna vec")))
        assertEquals(2L, ReplyFocusFinder.find(children, ReplyFocus(20, "ahoj, toto je moja dlhá odpoveď...")))
    }

    @Test
    fun otherAuthorWithSameTextDoesNotMatch() {
        assertEquals(2L, ReplyFocusFinder.find(children, ReplyFocus(20, "Prvá odpoveď")))
    }

    @Test
    fun withoutMatchFallsBackToFirstNewOrNothing() {
        assertEquals(2L, ReplyFocusFinder.find(children, ReplyFocus(20, "text, ktorý tam nie je")))
        assertNull(ReplyFocusFinder.find(children, ReplyFocus(99, "čokoľvek")))
    }
}
