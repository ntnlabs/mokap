package sk.ntnlabs.kyberia.data

import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.parse.TopKItem
import sk.ntnlabs.kyberia.parse.TopKParser

/** Top K (`/id/8340566/`) – sťahuje sa len na požiadanie v appke, nikdy v pozadí. */
class TopKRepository(private val client: KyberiaClient, private val presence: Presence? = null) {

    /** Po stránke ešte „používa MOKAP“ ([Presence]), aby „kde je“ neostalo na top K. */
    suspend fun load(): List<TopKItem> = TopKParser.parse(client.loggedInPage(KyberiaUrls.TOP_K).also { presence?.mark() })
}
