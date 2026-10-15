package sk.ntnlabs.kyberia.ui.friends

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
import sk.ntnlabs.kyberia.data.Durations
import sk.ntnlabs.kyberia.parse.Friend
import sk.ntnlabs.kyberia.ui.common.Avatar
import sk.ntnlabs.kyberia.ui.overview.OverviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(viewModel: OverviewViewModel, onOpenNode: (Long) -> Unit) {
    val state = viewModel.state
    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            if (!state.loading && state.friends.isEmpty()) {
                item { Text(stringResource(R.string.friends_nobody), Modifier.padding(16.dp)) }
            }
            items(state.friends, key = { "friend:${it.id}" }) { friend ->
                FriendRow(
                    friend = friend,
                    onOpenProfile = { onOpenNode(friend.id) },
                    onOpenLocation = { friend.locationId?.let(onOpenNode) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun FriendRow(friend: Friend, onOpenProfile: () -> Unit, onOpenLocation: () -> Unit) {
    val location = friend.locationTitle
    ListItem(
        leadingContent = { Avatar(friend.avatarUrl, size = 48.dp) },
        headlineContent = { Text(friend.nick, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = if (location != null) {
            {
                Text(
                    stringResource(R.string.friends_location, location),
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onOpenLocation),
                )
            }
        } else {
            null
        },
        trailingContent = {
            val idle = friend.timeSeconds?.let(Durations::short) ?: friend.time
            Text(stringResource(R.string.friends_idle, idle), style = MaterialTheme.typography.labelMedium)
        },
        modifier = Modifier.clickable(onClick = onOpenProfile),
    )
}
