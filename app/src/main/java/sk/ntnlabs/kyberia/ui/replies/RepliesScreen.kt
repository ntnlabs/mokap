package sk.ntnlabs.kyberia.ui.replies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.parse.Reply
import sk.ntnlabs.kyberia.ui.common.Avatar
import sk.ntnlabs.kyberia.ui.overview.OverviewViewModel

/** Odpovede na tvoje príspevky; klik otvorí tvoj príspevok, pod ktorým odpoveď visí. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepliesScreen(viewModel: OverviewViewModel, onOpenNode: (Long) -> Unit) {
    val state = viewModel.state
    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            if (!state.loading && state.replies.isEmpty()) {
                item { Text(stringResource(R.string.replies_empty), Modifier.padding(16.dp)) }
            }
            items(state.replies) { reply ->
                ReplyRow(reply, onClick = { onOpenNode(reply.nodeId) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ReplyRow(reply: Reply, onClick: () -> Unit) {
    ListItem(
        leadingContent = { Avatar(reply.avatarUrl) },
        overlineContent = { Text(reply.author, color = MaterialTheme.colorScheme.secondary) },
        headlineContent = { Text(reply.snippet, maxLines = 3, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(stringResource(R.string.replies_on, reply.nodeTitle), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
