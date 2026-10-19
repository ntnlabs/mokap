package sk.ntnlabs.kyberia.data

enum class CheckResult { OK, ERROR, EXPIRED }

enum class CheckTrigger { SCHEDULED, APP }

data class CheckEntry(
    val atMillis: Long,
    val result: CheckResult,
    val trigger: CheckTrigger,
    val detail: String = "",
)

/** Čo sa dialo pri kontrolách – aby sa dalo zistiť, prečo appku odhlásilo. Bez súkromných údajov. */
data class Diagnostics(
    val last: CheckEntry? = null,
    val lastOkMillis: Long? = null,
    val lastExpired: CheckEntry? = null,
    /** Posledná úspešná kontrola pred odhlásením – ukáže, ako dlho appka na kyberii nebola. */
    val okBeforeExpiredMillis: Long? = null,
    /** Posledné kontroly (najstaršia prvá) – vidno z nich, či a kedy bežali v noci. */
    val history: List<CheckEntry> = emptyList(),
) {
    fun record(entry: CheckEntry): Diagnostics = copy(
        last = entry,
        lastOkMillis = if (entry.result == CheckResult.OK) entry.atMillis else lastOkMillis,
        lastExpired = if (entry.result == CheckResult.EXPIRED) entry else lastExpired,
        okBeforeExpiredMillis = if (entry.result == CheckResult.EXPIRED) lastOkMillis else okBeforeExpiredMillis,
        history = (history + entry).takeLast(HISTORY_SIZE),
    )

    companion object {
        const val HISTORY_SIZE = 40
    }
}

/** História do jedného textu v SharedPreferences: riadok = `čas<TAB>výsledok<TAB>spúšťač<TAB>detail`. */
object CheckHistoryCodec {

    private val SEPARATORS = Regex("""[\t\r\n]+""")

    fun encode(history: List<CheckEntry>): String = history.joinToString("\n") { entry ->
        listOf(entry.atMillis, entry.result.name, entry.trigger.name, entry.detail.replace(SEPARATORS, " ")).joinToString("\t")
    }

    fun decode(value: String): List<CheckEntry> = value.lineSequence().mapNotNull { line ->
        val parts = line.split('\t', limit = 4)
        val at = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
        val result = CheckResult.entries.firstOrNull { it.name == parts.getOrNull(1) } ?: return@mapNotNull null
        val trigger = CheckTrigger.entries.firstOrNull { it.name == parts.getOrNull(2) } ?: CheckTrigger.SCHEDULED
        CheckEntry(at, result, trigger, parts.getOrNull(3).orEmpty())
    }.toList()
}
