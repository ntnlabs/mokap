package sk.ntnlabs.kyberia.ui.node

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.UrlPolicy
import sk.ntnlabs.kyberia.data.Nsfw
import sk.ntnlabs.kyberia.data.ThreadItem
import sk.ntnlabs.kyberia.data.ThreadView
import sk.ntnlabs.kyberia.parse.NodeChild
import sk.ntnlabs.kyberia.parse.NodePage
import sk.ntnlabs.kyberia.ui.common.Avatar
import sk.ntnlabs.kyberia.ui.common.Web

/**
 * Node v appke: hlavička, obsah a prvá stránka odpovedí vo vlákne. Odkazy na nody kyberie
 * otvárajú ďalší node v appke, ostatné prehliadač; „na webe“ je vždy v hornej lište.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeScreen(
    nodes: NodesViewModel,
    nodeId: Long,
    externalImages: ImageLoader?,
    onBack: () -> Unit,
    onOpenNode: (Long) -> Unit,
) {
    LaunchedEffect(nodeId) { nodes.open(nodeId) }
    val state = nodes.state(nodeId)
    val context = LocalContext.current
    val openWeb: (String) -> Unit = { Web.open(context, it) }
    val openLink: (String) -> Unit = { url ->
        val id = if (UrlPolicy.isInternal(url)) KyberiaUrls.nodeId(url) else null
        if (id != null) onOpenNode(id) else openWeb(url)
    }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.page?.title?.let(Nsfw::label).orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = stringResource(R.string.settings_back))
                    }
                },
                actions = {
                    IconButton(onClick = { openWeb(KyberiaUrls.node(nodeId)) }) {
                        Icon(painterResource(R.drawable.ic_open_web), contentDescription = stringResource(R.string.node_open_web))
                    }
                },
            )
        },
    ) { padding ->
        val page = state.page
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                page != null -> PullToRefreshBox(isRefreshing = state.loading, onRefresh = { nodes.load(nodeId) }) {
                    NodeContent(page, openLink, externalImages, onOpenNode, onMore = { openWeb(KyberiaUrls.node(nodeId)) })
                }
                state.error -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.node_error), textAlign = TextAlign.Center)
                    Button(onClick = { nodes.load(nodeId) }) { Text(stringResource(R.string.node_retry)) }
                }
                else -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun NodeContent(
    page: NodePage,
    onLink: (String) -> Unit,
    externalImages: ImageLoader?,
    onOpenNode: (Long) -> Unit,
    onMore: () -> Unit,
) {
    // Do štyroch úrovní; hlbšie odpovede sa otvoria ako vetva (node odpovede) v appke.
    val thread = remember(page.children) { ThreadView.items(page.children) }
    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "header") { Header(page, onOpenNode) }
        if (page.content.isNotEmpty()) {
            item(key = "content") { ContentView(page.content, onLink, externalImages, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
        }
        item(key = "replies") {
            HorizontalDivider(Modifier.padding(top = 8.dp))
            Text(
                pluralStringResource(R.plurals.node_replies, page.childrenTotal ?: page.children.size, page.childrenTotal ?: page.children.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp),
            )
        }
        items(thread, key = { it.key() }) { item ->
            when (item) {
                is ThreadItem.Reply -> Child(item.child, onLink, externalImages, onOpen = { onOpenNode(item.child.id) })
                is ThreadItem.More -> MoreReplies(item, onOpen = { onOpenNode(item.parentId) })
            }
            HorizontalDivider()
        }
        // Kyberia odpovede stránkuje; ďalšie zatiaľ na webe.
        if ((page.childrenTotal ?: 0) > page.children.count { it.depth == 0 }) {
            item(key = "more") {
                TextButton(onClick = onMore, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(stringResource(R.string.node_more_on_web))
                }
            }
        }
    }
}

@Composable
private fun Header(page: NodePage, onOpenNode: (Long) -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(Nsfw.label(page.title), style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            page.ownerId?.let { owner ->
                Avatar(KyberiaUrls.avatar(owner), size = 32.dp, modifier = Modifier.clickable { onOpenNode(owner) })
            }
            Column(Modifier.weight(1f)) {
                page.owner?.let { Text(it, fontWeight = FontWeight.Bold) }
                page.created?.let { Muted(it) }
            }
            page.k?.let { Text(stringResource(R.string.node_k, it), color = MaterialTheme.colorScheme.primary) }
        }
        val parentId = page.parentId
        val parentTitle = page.parentTitle
        if (parentId != null && parentTitle != null) {
            Text(
                stringResource(R.string.node_in, Nsfw.label(parentTitle)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onOpenNode(parentId) },
            )
        }
    }
}

/** Odpoveď odsadená podľa hĺbky vo vlákne ([ThreadView] ju obmedzí na štyri úrovne). */
@Composable
private fun Child(child: NodeChild, onLink: (String) -> Unit, externalImages: ImageLoader?, onOpen: () -> Unit) {
    val indent = indent(child.depth)
    Column(
        Modifier.padding(start = 16.dp + indent, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Avatar(child.avatarUrl, size = 28.dp)
            Column(Modifier.weight(1f)) {
                Text(child.author, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                child.created?.let { Muted(it) }
            }
            if (child.isNew) Text(stringResource(R.string.node_new), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            child.k?.let { Text(stringResource(R.string.node_k, it), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium) }
        }
        // Aj automatický názov („Re[2]: …“, dátum) – podľa neho sa vo vlákne ľahšie orientuje.
        if (child.title.isNotBlank()) {
            Text(Nsfw.label(child.title), style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        ContentView(child.content, onLink, externalImages)
    }
}

/** „↳ N ďalších odpovedí“ – hlbšia časť vlákna, otvorí sa ako node odpovede nad ňou. */
@Composable
private fun MoreReplies(item: ThreadItem.More, onOpen: () -> Unit) {
    Text(
        pluralStringResource(R.plurals.node_more_replies, item.count, item.count),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 16.dp + indent(item.depth), end = 16.dp, top = 12.dp, bottom = 12.dp),
    )
}

private fun indent(depth: Int) = (depth * INDENT_DP).dp

private fun ThreadItem.key(): String = when (this) {
    is ThreadItem.Reply -> "r${child.id}"
    is ThreadItem.More -> "m$parentId"
}

@Composable
private fun Muted(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private const val INDENT_DP = 14
