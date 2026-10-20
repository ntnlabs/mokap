package sk.ntnlabs.kyberia.data

import java.net.URI

/** Čo presne kyberia ukázala, keď appku odhlásila – do diagnostiky, bez osobných údajov. */
object LogoutDetail {

    const val LOGIN_SHOWN = "na stránke bol login"

    /**
     * Pri nečinnosti > 2 h kyberia session zruší a presmeruje na „/“. Keď session zmizla inak
     * (na serveri, vypršaná cookie), požadovaná stránka ukáže login bez presmerovania.
     */
    fun of(requestedUrl: String, finalUrl: String): String {
        val requested = path(requestedUrl)
        val final = path(finalUrl)
        return if (final != null && final != requested) {
            "$LOGIN_SHOWN, presmerovanie na ${final.ifEmpty { "/" }}"
        } else {
            "$LOGIN_SHOWN, bez presmerovania"
        }
    }

    private fun path(url: String): String? = runCatching { URI(url).path.orEmpty().trimEnd('/') }.getOrNull()
}
