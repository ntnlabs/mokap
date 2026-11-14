package sk.ntnlabs.kyberia.ui.k

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.io.IOException
import kotlinx.coroutines.launch
import sk.ntnlabs.kyberia.AppContainer
import sk.ntnlabs.kyberia.data.AuthRepository
import sk.ntnlabs.kyberia.data.CheckEntry
import sk.ntnlabs.kyberia.data.CheckResult
import sk.ntnlabs.kyberia.data.CheckTrigger
import sk.ntnlabs.kyberia.data.DiagnosticsStore
import sk.ntnlabs.kyberia.data.KResult
import sk.ntnlabs.kyberia.data.KStart
import sk.ntnlabs.kyberia.data.SessionExpiredException
import sk.ntnlabs.kyberia.data.SettingsStore
import sk.ntnlabs.kyberia.data.TopKRepository
import sk.ntnlabs.kyberia.parse.TopKItem
import sk.ntnlabs.kyberia.work.CheckScheduler

/** Tvoje K za hodinu idú z /id/19/ (OverviewViewModel); za N hodín ([KHoursViewModel]) a top K len na požiadanie. */
enum class KMode { MINE, HOURS, TOP }

data class TopKUiState(
    val mode: KMode = KMode.MINE,
    val loading: Boolean = false,
    val items: List<TopKItem>? = null,
    val error: Boolean = false,
    /** Stav K pri príspevkoch (ID nodu). „Dané“ ostáva aj po obnovení – Top K neukazuje, komu si K dal. */
    val k: Map<Long, TopKStatus> = emptyMap(),
)

/** K pri príspevku v Top K. */
sealed interface TopKStatus {
    data object Sending : TopKStatus

    /** K od teba tam je – dané teraz alebo už predtým. */
    data object Done : TopKStatus

    data class Rejected(val message: String) : TopKStatus

    data object Unconfirmed : TopKStatus

    /** Spojenie zlyhalo po odoslaní – K možno prešlo. */
    data object Network : TopKStatus

    /** Počet K klesol – hlásiť, nikdy zámerne. */
    data object Removed : TopKStatus
}

class TopKViewModel(
    private val repository: TopKRepository,
    private val auth: AuthRepository,
    private val diagnostics: DiagnosticsStore,
    private val scheduler: CheckScheduler,
    private val settings: SettingsStore,
) : ViewModel() {

    var state by mutableStateOf(TopKUiState(mode = startMode()))
        private set

    private var loadedAtMillis = 0L

    /** Token session zo stránky Top K – len v pamäti, na K (ako web `ajaxK`). */
    private var anticsrf: String? = null

    /** Či sa dá K dať (je token, ešte nie je dané ani sa neposiela). Aj vlastnému príspevku – kyberia to dovolí. */
    fun canGiveK(item: TopKItem): Boolean =
        anticsrf != null && state.k[item.id].let { it != TopKStatus.Sending && it != TopKStatus.Done }

    init {
        viewModelScope.launch {
            auth.loggedIn.collect { loggedIn ->
                if (!loggedIn) {
                    loadedAtMillis = 0L
                    anticsrf = null
                    state = TopKUiState(mode = startMode())
                }
            }
        }
    }

    /** Záložka K je zavretá – pri ďalšom otvorení ukáže to, čo je v nastaveniach (Tvoje K / Top K). */
    fun resetToStart() {
        val start = startMode()
        if (state.mode != start) state = state.copy(mode = start, error = false)
    }

    private fun startMode(): KMode = if (settings.current.kStart == KStart.TOP) KMode.TOP else KMode.MINE

    fun select(mode: KMode) {
        state = state.copy(mode = mode, error = false)
        refreshIfStale()
    }

    /** Top K sa stiahne, len keď je na obrazovke a je staršie ako [STALE_AFTER_MILLIS] – mení sa pomaly. */
    fun refreshIfStale() {
        if (state.mode == KMode.TOP && System.currentTimeMillis() - loadedAtMillis > STALE_AFTER_MILLIS) refresh()
    }

    fun refresh() {
        if (state.mode != KMode.TOP || state.loading || !auth.loggedIn.value) return
        state = state.copy(loading = true, error = false)
        viewModelScope.launch {
            state = try {
                val page = repository.load()
                anticsrf = page.anticsrf
                loadedAtMillis = System.currentTimeMillis()
                scheduler.touch()
                state.copy(loading = false, items = page.items, k = state.k.filterValues { it == TopKStatus.Done })
            } catch (e: SessionExpiredException) {
                diagnostics.record(CheckEntry(System.currentTimeMillis(), CheckResult.EXPIRED, CheckTrigger.APP, e.detail))
                auth.sessionEnded()
                TopKUiState()
            } catch (e: IOException) {
                state.copy(loading = false, error = true)
            }
        }
    }

    /** Jedno K príspevku – len po potvrdení používateľa; nikdy znova samo, nikdy `revoKe`. */
    fun giveK(item: TopKItem) {
        val token = anticsrf ?: return
        if (!canGiveK(item) || !auth.loggedIn.value) return
        state = state.copy(k = state.k + (item.id to TopKStatus.Sending))
        viewModelScope.launch {
            val status = try {
                val result = repository.giveK(item, token)
                scheduler.touch()
                // Počet K v zozname podľa vrátenej stránky nodu.
                result.page.k?.let { count ->
                    state = state.copy(items = state.items?.map { if (it.id == item.id) it.copy(k = count) else it })
                }
                when (result) {
                    is KResult.Given, is KResult.Already -> TopKStatus.Done
                    is KResult.Rejected -> TopKStatus.Rejected(result.message)
                    is KResult.Unconfirmed -> TopKStatus.Unconfirmed
                    is KResult.Removed -> TopKStatus.Removed
                }
            } catch (e: SessionExpiredException) {
                diagnostics.record(CheckEntry(System.currentTimeMillis(), CheckResult.EXPIRED, CheckTrigger.APP, e.detail))
                auth.sessionEnded()
                state = TopKUiState()
                return@launch
            } catch (e: IOException) {
                TopKStatus.Network
            }
            state = state.copy(k = state.k + (item.id to status))
        }
    }

    companion object {
        private const val STALE_AFTER_MILLIS = 5 * 60_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                TopKViewModel(container.topK, container.auth, container.diagnostics, container.scheduler, container.settings)
            }
        }
    }
}
