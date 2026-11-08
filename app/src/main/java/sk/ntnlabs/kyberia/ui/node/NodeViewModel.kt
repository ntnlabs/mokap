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
import sk.ntnlabs.kyberia.data.LatestPage
import sk.ntnlabs.kyberia.data.NodeRepository
import sk.ntnlabs.kyberia.data.ReplyResult
import sk.ntnlabs.kyberia.data.SessionExpiredException
import sk.ntnlabs.kyberia.parse.NodePage
import sk.ntnlabs.kyberia.work.CheckScheduler

data class NodeUiState(
    val loading: Boolean = false,
    val page: NodePage? = null,
    val error: Boolean = false,
    /** Odpoveď sa práve odosiela – druhé odoslanie ani obnovenie sa nespustí. */
    val sending: Boolean = false,
    /** Posledná odpoveď prešla – obrazovka zavrie editor a zahodí text ([NodesViewModel.replyHandled]). */
    val replySent: Boolean = false,
    val replyProblem: ReplyProblem? = null,
)

/** Prečo odpoveď (možno) neprešla; text v editore vždy ostáva. */
sealed interface ReplyProblem {
    data class Rejected(val message: String) : ReplyProblem

    /** Kyberia chybu nehlásila, ale novú odpoveď na stránke nevidno. */
    data object Unconfirmed : ReplyProblem

    /** Spojenie zlyhalo po odoslaní – nevieme, či prešla. */
    data object Network : ReplyProblem
}

/**
 * Otvorené nody (zásobník v HomeScreen). Každý sa stiahne pri otvorení a pri potiahnutí nadol, nikdy
 * na pozadí; node, ktorý zo zásobníka zmizne, sa z pamäte uvoľní ([keepOnly]).
 */
class NodesViewModel(
    private val repository: NodeRepository,
    private val auth: AuthRepository,
    private val diagnostics: DiagnosticsStore,
    private val scheduler: CheckScheduler,
    private val latest: LatestPage,
) : ViewModel() {

    private val states = mutableStateMapOf<Long, NodeUiState>()

    fun state(id: Long): NodeUiState = states[id] ?: NodeUiState()

    /** Pri otvorení – už stiahnutý node (návrat späť) sa znova nesťahuje. */
    fun open(id: Long) {
        if (id !in states) load(id)
    }

    fun load(id: Long) {
        if (state(id).loading || state(id).sending || !auth.loggedIn.value) return
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

    /**
     * Odošle odpoveď do nodu (alebo na odpoveď [replyTo]). Len na ťuknutie používateľa, nikdy znova sama –
     * pri chybe spojenia nevieme, či prešla, a druhý pokus by mohol príspevok zdvojiť.
     */
    fun reply(id: Long, content: String, replyTo: Long?, noHtml: Boolean) {
        val current = state(id)
        val page = current.page ?: return
        if (current.sending || current.loading || page.replyForm == null || content.isBlank() || !auth.loggedIn.value) return
        states[id] = current.copy(sending = true, replySent = false, replyProblem = null)
        viewModelScope.launch {
            val result = try {
                val outcome = repository.reply(page, content, replyTo, noHtml, me = latest.flow.value?.page?.user?.userId)
                scheduler.touch()
                when (outcome) {
                    is ReplyResult.Added -> NodeUiState(page = outcome.page, replySent = true)
                    is ReplyResult.Rejected -> NodeUiState(page = outcome.page, replyProblem = ReplyProblem.Rejected(outcome.message))
                    is ReplyResult.Unconfirmed -> NodeUiState(page = outcome.page, replyProblem = ReplyProblem.Unconfirmed)
                }
            } catch (e: SessionExpiredException) {
                diagnostics.record(CheckEntry(System.currentTimeMillis(), CheckResult.EXPIRED, CheckTrigger.APP, e.detail))
                auth.sessionEnded()
                NodeUiState()
            } catch (e: IOException) {
                state(id).copy(sending = false, replyProblem = ReplyProblem.Network)
            }
            if (id in states) states[id] = result
        }
    }

    /** Obrazovka spracovala výsledok (zavrela editor alebo ho používateľ zatvoril). */
    fun replyHandled(id: Long) {
        val current = states[id] ?: return
        if (current.replySent || current.replyProblem != null) states[id] = current.copy(replySent = false, replyProblem = null)
    }

    fun keepOnly(ids: Set<Long>) {
        states.keys.retainAll(ids)
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                NodesViewModel(container.nodes, container.auth, container.diagnostics, container.scheduler, container.latestPage)
            }
        }
    }
}
