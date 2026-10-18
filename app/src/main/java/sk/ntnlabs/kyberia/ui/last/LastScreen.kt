package sk.ntnlabs.kyberia.ui.last

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.data.Nsfw
import sk.ntnlabs.kyberia.parse.LastPost
import sk.ntnlabs.kyberia.ui.common.Avatar
import sk.ntnlabs.kyberia.ui.overview.OverviewViewModel

/** Najnovšie príspevky na celej kyberii; klik otvorí príspevok na webe. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastScreen(viewModel: OverviewViewModel, hideNsfw: Boolean, onOpenNode: (Long) -> Unit, onOpenAll: () -> Unit) {
    val state = viewModel.state
    val posts = Nsfw.last(state.last, hideNsfw)
    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            if (!state.loading && posts.isEmpty()) {
                item { Text(stringResource(R.string.last_empty), Modifier.padding(16.dp)) }
            }
            items(posts) { post ->
                LastRow(post, onClick = { onOpenNode(post.nodeId) })
                HorizontalDivider()
            }
            item {
                TextButton(onClick = onOpenAll, modifier = Modifier.padding(8.dp)) {
                    Text(stringResource(R.string.last_more))
                }
            }
        }
    }
}

@Composable
private fun LastRow(post: LastPost, onClick: () -> Unit) {
    ListItem(
        leadingContent = { Avatar(post.avatarUrl) },
        overlineContent = {
            Text(
                listOfNotNull(post.author.ifEmpty { null }, post.time).joinToString(" · "),
                color = MaterialTheme.colorScheme.secondary,
            )
        },
        // Úryvok povie viac než názvy typu „Re[6]: 16.10.…“.
        // 🔞 len v názvoch nodov, nie v texte príspevku.
        headlineContent = { Text(post.snippet.ifEmpty { Nsfw.label(post.title) }, maxLines = 3, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                Text(Nsfw.label(post.title), maxLines = 1, overflow = TextOverflow.Ellipsis)
                post.parentTitle?.let {
                    Text(
                        stringResource(R.string.last_in, Nsfw.label(it)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
