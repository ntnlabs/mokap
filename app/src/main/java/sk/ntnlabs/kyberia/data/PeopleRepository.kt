package sk.ntnlabs.kyberia.data

import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.parse.Friend
import sk.ntnlabs.kyberia.parse.PeopleParser
import sk.ntnlabs.kyberia.parse.Populated
import sk.ntnlabs.kyberia.parse.PopulatedParser

/** Ľudia online a kde sú. Sťahuje sa len na požiadanie v appke, nikdy v pozadí. */
class PeopleRepository(private val client: KyberiaClient, private val presence: Presence? = null) {

    suspend fun everyone(): List<Friend> = PeopleParser.parse(page(KyberiaUrls.PEOPLE))

    suspend fun populated(): Populated = PopulatedParser.parse(page(KyberiaUrls.POPULATED))

    /** Po stránke ešte „používa MOKAP“ ([Presence]), aby „kde je“ neostalo na people či populated. */
    private suspend fun page(url: String) = client.loggedInPage(url).also { presence?.mark() }
}
