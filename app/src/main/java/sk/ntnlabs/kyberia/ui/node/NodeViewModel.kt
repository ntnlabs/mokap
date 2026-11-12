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
import sk.ntnlabs.kyberia.data.EditResult
import sk.ntnlabs.kyberia.data.KResult
import sk.ntnlabs.kyberia.data.LatestPage
import sk.ntnlabs.kyberia.data.NodeRepository
import sk.ntnlabs.kyberia.data.ReplyResult
import sk.ntnlabs.kyberia.data.SessionExpiredException
import sk.ntnlabs.kyberia.parse.EditForm
import sk.ntnlabs.kyberia.parse.NodePage
import sk.ntnlabs.kyberia.work.CheckScheduler

data class NodeUiState(
    val loading: Boolean = false,
    val page: NodePage? = null,
    val error: Boolean = false,
    /** Akcia (odpoveď, K, úprava) sa práve odosiela – druhá ani obnovenie sa nespustí. */
    val sending: Boolean = false,
    /** Posledná odpoveď prešla – obrazovka zavrie editor a zahodí text ([NodesViewModel.replyHandled]). */
    val replySent: Boolean = false,
    /** ID práve pridanej odpovede – obrazovka na ňu odroluje a potvrdí ([NodesViewModel.replyShown]). */
    val newReply: Long? = null,
    val replyProblem: ReplyProblem? = null,
    /** Úprava vlastného príspevku; null = editor zavretý. */
    val edit: EditUiState? = null,
    /** Krátka správa po akcii pre snackbar; obrazovka ju potvrdí ([NodesViewModel.noticeHandled]). */
    val notice: NodeNotice? = null,
)

/** Úprava príspevku [target] – pôvodný text z jeho „conf“ stránky. */
data class EditUiState(
    val target: Long,
    val form: EditForm? = null,
    /** Kyberia úpravu neponúkla (nie je to tvoj príspevok, alebo sa zmenila stránka). */
    val unavailable: Boolean = false,
    val loadFailed: Boolean = false,
    val problem: ReplyProblem? = null,
) {
    val loading: Boolean get() = form == null && !unavailable && !loadFailed
}

sealed interface NodeNotice {
    data object KGiven : NodeNotice

    data class KRejected(val message: String) : NodeNotice

    data object KUnconfirmed : NodeNotice

    /** Počet K klesol – kyberia K odobrala (prepína ho). */
    data object KRemoved : NodeNotice

    /** K od teba tam už bolo. */
    data object KAlready : NodeNotice

    /** Spojenie zlyhalo po odoslaní akcie – nevieme, či prešla. */
    data object Network : NodeNotice

