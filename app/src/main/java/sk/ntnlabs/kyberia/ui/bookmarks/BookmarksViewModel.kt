package sk.ntnlabs.kyberia.ui.bookmarks

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
import sk.ntnlabs.kyberia.data.NewsTracker
import sk.ntnlabs.kyberia.data.BookmarksRepository
import sk.ntnlabs.kyberia.data.SessionExpiredException
import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.BookmarksParser
import sk.ntnlabs.kyberia.parse.Reply
import sk.ntnlabs.kyberia.parse.UserInfo

data class BookmarksUiState(
    val loading: Boolean = false,
    val bookmarks: List<Bookmark> = emptyList(),
    val replies: List<Reply> = emptyList(),
    val user: UserInfo? = null,
    val showAll: Boolean = false,
    val error: Boolean = false,
) {
    val visible: List<Bookmark> get() = if (showAll) bookmarks else bookmarks.filter { it.hasNews }
}

class BookmarksViewModel(
    private val repository: BookmarksRepository,
    private val tracker: NewsTracker,
    private val auth: AuthRepository,
) : ViewModel() {

    var state by mutableStateOf(BookmarksUiState())
        private set

    init {
        // Po prihlásení načítaj, po odhlásení zahoď dáta predchádzajúceho účtu.
        viewModelScope.launch {
            auth.loggedIn.collect { loggedIn ->
                if (loggedIn) refresh() else state = BookmarksUiState()
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
                state.copy(
                    loading = false,
                    bookmarks = BookmarksParser.sortByNews(page.bookmarks),
                    replies = page.replies.filter { it.authorId != page.user.userId },
                    user = page.user,
                )
            } catch (e: SessionExpiredException) {
                auth.sessionEnded()
                BookmarksUiState()
            } catch (e: IOException) {
                state.copy(loading = false, error = true)
            }
        }
    }

    fun toggleShowAll() {
        state = state.copy(showAll = !state.showAll)
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { BookmarksViewModel(container.bookmarks, container.newsTracker, container.auth) }
        }
    }
}
