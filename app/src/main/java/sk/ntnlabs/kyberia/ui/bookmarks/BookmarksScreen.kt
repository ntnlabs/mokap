package sk.ntnlabs.kyberia.ui.bookmarks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.parse.Bookmark
import sk.ntnlabs.kyberia.parse.Reply
import sk.ntnlabs.kyberia.ui.common.Avatar
import sk.ntnlabs.kyberia.ui.overview.OverviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(viewModel: OverviewViewModel, onOpenNode: (Long) -> Unit) {
    val state = viewModel.state
    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            if (state.replies.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.replies_title)) }
                items(state.replies) { reply ->
                    ReplyRow(reply, onClick = { onOpenNode(reply.nodeId) })
                }
                item { HorizontalDivider() }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = viewModel::toggleShowAll)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.bookmarks_show_all), Modifier.weight(1f))
                    Switch(checked = state.showAll, onCheckedChange = { viewModel.toggleShowAll() })
                }
            }
            when {
                state.error -> item { LoadError(onRetry = viewModel::refresh) }
                !state.loading && state.visible.isEmpty() -> item {
                    Text(stringResource(R.string.bookmarks_nothing_new), Modifier.padding(16.dp))
                }
            }
            items(state.visible, key = { "bookmark:${it.id}" }) { bookmark ->
                BookmarkRow(bookmark, onClick = { onOpenNode(bookmark.id) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun ReplyRow(reply: Reply, onClick: () -> Unit) {
    ListItem(
        leadingContent = { Avatar(reply.avatarUrl) },
        overlineContent = { Text(reply.author, color = MaterialTheme.colorScheme.secondary) },
        headlineContent = { Text(reply.snippet, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(stringResource(R.string.replies_on, reply.nodeTitle), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
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
        headlineContent = { Text(bookmark.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
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
