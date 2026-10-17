package sk.ntnlabs.kyberia.data

enum class CheckResult { OK, ERROR, EXPIRED, QUIET_KEEPALIVE }

enum class CheckTrigger { SCHEDULED, KEEPALIVE, APP }

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
        lastOkMillis = if (entry.result == CheckResult.OK || entry.result == CheckResult.QUIET_KEEPALIVE) {
            entry.atMillis
        } else {
            lastOkMillis
        },
        lastExpired = if (entry.result == CheckResult.EXPIRED) entry else lastExpired,
    )
}

object KeepAlivePolicy {
    /** Keepalive budík tiká raz za hodinu. */
    const val INTERVAL_MILLIS = 60 * 60 * 1000L

    /** V tichom režime stačí session obnoviť najviac raz za ~hodinu. */
    private const val QUIET_GAP_MILLIS = 50 * 60 * 1000L

    fun quietKeepAliveDue(lastOkMillis: Long?, nowMillis: Long): Boolean =
        lastOkMillis == null || nowMillis - lastOkMillis >= QUIET_GAP_MILLIS
}
