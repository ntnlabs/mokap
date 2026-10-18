package sk.ntnlabs.kyberia.data

import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.parse.Friend
import sk.ntnlabs.kyberia.parse.PeopleParser
import sk.ntnlabs.kyberia.parse.Populated
import sk.ntnlabs.kyberia.parse.PopulatedParser

/** Ľudia online a kde sú. Sťahuje sa len na požiadanie v appke, nikdy v pozadí. */
class PeopleRepository(private val client: KyberiaClient) {

    suspend fun everyone(): List<Friend> = PeopleParser.parse(client.loggedInPage(KyberiaUrls.PEOPLE))

    suspend fun populated(): Populated = PopulatedParser.parse(client.loggedInPage(KyberiaUrls.POPULATED))
}
