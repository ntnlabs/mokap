package sk.ntnlabs.kyberia.data

enum class NetState { OK, NO_INTERNET, BLOCKED, NONE }

/**
 * Stav siete a obmedzení Androidu v čase chyby kontroly na pozadí – do diagnostiky, aby bolo
 * vidieť, či bol telefón offline, alebo appke sieť zakázal Android (Doze, šetriče, standby bucket).
 */
data class NetworkConditions(
    val network: NetState,
    val deviceIdle: Boolean = false,
    val powerSave: Boolean = false,
    val dataSaver: Boolean = false,
    val backgroundRestricted: Boolean = false,
    val standbyBucket: Int? = null,
) {
    fun describe(): String = buildList {
        add(
            when (network) {
                NetState.OK -> "sieť OK"
                NetState.NO_INTERNET -> "sieť bez internetu"
                NetState.BLOCKED -> "sieť zablokovaná pre appku"
                NetState.NONE -> "bez siete"
            },
        )
        if (deviceIdle) add("Doze")
        if (powerSave) add("šetrič batérie")
        if (dataSaver) add("šetrič dát")
        if (backgroundRestricted) add("obmedzená na pozadí")
        standbyBucket?.let { add("standby ${bucketName(it)}") }
    }.joinToString(", ")

    companion object {
        /** Hodnoty `UsageStatsManager.STANDBY_BUCKET_*`. */
        fun bucketName(bucket: Int): String = when (bucket) {
            5 -> "exempted"
            10 -> "active"
            20 -> "working set"
            30 -> "frequent"
            40 -> "rare"
            45 -> "restricted"
            50 -> "never"
            else -> bucket.toString()
        }
    }
}
