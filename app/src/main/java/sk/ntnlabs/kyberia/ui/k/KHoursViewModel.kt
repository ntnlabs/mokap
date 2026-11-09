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
import sk.ntnlabs.kyberia.data.MyKRepository
import sk.ntnlabs.kyberia.data.SessionExpiredException
import sk.ntnlabs.kyberia.parse.MyK
import sk.ntnlabs.kyberia.work.CheckScheduler

data class KHoursUiState(
    val loading: Boolean = false,
    /** Za koľko hodín sú [items] – po zmene v nastaveniach sa stiahnu znova. */
    val hours: Int? = null,
    val items: List<MyK>? = null,
    val error: Boolean = false,
)

/** Záložka „K za N h“ – sťahuje sa len pri otvorení záložky (a potiahnutí nadol), nikdy v pozadí. */
class KHoursViewModel(
    private val repository: MyKRepository,
    private val auth: AuthRepository,
    private val diagnostics: DiagnosticsStore,
    private val scheduler: CheckScheduler,
) : ViewModel() {

    var state by mutableStateOf(KHoursUiState())
        private set

    private var loadedAtMillis = 0L

    init {
        viewModelScope.launch {
            auth.loggedIn.collect { loggedIn ->
                if (!loggedIn) {
                    loadedAtMillis = 0L
                    state = KHoursUiState()
                }
            }
        }
    }

    /** Pri otvorení záložky: znova len ak sa zmenil interval, alebo sú dáta staršie ako minúta (rýchle prepínanie). */
    fun refreshIfStale(hours: Int) {
        if (state.hours != hours || System.currentTimeMillis() - loadedAtMillis > STALE_AFTER_MILLIS) refresh(hours)
    }

    fun refresh(hours: Int) {
        if (state.loading || !auth.loggedIn.value) return
        state = state.copy(loading = true, error = false)
        viewModelScope.launch {
            state = try {
                val items = repository.load(hours)
                loadedAtMillis = System.currentTimeMillis()
                scheduler.touch()
                KHoursUiState(hours = hours, items = items)
            } catch (e: SessionExpiredException) {
                diagnostics.record(CheckEntry(System.currentTimeMillis(), CheckResult.EXPIRED, CheckTrigger.APP, e.detail))
                auth.sessionEnded()
                KHoursUiState()
            } catch (e: IOException) {
                state.copy(loading = false, error = true)
            }
        }
    }

    companion object {
        private const val STALE_AFTER_MILLIS = 60_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { KHoursViewModel(container.myK, container.auth, container.diagnostics, container.scheduler) }
        }
    }
}
