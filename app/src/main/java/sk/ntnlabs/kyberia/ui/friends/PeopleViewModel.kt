package sk.ntnlabs.kyberia.ui.friends

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
import sk.ntnlabs.kyberia.data.PeopleRepository
import sk.ntnlabs.kyberia.data.SessionExpiredException
import sk.ntnlabs.kyberia.parse.Friend
import sk.ntnlabs.kyberia.parse.Populated
import sk.ntnlabs.kyberia.work.NewsChecker

/** Priatelia idú z /id/19/ (OverviewViewModel); všetci a diskusie z vlastných stránok. */
enum class PeopleMode { FRIENDS, EVERYONE, POPULATED }

data class PeopleUiState(
    val mode: PeopleMode = PeopleMode.FRIENDS,
    val loading: Boolean = false,
    val everyone: List<Friend>? = null,
    val populated: Populated? = null,
    val error: Boolean = false,
)

class PeopleViewModel(
    private val repository: PeopleRepository,
    private val auth: AuthRepository,
    private val diagnostics: DiagnosticsStore,
) : ViewModel() {

    var state by mutableStateOf(PeopleUiState())
        private set

    private val loadedAtMillis = mutableMapOf<PeopleMode, Long>()

    init {
        viewModelScope.launch {
            auth.loggedIn.collect { loggedIn ->
                if (!loggedIn) {
                    loadedAtMillis.clear()
                    state = PeopleUiState()
                }
            }
        }
    }

    fun select(mode: PeopleMode) {
        state = state.copy(mode = mode, error = false)
        refreshIfStale()
    }

    /** Stiahne len to, čo je práve na obrazovke a je staršie ako minúta – žiadne sťahovanie do zásoby. */
    fun refreshIfStale() {
        val mode = state.mode
        if (mode != PeopleMode.FRIENDS && System.currentTimeMillis() - (loadedAtMillis[mode] ?: 0L) > STALE_AFTER_MILLIS) {
            refresh()
        }
    }

    fun refresh() {
        val mode = state.mode
        if (mode == PeopleMode.FRIENDS || state.loading) return
        state = state.copy(loading = true, error = false)
        viewModelScope.launch {
            state = try {
                val loaded = when (mode) {
                    // Najaktívnejší (najkratšia nečinnosť) hore, rovnako ako pri priateľoch.
                    PeopleMode.EVERYONE -> state.copy(everyone = repository.everyone().sortedBy { it.timeSeconds ?: Long.MAX_VALUE })
                    PeopleMode.POPULATED -> state.copy(populated = repository.populated())
                    PeopleMode.FRIENDS -> state
                }
                loadedAtMillis[mode] = System.currentTimeMillis()
                loaded.copy(loading = false)
            } catch (e: SessionExpiredException) {
                diagnostics.record(
                    CheckEntry(System.currentTimeMillis(), CheckResult.EXPIRED, CheckTrigger.APP, NewsChecker.LOGIN_SHOWN),
                )
                auth.sessionEnded()
                PeopleUiState()
            } catch (e: IOException) {
                state.copy(loading = false, error = true)
            }
        }
    }

    companion object {
        private const val STALE_AFTER_MILLIS = 60_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { PeopleViewModel(container.people, container.auth, container.diagnostics) }
        }
    }
}
