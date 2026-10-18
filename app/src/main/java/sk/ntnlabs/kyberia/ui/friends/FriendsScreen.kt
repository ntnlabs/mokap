package sk.ntnlabs.kyberia.ui.friends

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.data.Durations
import sk.ntnlabs.kyberia.data.Nsfw
import sk.ntnlabs.kyberia.parse.Friend
import sk.ntnlabs.kyberia.parse.Hotspot
import sk.ntnlabs.kyberia.parse.Populated
import sk.ntnlabs.kyberia.ui.common.Avatar
import sk.ntnlabs.kyberia.ui.overview.OverviewViewModel

/** Priatelia online, všetci online (/id/27/) a kde sa práve diskutuje (/id/4830026/). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(overview: OverviewViewModel, people: PeopleViewModel, hideNsfw: Boolean, onOpenNode: (Long) -> Unit) {
    val mode = people.state.mode
    // Pri otvorení záložky a pri prepnutí – len ak sú dáta staršie ako minúta.
    LaunchedEffect(mode) { people.refreshIfStale() }

    Column(Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            PeopleMode.entries.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = item == mode,
                    onClick = { people.select(item) },
                    shape = SegmentedButtonDefaults.itemShape(index, PeopleMode.entries.size),
                ) { Text(stringResource(item.label), maxLines = 1) }
            }
        }
        val friendsMode = mode == PeopleMode.FRIENDS
        PullToRefreshBox(
            isRefreshing = if (friendsMode) overview.state.loading else people.state.loading,
            onRefresh = { if (friendsMode) overview.refresh() else people.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            when (mode) {
                PeopleMode.FRIENDS -> PeopleList(
                    overview.state.friends,
                    empty = if (overview.state.loading) null else R.string.friends_nobody,
                    onOpenNode = onOpenNode,
                )
                PeopleMode.EVERYONE -> PeopleList(
                    people.state.everyone.orEmpty(),
                    empty = when {
                        people.state.error -> R.string.people_error
                        people.state.loading || people.state.everyone == null -> null
                        else -> R.string.people_nobody
                    },
                    onOpenNode = onOpenNode,
                )
                PeopleMode.POPULATED -> PopulatedList(
                    people.state.populated?.let { it.copy(hotspots = Nsfw.hotspots(it.hotspots, hideNsfw)) },
                    error = people.state.error,
                    onOpenNode = onOpenNode,
                )
            }
        }
    }
}

private val PeopleMode.label: Int
    get() = when (this) {
        PeopleMode.FRIENDS -> R.string.people_mode_friends
        PeopleMode.EVERYONE -> R.string.people_mode_everyone
        PeopleMode.POPULATED -> R.string.people_mode_populated
    }

@Composable
private fun PeopleList(people: List<Friend>, empty: Int?, onOpenNode: (Long) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        if (people.isEmpty() && empty != null) {
            item { Text(stringResource(empty), Modifier.padding(16.dp)) }
        }
        items(people, key = { "person:${it.id}" }) { person ->
            FriendRow(
                friend = person,
                onOpenProfile = { onOpenNode(person.id) },
                onOpenLocation = { person.locationId?.let(onOpenNode) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun PopulatedList(populated: Populated?, error: Boolean, onOpenNode: (Long) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        when {
            error -> item { Text(stringResource(R.string.people_error), Modifier.padding(16.dp)) }
            populated == null -> Unit
            populated.hotspots.isEmpty() -> item { Text(stringResource(R.string.populated_empty), Modifier.padding(16.dp)) }
            else -> {
                item {
                    Text(
                        populated.online?.let { pluralStringResource(R.plurals.populated_header, it, it) }
                            ?: stringResource(R.string.populated_header_plain),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                    )
                }
                items(populated.hotspots, key = { "hot:${it.nodeId}" }) { hotspot ->
                    HotspotRow(hotspot, onClick = { onOpenNode(hotspot.nodeId) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun HotspotRow(hotspot: Hotspot, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(Nsfw.label(hotspot.title), maxLines = 2, overflow = TextOverflow.Ellipsis) },
        trailingContent = {
            Text(
                pluralStringResource(R.plurals.people_count, hotspot.people, hotspot.people),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun FriendRow(friend: Friend, onOpenProfile: () -> Unit, onOpenLocation: () -> Unit) {
    val location = friend.locationTitle?.let(Nsfw::label)
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
