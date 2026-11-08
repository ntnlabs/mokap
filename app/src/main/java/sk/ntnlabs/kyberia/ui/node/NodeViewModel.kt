package sk.ntnlabs.kyberia.ui.node

import androidx.compose.runtime.mutableStateMapOf
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
import sk.ntnlabs.kyberia.data.NodeRepository
import sk.ntnlabs.kyberia.data.SessionExpiredException
import sk.ntnlabs.kyberia.parse.NodePage
import sk.ntnlabs.kyberia.work.CheckScheduler

data class NodeUiState(
    val loading: Boolean = false,
    val page: NodePage? = null,
    val error: Boolean = false,
)

/**
 * Otvorené nody (zásobník v HomeScreen). Každý sa stiahne pri otvorení a pri potiahnutí nadol, nikdy
 * na pozadí; node, ktorý zo zásobníka zmizne, sa z pamäte uvoľní ([keepOnly]).
 */
class NodesViewModel(
    private val repository: NodeRepository,
    private val auth: AuthRepository,
    private val diagnostics: DiagnosticsStore,
    private val scheduler: CheckScheduler,
) : ViewModel() {

    private val states = mutableStateMapOf<Long, NodeUiState>()

    fun state(id: Long): NodeUiState = states[id] ?: NodeUiState()

    /** Pri otvorení – už stiahnutý node (návrat späť) sa znova nesťahuje. */
    fun open(id: Long) {
        if (id !in states) load(id)
    }

    fun load(id: Long) {
        if (state(id).loading || !auth.loggedIn.value) return
        states[id] = state(id).copy(loading = true, error = false)
        viewModelScope.launch {
            val result = try {
                val page = repository.load(id)
                // Kyberia videla načítanie – strážca session sa posunie. Do diagnostiky nie: „načítanie v appke“
                // tam znamená /id/19/ a kontrola v pozadí by ju potom preskočila.
                scheduler.touch()
                NodeUiState(page = page)
            } catch (e: SessionExpiredException) {
                diagnostics.record(CheckEntry(System.currentTimeMillis(), CheckResult.EXPIRED, CheckTrigger.APP, e.detail))
                auth.sessionEnded()
                NodeUiState()
            } catch (e: IOException) {
                state(id).copy(loading = false, error = true)
            }
            // Medzitým zatvorený node netreba držať.
            if (id in states) states[id] = result
        }
    }

    fun keepOnly(ids: Set<Long>) {
        states.keys.retainAll(ids)
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { NodesViewModel(container.nodes, container.auth, container.diagnostics, container.scheduler) }
        }
    }
}
