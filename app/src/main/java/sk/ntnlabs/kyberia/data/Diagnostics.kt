package sk.ntnlabs.kyberia.data

enum class CheckResult { OK, ERROR, EXPIRED }

enum class CheckTrigger { SCHEDULED, APP }

data class CheckEntry(
    val atMillis: Long,
    val result: CheckResult,
    val trigger: CheckTrigger,
    val detail: String = "",
)

/** Čo sa dialo pri kontrolách na pozadí – aby sa dalo zistiť, prečo appku odhlásilo. */
data class Diagnostics(
    val last: CheckEntry? = null,
    val lastOkMillis: Long? = null,
    val lastExpired: CheckEntry? = null,
) {
    fun record(entry: CheckEntry): Diagnostics = copy(
        last = entry,
        lastOkMillis = if (entry.result == CheckResult.OK) entry.atMillis else lastOkMillis,
        lastExpired = if (entry.result == CheckResult.EXPIRED) entry else lastExpired,
    )
}
