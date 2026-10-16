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
import sk.ntnlabs.kyberia.data.NewsTracker
import sk.ntnlabs.kyberia.data.SessionExpiredException
import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.Friend
import sk.ntnlabs.kyberia.parse.Reply
import sk.ntnlabs.kyberia.parse.UserInfo

data class OverviewUiState(
    val loading: Boolean = false,
    val bookmarks: List<Bookmark> = emptyList(),
    val replies: List<Reply> = emptyList(),
    val friends: List<Friend> = emptyList(),
    val user: UserInfo? = null,
    val error: Boolean = false,
)

/** Dáta z /id/19/ – jedna požiadavka kŕmi bookmarky, odpovede, friends aj K_wallet. */
class OverviewViewModel(
    private val repository: BookmarksRepository,
    private val tracker: NewsTracker,
    private val auth: AuthRepository,
) : ViewModel() {

    var state by mutableStateOf(OverviewUiState())
        private set

    init {
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
                state.copy(
                    loading = false,
                    // Radenie a filter rieši obrazovka podľa nastavení (BookmarkFilter).
                    bookmarks = page.bookmarks,
                    replies = page.replies.filter { it.authorId != page.user.userId },
                    // Najaktívnejší (najkratšia nečinnosť) hore.
                    friends = page.friends.sortedBy { it.timeSeconds ?: Long.MAX_VALUE },
                    user = page.user,
                )
            } catch (e: SessionExpiredException) {
                auth.sessionEnded()
                OverviewUiState()
            } catch (e: IOException) {
                state.copy(loading = false, error = true)
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { OverviewViewModel(container.bookmarks, container.newsTracker, container.auth) }
        }
    }
}
