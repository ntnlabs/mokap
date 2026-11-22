package sk.ntnlabs.kyberia.data

import sk.ntnlabs.kyberia.parse.Friend
import sk.ntnlabs.kyberia.parse.LoginType

/**
 * Vlastné ID pri prihlásení – raz za session, potom ho drží [OwnIdStore]. Prihlásenie cez ID ho dáva rovno; pri nicku
 * ho dá odkaz SELF (ak ho šablóna má), inak people (/id/27/): práve prihlásený je online a jeho riadok má odkaz na
 * profil. Nick sa nikam neukladá.
 */
object OwnId {

    /** ID napísané pri prihlásení cez ID; pri nicku null. */
    fun typed(login: String, type: LoginType): Long? = if (type == LoginType.ID) login.trim().toLongOrNull() else null

    /** Riadok s tým istým nickom (bez ohľadu na veľkosť písmen) – len keď je jediný. */
    fun fromPeople(people: List<Friend>, nick: String): Long? {
        val wanted = nick.trim().ifEmpty { return null }
        return people.filter { it.nick.trim().equals(wanted, ignoreCase = true) }.map { it.id }.distinct().singleOrNull()
    }
}
