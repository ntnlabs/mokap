package sk.ntnlabs.kyberia.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lokálny záznam kontrol (bez súkromných údajov), zobrazený v nastaveniach. */
class DiagnosticsStore(context: Context) {

    private val prefs = context.getSharedPreferences("diagnostics", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    val diagnostics: StateFlow<Diagnostics> = state.asStateFlow()

    val current: Diagnostics get() = state.value

    @Synchronized
    fun record(entry: CheckEntry) {
        val updated = state.value.record(entry)
        prefs.edit {
            writeEntry(LAST, updated.last)
            writeEntry(EXPIRED, updated.lastExpired)
            updated.lastOkMillis?.let { putLong(LAST_OK, it) }
        }
        state.value = updated
    }

    private fun read(): Diagnostics = Diagnostics(
        last = readEntry(LAST),
        lastOkMillis = prefs.getLong(LAST_OK, 0L).takeIf { it > 0 },
        lastExpired = readEntry(EXPIRED),
    )

    private fun SharedPreferences.Editor.writeEntry(prefix: String, entry: CheckEntry?) {
        if (entry == null) return
        putLong("$prefix.at", entry.atMillis)
        putString("$prefix.result", entry.result.name)
        putString("$prefix.trigger", entry.trigger.name)
        putString("$prefix.detail", entry.detail)
    }

    private fun readEntry(prefix: String): CheckEntry? {
        val at = prefs.getLong("$prefix.at", 0L).takeIf { it > 0 } ?: return null
        val result = CheckResult.entries.firstOrNull { it.name == prefs.getString("$prefix.result", null) } ?: return null
        val trigger = CheckTrigger.entries.firstOrNull { it.name == prefs.getString("$prefix.trigger", null) }
            ?: CheckTrigger.SCHEDULED
        return CheckEntry(at, result, trigger, prefs.getString("$prefix.detail", "").orEmpty())
    }

    private companion object {
        const val LAST = "last"
        const val EXPIRED = "expired"
        const val LAST_OK = "last_ok"
    }
}