    data object Edited : NodeNotice
}

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

    /**
     * Komu appka v tejto relácii dala K. Pri samotnom node to kyberia ukazuje sama (`NodePage.kGiven` – tlačidlo „-❤️“),
     * pri odpovediach nie – tie si appka pamätá, aby K znova neponúkla.
     */
    private val kGiven = mutableStateMapOf<Long, Boolean>()

    fun kGiven(id: Long): Boolean = kGiven[id] == true

    /** Vlastné ID z poslednej /id/19/ – podľa neho appka ponúkne úpravu vlastného príspevku. */
    val me: Long? get() = latest.flow.value?.page?.user?.userId

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
                    is ReplyResult.Added -> NodeUiState(page = outcome.page, replySent = true, newReply = outcome.childId)
                    is ReplyResult.Rejected -> NodeUiState(page = outcome.page, replyProblem = ReplyProblem.Rejected(outcome.message))
                    is ReplyResult.Unconfirmed -> NodeUiState(page = outcome.page, replyProblem = ReplyProblem.Unconfirmed)
                }
            } catch (e: SessionExpiredException) {
                expired(e)
            } catch (e: IOException) {
                state(id).copy(sending = false, replyProblem = ReplyProblem.Network)
            }
            if (id in states) states[id] = result
        }
    }

    /** Jedno K nodu (bez [childId]) alebo odpovedi – len na potvrdenie používateľa. Odobrať K appka nevie. */
    fun giveK(id: Long, childId: Long?) {
        val current = state(id)
        val page = current.page ?: return
        if (current.sending || current.loading || !auth.loggedIn.value) return
        if ((if (childId == null) page.kNode else page.kChild) == null) return
        val target = childId ?: id
        if (kGiven(target) || (childId == null && page.kGiven)) return
        states[id] = current.copy(sending = true, notice = null)
        viewModelScope.launch {
            val result = try {
                val outcome = repository.giveK(page, childId)
                scheduler.touch()
                when (outcome) {
                    is KResult.Given, is KResult.Already -> kGiven[target] = true
                    is KResult.Removed -> kGiven.remove(target)
                    else -> Unit
                }
                state(id).copy(
                    page = outcome.page,
                    sending = false,
                    notice = when (outcome) {
                        is KResult.Given -> NodeNotice.KGiven
                        is KResult.Rejected -> NodeNotice.KRejected(outcome.message)
                        is KResult.Unconfirmed -> NodeNotice.KUnconfirmed
                        is KResult.Removed -> NodeNotice.KRemoved
                        is KResult.Already -> NodeNotice.KAlready
                    },
                )
            } catch (e: SessionExpiredException) {
                expired(e)
            } catch (e: IOException) {
                state(id).copy(sending = false, notice = NodeNotice.Network)
            }
            if (id in states) states[id] = result
        }
    }

    /** Otvorí úpravu vlastného príspevku [target] (node alebo odpoveď v ňom) – stiahne jeho „conf“ stránku. */
    fun startEdit(id: Long, target: Long) {
        val current = state(id)
        if (current.sending || current.edit != null || !auth.loggedIn.value) return
        states[id] = current.copy(edit = EditUiState(target))
        viewModelScope.launch {
            val edit = try {
                val form = repository.editSource(target)
                scheduler.touch()
                if (form == null) EditUiState(target, unavailable = true) else EditUiState(target, form = form)
            } catch (e: SessionExpiredException) {
                if (id in states) states[id] = expired(e)
                return@launch
            } catch (e: IOException) {
                EditUiState(target, loadFailed = true)
            }
            // Medzitým zatvorený editor sa znova neotvorí.
            val now = states[id] ?: return@launch
            if (now.edit?.target == target) states[id] = now.copy(edit = edit)
        }
    }

    fun saveEdit(id: Long, content: String) {
        val current = state(id)
        val edit = current.edit ?: return
        val form = edit.form ?: return
        if (current.sending || content.isBlank() || !auth.loggedIn.value) return
        states[id] = current.copy(sending = true, edit = edit.copy(problem = null))
        viewModelScope.launch {
            val result = try {
                val outcome = repository.saveEdit(form, content, editedId = edit.target, shownId = id)
                scheduler.touch()
                val now = state(id)
                when (outcome) {
                    is EditResult.Saved -> now.copy(page = outcome.page ?: now.page, sending = false, edit = null, notice = NodeNotice.Edited)
                    is EditResult.Rejected -> now.copy(
                        page = outcome.page ?: now.page,
                        sending = false,
                        edit = now.edit?.copy(problem = ReplyProblem.Rejected(outcome.message)),
                    )
                }
            } catch (e: SessionExpiredException) {
                expired(e)
            } catch (e: IOException) {
                state(id).let { it.copy(sending = false, edit = it.edit?.copy(problem = ReplyProblem.Network)) }
            }
            if (id in states) states[id] = result
        }
    }

    fun closeEdit(id: Long) {
        val current = states[id] ?: return
        if (!current.sending && current.edit != null) states[id] = current.copy(edit = null)
    }

    fun noticeHandled(id: Long) {
        val current = states[id] ?: return
        if (current.notice != null) states[id] = current.copy(notice = null)
    }

    private fun expired(e: SessionExpiredException): NodeUiState {
        diagnostics.record(CheckEntry(System.currentTimeMillis(), CheckResult.EXPIRED, CheckTrigger.APP, e.detail))
        auth.sessionEnded()
        return NodeUiState()
    }

    /** Obrazovka spracovala výsledok (zavrela editor alebo ho používateľ zatvoril). */
    fun replyHandled(id: Long) {
        val current = states[id] ?: return
        if (current.replySent || current.replyProblem != null) states[id] = current.copy(replySent = false, replyProblem = null)
    }

    fun replyShown(id: Long) {
        val current = states[id] ?: return
        if (current.newReply != null) states[id] = current.copy(newReply = null)
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
