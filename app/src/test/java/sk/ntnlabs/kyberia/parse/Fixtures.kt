package sk.ntnlabs.kyberia.parse

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/** Umelé HTML vzorky z src/test/resources/fixtures – nikdy nie skutočné stránky. */
object Fixtures {
    fun load(name: String, url: String): Document {
        val stream = requireNotNull(javaClass.getResourceAsStream("/fixtures/$name")) { "Chýba fixture $name" }
        return stream.use { Jsoup.parse(it, "UTF-8", url) }
    }
}
