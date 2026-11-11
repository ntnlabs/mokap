package sk.ntnlabs.kyberia.ui.mail

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
import sk.ntnlabs.kyberia.data.MailRepository
import sk.ntnlabs.kyberia.data.SendResult
import sk.ntnlabs.kyberia.data.SessionExpiredException
import sk.ntnlabs.kyberia.parse.MailPage
import sk.ntnlabs.kyberia.ui.node.ReplyProblem
import sk.ntnlabs.kyberia.work.CheckScheduler

data class MailUiState(
    val loading: Boolean = false,
    val page: MailPage? = null,
    val error: Boolean = false,
    /** Správa sa práve odosiela – druhé odoslanie ani obnovenie sa nespustí. */
    val sending: Boolean = false,
    /** Posledná správa odišla – obrazovka zavrie editor a zahodí text ([MailViewModel.sendHandled]). */
    val sent: Boolean = false,
    val problem: ReplyProblem? = null,
)

/**
 * Pošta (`/id/24/`) – sťahuje sa len pri otvorení záložky a potiahnutí nadol, nikdy v pozadí. Kyberia ju tým, ako
 * na webe, označí ako prečítanú – preto [onRead] zruší notifikáciu a číslo na záložke. Správy sú len v pamäti.
 */
class MailViewModel(
    private val repository: MailRepository,
    private val auth: AuthRepository,
    private val diagnostics: DiagnosticsStore,
    private val scheduler: CheckScheduler,
    private val onRead: () -> Unit,
) : ViewModel() {

    var state by mutableStateOf(MailUiState())
        private set

    private var loadedAtMillis = 0L

    init {
        viewModelScope.launch {
            auth.loggedIn.collect { loggedIn ->
                if (!loggedIn) {
                    loadedAtMillis = 0L
                    state = MailUiState()
                }
            }
        }
    }

    /** Pri otvorení záložky – pri rýchlom prepínaní (do pol minúty) znova nesťahuje. */
    fun refreshIfStale() {
        if (System.currentTimeMillis() - loadedAtMillis > STALE_AFTER_MILLIS) refresh()
    }

    fun refresh() {
        if (state.loading || state.sending || !auth.loggedIn.value) return
        state = state.copy(loading = true, error = false)
        viewModelScope.launch {
            state = try {
                val page = repository.load()
                loadedAtMillis = System.currentTimeMillis()
                scheduler.touch()
                onRead()
                state.copy(loading = false, page = page)
            } catch (e: SessionExpiredException) {
                expired(e)
            } catch (e: IOException) {
                state.copy(loading = false, error = true)
            }
        }
    }

    /**
     * Pošle správu – len na ťuknutie, nikdy znova sama (pri chybe spojenia nevieme, či odišla). [byId] = adresát je
     * číslo profilu (odpoveď), inak nick.
     */
    fun send(to: String, byId: Boolean, text: String, noHtml: Boolean) {
        val page = state.page ?: return
        if (state.sending || state.loading || page.form == null || to.isBlank() || text.isBlank() || !auth.loggedIn.value) return
        state = state.copy(sending = true, sent = false, problem = null)
        viewModelScope.launch {
            state = try {
                val result = repository.send(page, to, byId, text, noHtml)
                loadedAtMillis = System.currentTimeMillis()
                scheduler.touch()
                onRead()
                when (result) {
                    is SendResult.Sent -> state.copy(sending = false, page = result.page, sent = true)
                    is SendResult.Rejected -> state.copy(sending = false, page = result.page, problem = ReplyProblem.Rejected(result.message))
                    is SendResult.Unconfirmed -> state.copy(sending = false, page = result.page, problem = ReplyProblem.Unconfirmed)
                }
            } catch (e: SessionExpiredException) {
                expired(e)
            } catch (e: IOException) {
                state.copy(sending = false, problem = ReplyProblem.Network)
            }
        }
    }

    /** Obrazovka spracovala výsledok (zavrela editor alebo ho používateľ zatvoril). */
    fun sendHandled() {
        if (state.sent || state.problem != null) state = state.copy(sent = false, problem = null)
    }

    private fun expired(e: SessionExpiredException): MailUiState {
        diagnostics.record(CheckEntry(System.currentTimeMillis(), CheckResult.EXPIRED, CheckTrigger.APP, e.detail))
        auth.sessionEnded()
        return MailUiState()
    }

    companion object {
        private const val STALE_AFTER_MILLIS = 30_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                MailViewModel(container.mail, container.auth, container.diagnostics, container.scheduler, container::mailRead)
            }
        }
    }
}
