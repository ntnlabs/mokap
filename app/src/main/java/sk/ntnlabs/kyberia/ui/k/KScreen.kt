package sk.ntnlabs.kyberia.ui.k

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.UrlPolicy
import sk.ntnlabs.kyberia.data.Nsfw
import sk.ntnlabs.kyberia.parse.MyK
import sk.ntnlabs.kyberia.parse.TopKItem
import sk.ntnlabs.kyberia.ui.common.Avatar
import sk.ntnlabs.kyberia.ui.node.ContentView
import sk.ntnlabs.kyberia.ui.overview.OverviewViewModel

/**
 * K: tvoje príspevky s K za poslednú hodinu (z /id/19/, bez požiadavky navyše) a top K
 * (`/id/8340566/`, stiahne sa až pri prepnutí).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KScreen(
    overview: OverviewViewModel,
    topK: TopKViewModel,
    hideNsfw: Boolean,
    externalImages: ImageLoader?,
    onOpenNode: (Long) -> Unit,
    onOpenWeb: (String) -> Unit,
) {
    val mode = topK.state.mode
    // Pri otvorení záložky a pri prepnutí – top K len ak je staršie ako pár minút.
    LaunchedEffect(mode) { topK.refreshIfStale() }
    val openLink: (String) -> Unit = { url ->
        val id = if (UrlPolicy.isInternal(url)) KyberiaUrls.nodeId(url) else null
        if (id != null) onOpenNode(id) else onOpenWeb(url)
    }

    Column(Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            KMode.entries.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = item == mode,
                    onClick = { topK.select(item) },
                    shape = SegmentedButtonDefaults.itemShape(index, KMode.entries.size),
                ) { Text(stringResource(item.label), maxLines = 1) }
            }
        }
        val mine = mode == KMode.MINE
        PullToRefreshBox(
            isRefreshing = if (mine) overview.state.loading else topK.state.loading,
            onRefresh = { if (mine) overview.refresh() else topK.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            when (mode) {
                KMode.MINE -> MyKList(
                    overview.state.myK.filterNot { hideNsfw && Nsfw.matches(it.title) },
                    loaded = !overview.state.loading && overview.state.loadedAtMillis != null,
                    onOpenNode = onOpenNode,
                )
                KMode.TOP -> TopKList(
                    topK.state.items?.filterNot { hideNsfw && Nsfw.matches(it.title) },
                    error = topK.state.error,
                    externalImages = externalImages,
                    onLink = openLink,
                    onOpenNode = onOpenNode,
                    onOpenAll = { onOpenWeb(KyberiaUrls.TOP_K) },
                )
            }
        }
    }
}

private val KMode.label: Int
    get() = when (this) {
        KMode.MINE -> R.string.k_mode_mine
        KMode.TOP -> R.string.k_mode_top
    }

@Composable
private fun MyKList(items: List<MyK>, loaded: Boolean, onOpenNode: (Long) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                stringResource(R.string.k_mine_header),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp),
            )
        }
        if (items.isEmpty() && loaded) {
            item { Text(stringResource(R.string.k_mine_empty), Modifier.padding(horizontal = 16.dp)) }
        }
        items(items, key = { "myk:${it.nodeId}" }) { item ->
            ListItem(
                leadingContent = {
                    Text(
                        stringResource(R.string.k_added, item.added),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                },
                headlineContent = { Text(Nsfw.label(item.title), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                supportingContent = { item.total?.let { Text(stringResource(R.string.k_total, it)) } },
                modifier = Modifier.clickable { onOpenNode(item.nodeId) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun TopKList(
    items: List<TopKItem>?,
    error: Boolean,
    externalImages: ImageLoader?,
    onLink: (String) -> Unit,
    onOpenNode: (Long) -> Unit,
    onOpenAll: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        when {
            error -> item { Text(stringResource(R.string.k_top_error), Modifier.padding(16.dp)) }
            items == null -> Unit
            items.isEmpty() -> item { Text(stringResource(R.string.k_top_empty), Modifier.padding(16.dp)) }
        }
        items(items.orEmpty(), key = { "topk:${it.id}" }) { item ->
            TopKCard(item, externalImages, onLink, onOpenNode)
            HorizontalDivider()
        }
        if (items != null) {
            item {
                TextButton(onClick = onOpenAll, modifier = Modifier.padding(8.dp)) {
                    Text(stringResource(R.string.k_top_web))
                }
            }
        }
    }
}

@Composable
private fun TopKCard(item: TopKItem, externalImages: ImageLoader?, onLink: (String) -> Unit, onOpenNode: (Long) -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { onOpenNode(item.id) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Avatar(item.avatarUrl, size = 32.dp)
            Column(Modifier.weight(1f)) {
                item.author?.let { Text(it, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                item.created?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item.k?.let { Text(stringResource(R.string.node_k, it), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        }
        // Aj automatický názov („Re: …“) – s rodičom povie, kde sa príspevok nachádza.
        Text(
            Nsfw.label(item.title),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { onOpenNode(item.id) },
        )
        val parentId = item.parentId
        val parentTitle = item.parentTitle
        if (parentId != null && parentTitle != null) {
            Text(
                stringResource(R.string.node_in, Nsfw.label(parentTitle)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onOpenNode(parentId) },
            )
        }
        if (item.content.isNotEmpty()) ContentView(item.content, onLink, externalImages)
        item.children?.takeIf { it > 0 }?.let { count ->
            Text(
                pluralStringResource(R.plurals.node_replies, count, count),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onOpenNode(item.id) },
            )
        }
    }
}
