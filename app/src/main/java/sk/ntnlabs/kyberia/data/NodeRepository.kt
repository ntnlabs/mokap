package sk.ntnlabs.kyberia.data

import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.parse.NodePage
import sk.ntnlabs.kyberia.parse.NodeParser

/**
 * Node na čítanie v appke – jedna požiadavka ako na webe, nič vopred. Bez „používa MOKAP“ po ňom:
 * otvorený node je skutočné „kde je“ (ručné kliknutie).
 */
class NodeRepository(private val client: KyberiaClient) {

    /** O odhlásení rozhoduje [loggedInPage]; pri sieťovej chybe IOException. */
    suspend fun load(id: Long): NodePage = NodeParser.parse(client.loggedInPage(KyberiaUrls.node(id)), id)
}
