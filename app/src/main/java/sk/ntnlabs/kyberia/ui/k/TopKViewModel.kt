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
import sk.ntnlabs.kyberia.data.SessionExpiredException
import sk.ntnlabs.kyberia.data.TopKRepository
import sk.ntnlabs.kyberia.parse.TopKItem
import sk.ntnlabs.kyberia.work.CheckScheduler

/** Tvoje K idú z /id/19/ (OverviewViewModel); top K z vlastnej stránky, len na požiadanie. */
enum class KMode { MINE, TOP }

data class TopKUiState(
    val mode: KMode = KMode.MINE,
    val loading: Boolean = false,
    val items: List<TopKItem>? = null,
    val error: Boolean = false,
)

class TopKViewModel(
    private val repository: TopKRepository,
    private val auth: AuthRepository,
    private val diagnostics: DiagnosticsStore,
    private val scheduler: CheckScheduler,
) : ViewModel() {

    var state by mutableStateOf(TopKUiState())
        private set

    private var loadedAtMillis = 0L

    init {
        viewModelScope.launch {
            auth.loggedIn.collect { loggedIn ->
                if (!loggedIn) {
                    loadedAtMillis = 0L
                    state = TopKUiState()
                }
            }
        }
    }

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
                val items = repository.load()
                loadedAtMillis = System.currentTimeMillis()
                scheduler.touch()
                state.copy(loading = false, items = items)
            } catch (e: SessionExpiredException) {
                diagnostics.record(CheckEntry(System.currentTimeMillis(), CheckResult.EXPIRED, CheckTrigger.APP, e.detail))
                auth.sessionEnded()
                TopKUiState()
            } catch (e: IOException) {
                state.copy(loading = false, error = true)
            }
        }
    }

    companion object {
        private const val STALE_AFTER_MILLIS = 5 * 60_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { TopKViewModel(container.topK, container.auth, container.diagnostics, container.scheduler) }
        }
    }
}
