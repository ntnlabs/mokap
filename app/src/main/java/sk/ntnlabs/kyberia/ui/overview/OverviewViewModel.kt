package sk.ntnlabs.kyberia.ui.overview

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
import sk.ntnlabs.kyberia.data.BookmarksRepository
import sk.ntnlabs.kyberia.data.CheckEntry
import sk.ntnlabs.kyberia.data.CheckResult
import sk.ntnlabs.kyberia.data.CheckTrigger
import sk.ntnlabs.kyberia.data.DiagnosticsStore
import sk.ntnlabs.kyberia.data.LatestPage
import sk.ntnlabs.kyberia.data.LoadedPage
import sk.ntnlabs.kyberia.data.NewsTracker
import sk.ntnlabs.kyberia.data.SeenArea
import sk.ntnlabs.kyberia.data.SessionExpiredException
import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.Friend
import sk.ntnlabs.kyberia.parse.LastPost
import sk.ntnlabs.kyberia.parse.Reply
import sk.ntnlabs.kyberia.parse.UserInfo
import sk.ntnlabs.kyberia.work.NewsChecker

data class OverviewUiState(
    val loading: Boolean = false,
    val bookmarks: List<Bookmark> = emptyList(),
    val replies: List<Reply> = emptyList(),
    val friends: List<Friend> = emptyList(),
    val last: List<LastPost> = emptyList(),
    val user: UserInfo? = null,
    val error: Boolean = false,
    /** Kedy sa naposledy stiahla /id/19/ – v appke alebo kontrolou v pozadí. */
    val loadedAtMillis: Long? = null,
    /** Záložky, kde je niečo nové od poslednej návštevy. */
    val changed: Set<SeenArea> = emptySet(),
)

/** Dáta z /id/19/ – jedna požiadavka kŕmi bookmarky, odpovede, last, friends aj K_wallet. */
class OverviewViewModel(
    private val repository: BookmarksRepository,
    private val tracker: NewsTracker,
    private val auth: AuthRepository,
    private val diagnostics: DiagnosticsStore,
    private val latest: LatestPage,
) : ViewModel() {

    var state by mutableStateOf(OverviewUiState())
        private set

    init {
        // Čokoľvek stiahne appka alebo kontrola v pozadí, ukáže sa tu.
        viewModelScope.launch {
            latest.flow.collect { loaded -> if (loaded != null) show(loaded) }
        }
        // Po prihlásení načítaj, po odhlásení zahoď dáta predchádzajúceho účtu.
        viewModelScope.launch {
            auth.loggedIn.collect { loggedIn ->
                if (loggedIn) refresh() else state = OverviewUiState()
            }
        }
    }

    fun refresh() {
        if (state.loading) return
        state = state.copy(loading = true, error = false)
        viewModelScope.launch {
            state = try {
                val page = repository.load()
                // Čo používateľ videl v appke, už netreba hlásiť notifikáciou.
                tracker.updateBookmarks(page.bookmarks)
                tracker.updateReplies(page.replies, page.user.userId)
                tracker.updateFriends(page.friends)
                val now = System.currentTimeMillis()
                diagnostics.record(CheckEntry(now, CheckResult.OK, CheckTrigger.APP))
                latest.publish(page, now)
                state.copy(loading = false)
            } catch (e: SessionExpiredException) {
                // Len keď kyberia naozaj ukázala prihlasovací formulár (pozri loggedInPage).
                diagnostics.record(
                    CheckEntry(System.currentTimeMillis(), CheckResult.EXPIRED, CheckTrigger.APP, NewsChecker.LOGIN_SHOWN),
                )
                auth.sessionEnded()
                OverviewUiState()
            } catch (e: IOException) {
                diagnostics.record(
                    CheckEntry(System.currentTimeMillis(), CheckResult.ERROR, CheckTrigger.APP, e.message ?: e.javaClass.simpleName),
                )
                state.copy(loading = false, error = true)
            }
        }
    }

    /** Obnoví, len ak sú dáta staršie ako [maxAgeMillis] – pri rýchlom prepínaní appiek nič nesťahuje. */
    fun refreshIfStale(maxAgeMillis: Long = STALE_AFTER_MILLIS) {
        if (System.currentTimeMillis() - (state.loadedAtMillis ?: 0L) > maxAgeMillis) refresh()
    }

    /** Záložka je na obrazovke – jej novinky sú videné. */
    fun markSeen(area: SeenArea) {
        if (state.loadedAtMillis == null) return
        tracker.markSeen(area, state.bookmarks, state.replies, state.last)
        if (area in state.changed) state = state.copy(changed = state.changed - area)
    }

    private fun show(loaded: LoadedPage) {
        val page = loaded.page
        val replies = page.replies.filter { it.authorId != page.user.userId }
        state = state.copy(
            // Radenie a filter rieši obrazovka podľa nastavení (BookmarkFilter).
            bookmarks = page.bookmarks,
            replies = replies,
            // Najaktívnejší (najkratšia nečinnosť) hore.
            friends = page.friends.sortedBy { it.timeSeconds ?: Long.MAX_VALUE },
            last = page.last,
            user = page.user,
            loadedAtMillis = loaded.atMillis,
            changed = tracker.tabChanges(page.bookmarks, replies, page.last),
        )
    }

    companion object {
        private const val STALE_AFTER_MILLIS = 60_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                OverviewViewModel(
                    container.bookmarks,
                    container.newsTracker,
                    container.auth,
                    container.diagnostics,
                    container.latestPage,
                )
            }
        }
    }
}
