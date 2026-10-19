package sk.ntnlabs.kyberia.ui.bookmarks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.data.BookmarkFilter
import sk.ntnlabs.kyberia.data.Nsfw
import sk.ntnlabs.kyberia.data.BookmarkSort
import sk.ntnlabs.kyberia.data.SettingsStore
import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.ui.overview.OverviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(viewModel: OverviewViewModel, settingsStore: SettingsStore, onOpenNode: (Long) -> Unit) {
    val state = viewModel.state
    val settings by settingsStore.settings.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(state.bookmarks, query, settings.bookmarksOnlyNews, settings.bookmarkSort, settings.hideNsfw) {
        BookmarkFilter.apply(state.bookmarks, query, settings.bookmarksOnlyNews, settings.bookmarkSort, settings.hideNsfw)
    }
    val listState = rememberLazyListState()
    // Po zmene radenia, filtra alebo hľadania hore – inak človek stojí uprostred iného zoznamu.
    LaunchedEffect(settings.bookmarkSort, settings.bookmarksOnlyNews, settings.hideNsfw, query) {
        listState.scrollToItem(0)
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.bookmarks_search)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.bookmarks_search_clear),
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )
        // Radenie = jedna z dvoch možností (segmenty), „len nové“ = zapínač – aby sa to nedalo pomýliť.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                listOf(
                    BookmarkSort.ALPHABET to R.string.bookmarks_sort_alphabet,
                    BookmarkSort.NEWS to R.string.bookmarks_sort_news,
                ).forEachIndexed { index, (sort, label) ->
                    SegmentedButton(
                        selected = settings.bookmarkSort == sort,
                        onClick = { settingsStore.update { it.copy(bookmarkSort = sort) } },
                        shape = SegmentedButtonDefaults.itemShape(index, count = 2),
                    ) { Text(stringResource(label), maxLines = 1) }
                }
            }
            Spacer(Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.toggleable(
                    value = settings.bookmarksOnlyNews,
                    role = Role.Switch,
                    onValueChange = { on -> settingsStore.update { it.copy(bookmarksOnlyNews = on) } },
                ),
            ) {
                Text(stringResource(R.string.bookmarks_only_news), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(8.dp))
                // Klik rieši celý riadok (toggleable), prepínač je len obrázok stavu.
                Switch(checked = settings.bookmarksOnlyNews, onCheckedChange = null)
            }
        }

        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(Modifier.fillMaxSize(), state = listState) {
                when {
                    state.error -> item { LoadError(onRetry = viewModel::refresh) }
                    !state.loading && shown.isEmpty() -> item {
                        val empty = if (query.isBlank() && settings.bookmarksOnlyNews) {
                            R.string.bookmarks_nothing_new
                        } else {
                            R.string.bookmarks_nothing_found
                        }
                        Text(stringResource(empty), Modifier.padding(16.dp))
                    }
                }
                items(shown, key = { "bookmark:${it.id}" }) { bookmark ->
                    BookmarkRow(bookmark, onClick = { onOpenNode(bookmark.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun BookmarkRow(bookmark: Bookmark, onClick: () -> Unit) {
    val descendants = stringResource(R.string.bookmark_descendants)
    val changed = stringResource(R.string.bookmark_changed)
    val flags = listOfNotNull(
        descendants.takeIf { bookmark.newDescendants },
        changed.takeIf { bookmark.changed },
    ).joinToString(" · ")

    ListItem(
        headlineContent = { Text(Nsfw.label(bookmark.title), maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = if (flags.isNotEmpty()) {
            { Text(flags, color = MaterialTheme.colorScheme.secondary) }
        } else {
            null
        },
        trailingContent = if (bookmark.newChildren > 0) {
            { Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("+${bookmark.newChildren}") } }
        } else {
            null
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun LoadError(onRetry: () -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text(stringResource(R.string.bookmarks_load_error), color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
