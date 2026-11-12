package sk.ntnlabs.kyberia.ui.node

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import kotlinx.coroutines.launch
import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.UrlPolicy
import sk.ntnlabs.kyberia.data.Nsfw
import sk.ntnlabs.kyberia.data.ThreadItem
import sk.ntnlabs.kyberia.data.ThreadView
import sk.ntnlabs.kyberia.parse.ContentBlock
import sk.ntnlabs.kyberia.parse.NodeChild
import sk.ntnlabs.kyberia.parse.NodePage
import sk.ntnlabs.kyberia.ui.common.Avatar
import sk.ntnlabs.kyberia.ui.common.Web

/**
 * Node v appke: téma (samotný node) v karte, pod ňou diskusia – prvá stránka odpovedí vo vlákne. Odkazy na nody
 * kyberie otvárajú ďalší node v appke, ostatné prehliadač; „na webe“ je vždy v hornej lište. Akcie (odpoveď, K,
 * úprava vlastného) appka ponúkne len tam, kde ich ponúka aj kyberia.
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

    // Editor odpovede: text prežije otočenie aj chybu; zahodí sa len po úspešnom odoslaní.
    var composing by rememberSaveable(nodeId) { mutableStateOf(false) }
    var replyTo by rememberSaveable(nodeId) { mutableStateOf<Long?>(null) }
    var draft by rememberSaveable(nodeId) { mutableStateOf("") }
    var noHtml by rememberSaveable(nodeId) { mutableStateOf(false) }
    // Komu dať K (po potvrdení): nodeId = samotný node, inak ID odpovede.
    var askK by rememberSaveable(nodeId) { mutableStateOf<Long?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Texty pre snackbar sa skladajú až po akcii – cez LocalResources, nie Context (zmena konfigurácie).
    val resources = LocalResources.current
    LaunchedEffect(state.replySent) {
        if (state.replySent) {
            composing = false
            draft = ""
            replyTo = null
            nodes.replyHandled(nodeId)
            val text = resources.getString(R.string.node_reply_sent)
            scope.launch { snackbar.showSnackbar(text) }
        }
    }
    LaunchedEffect(state.notice) {
        val notice = state.notice ?: return@LaunchedEffect
        val text = when (notice) {
            NodeNotice.KGiven -> resources.getString(R.string.node_k_given)
            is NodeNotice.KRejected -> resources.getString(R.string.node_k_rejected, notice.message)
            NodeNotice.KUnconfirmed -> resources.getString(R.string.node_k_unconfirmed)
            NodeNotice.KRemoved -> resources.getString(R.string.node_k_removed)
            NodeNotice.KAlready -> resources.getString(R.string.node_k_already)
            NodeNotice.Network -> resources.getString(R.string.node_action_network)
            NodeNotice.Edited -> resources.getString(R.string.node_edit_saved)
        }
        nodes.noticeHandled(nodeId)
        scope.launch { snackbar.showSnackbar(text) }
    }

    val me = nodes.me
    val page = state.page
    val actions = page?.let {
        NodeActions(
            reply = if (it.replyForm != null) {
                { target: Long? ->
                    replyTo = target
                    composing = true
                }
            } else {
                null
            },
            kNode = if (it.kNode != null && !nodes.kGiven(nodeId)) ({ askK = nodeId }) else null,
            kChild = if (it.kChild != null) ({ child: NodeChild -> askK = child.id }) else null,
            kGiven = nodes::kGiven,
            edit = { target -> nodes.startEdit(nodeId, target) },
            me = me,
            enabled = !state.sending,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(page?.title?.let(Nsfw::label).orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = stringResource(R.string.settings_back))
                    }
                },
                actions = {
                    // Jediná cesta na rodiča (napr. z odpovede otvorenej zo záložky Odpovede) – zásobník ho nemusí mať.
                    // Zelená, aby bola vidieť ako hlavný navigačný prvok; názov rodiča len pre čítačku.
                    page?.parentId?.let { parent ->
                        IconButton(onClick = { onOpenNode(parent) }) {
                            Icon(
                                painterResource(R.drawable.ic_arrow_upward),
                                contentDescription = page.parentTitle?.let { stringResource(R.string.node_parent_open_named, Nsfw.label(it)) }
                                    ?: stringResource(R.string.node_parent_open),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    IconButton(onClick = { openWeb(KyberiaUrls.node(nodeId)) }) {
                        Icon(painterResource(R.drawable.ic_open_web), contentDescription = stringResource(R.string.node_open_web))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            val reply = actions?.reply
            if (reply != null && !composing && state.edit == null) {
                ExtendedFloatingActionButton(
                    onClick = { reply(null) },
                    icon = { Icon(painterResource(R.drawable.ic_reply), contentDescription = null) },
                    text = { Text(stringResource(R.string.node_reply)) },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                page != null && actions != null -> PullToRefreshBox(isRefreshing = state.loading, onRefresh = { nodes.load(nodeId) }) {
                    NodeContent(
                        page,
                        openLink,
                        externalImages,
                        onOpenNode,
                        onMore = { openWeb(KyberiaUrls.node(nodeId)) },
                        actions,
                        newReply = state.newReply,
                        onReplyShown = { nodes.replyShown(nodeId) },
                    )
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

    if (page == null) return

    // K sa nedá vziať späť (odobratie je na kyberii penalizované) – preto vždy potvrdenie.
    askK?.let { target ->
        val child = page.children.firstOrNull { it.id == target }
        val what = when {
            target == nodeId -> Nsfw.label(page.title)
            child != null -> "${child.author}: ${Nsfw.label(child.title)}"
            else -> null
        }
        if (what != null) {
            AlertDialog(
                onDismissRequest = { askK = null },
                title = { Text(stringResource(R.string.node_k_confirm_title)) },
                text = { Text(stringResource(R.string.node_k_confirm_text, what)) },
                confirmButton = {
                    TextButton(onClick = {
                        askK = null
                        nodes.giveK(nodeId, if (target == nodeId) null else target)
                    }) { Text(stringResource(R.string.node_k_confirm)) }
                },
                dismissButton = { TextButton(onClick = { askK = null }) { Text(stringResource(R.string.cancel)) } },
            )
        }
    }

    if (composing && page.replyForm != null) {
        val target = replyTo?.let { id -> page.children.firstOrNull { it.id == id } }
        TextSheet(
            header = when {
                target != null -> stringResource(R.string.node_reply_to_child, target.author, Nsfw.label(target.title))
                replyTo != null -> stringResource(R.string.node_reply_to_reply)
                else -> stringResource(R.string.node_reply_to_node, Nsfw.label(page.title))
            },
            draft = draft,
            onDraft = { draft = it },
            busy = state.sending,
            message = state.replyProblem?.let {
                when (it) {
                    is ReplyProblem.Rejected -> stringResource(R.string.node_reply_rejected, it.message)
                    ReplyProblem.Unconfirmed -> stringResource(R.string.node_reply_unconfirmed)
                    ReplyProblem.Network -> stringResource(R.string.node_reply_network)
                }
            },
            hint = stringResource(R.string.node_reply_hint),
            confirmLabel = stringResource(R.string.node_reply_send),
            onConfirm = { nodes.reply(nodeId, draft, replyTo, noHtml) },
            confirmResend = state.replyProblem == ReplyProblem.Unconfirmed || state.replyProblem == ReplyProblem.Network,
            onDismiss = {
                composing = false
                nodes.replyHandled(nodeId)
            },
            noHtml = noHtml,
            onNoHtml = { noHtml = it },
        )
    }

    state.edit?.let { edit ->
        val title = if (edit.target == nodeId) page.title else page.children.firstOrNull { it.id == edit.target }?.title
        // Po načítaní pôvodného textu sa editor naplní ním; potom prežije otočenie aj chybu.
        var editDraft by rememberSaveable(nodeId, edit.target, edit.form != null) { mutableStateOf(edit.form?.content.orEmpty()) }
        TextSheet(
            header = stringResource(R.string.node_edit_title, Nsfw.label(title.orEmpty())),
            draft = editDraft,
            onDraft = { editDraft = it },
            busy = state.sending,
            loading = edit.loading,
            showEditor = edit.form != null,
            message = when {
                edit.unavailable -> stringResource(R.string.node_edit_unavailable)
                edit.loadFailed -> stringResource(R.string.node_edit_load_error)
                else -> edit.problem?.let {
                    when (it) {
                        is ReplyProblem.Rejected -> stringResource(R.string.node_edit_rejected, it.message)
                        else -> stringResource(R.string.node_edit_network)
                    }
                }
            },
            hint = stringResource(R.string.node_edit_hint),
            confirmLabel = stringResource(R.string.node_edit_save),
            onConfirm = if (edit.form != null) ({ nodes.saveEdit(nodeId, editDraft) }) else null,
            onDismiss = { nodes.closeEdit(nodeId) },
        )
    }
}

/** Čo sa dá s nodom a odpoveďami robiť; null = kyberia to tu neponúka. */
private class NodeActions(
    /** Otvorí editor odpovede: null = do nodu, ID = na tú odpoveď. */
    val reply: ((Long?) -> Unit)?,
    val kNode: (() -> Unit)?,
    val kChild: ((NodeChild) -> Unit)?,
    /** Už dané K v tejto relácii – znova sa neponúkne (druhé stlačenie by ho na kyberii odobralo). */
    val kGiven: (Long) -> Boolean,
    /** Úprava vlastného príspevku (node alebo odpoveď). */
    val edit: (Long) -> Unit,
    val me: Long?,
    /** Počas odosielania inej akcie sú tlačidlá vypnuté. */
    val enabled: Boolean,
)

/**
 * Editor v spodnom paneli – odpoveď aj úprava. Kyberia berie text ako na webe: HTML (`<b>`, `<a href>`) a nové
 * riadky; „Bez HTML“ ([noHtml], len pri odpovedi) = jej vlastné „NO HTML“. Počas odosielania sa nedá zavrieť
 * ani odoslať znova.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TextSheet(
    header: String,
    draft: String,
    onDraft: (String) -> Unit,
    busy: Boolean,
    message: String?,
    hint: String,
    confirmLabel: String,
    /** null = nie je čo odoslať (text sa ešte načítava alebo úprava nie je možná). */
    onConfirm: (() -> Unit)?,
    onDismiss: () -> Unit,
    loading: Boolean = false,
    showEditor: Boolean = true,
    noHtml: Boolean? = null,
    onNoHtml: (Boolean) -> Unit = {},
    /** Polia nad textom (napr. adresát pošty). */
    extra: (@Composable () -> Unit)? = null,
    /** Okrem textu musí byť vyplnené aj niečo iné (napr. adresát). */
    confirmEnabled: Boolean = true,
    /**
     * Predošlé odoslanie ostalo nepotvrdené (alebo spadlo spojenie) – text možno už odišiel. Ďalšie „Odoslať“ sa
     * najprv opýta, aby nevznikli duplikáty (tester takto poslal poštu 4×).
     */
    confirmResend: Boolean = false,
) {
    val currentBusy by rememberUpdatedState(busy)
    var askResend by remember { mutableStateOf(false) }
    if (askResend && onConfirm != null) {
        AlertDialog(
            onDismissRequest = { askResend = false },
            title = { Text(stringResource(R.string.resend_title)) },
            text = { Text(stringResource(R.string.resend_text)) },
            confirmButton = {
                TextButton(onClick = {
                    askResend = false
                    onConfirm()
                }) { Text(stringResource(R.string.resend_confirm)) }
            },
            dismissButton = { TextButton(onClick = { askResend = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !currentBusy },
    )
    val focus = remember { FocusRequester() }
    LaunchedEffect(showEditor) { if (showEditor) runCatching { focus.requestFocus() } }
    ModalBottomSheet(onDismissRequest = { if (!currentBusy) onDismiss() }, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(header, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            extra?.invoke()
            if (loading) {
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
            }
            if (showEditor) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraft,
                    enabled = !busy,
                    minLines = 5,
                    maxLines = 12,
                    label = { Text(stringResource(R.string.node_reply_text)) },
                    supportingText = { Text(hint) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
            message?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (noHtml != null && showEditor) {
                        Row(
                            Modifier.clickable(enabled = !busy) { onNoHtml(!noHtml) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = noHtml, onCheckedChange = null, enabled = !busy)
                            Text(stringResource(R.string.node_reply_no_html), Modifier.padding(start = 4.dp))
                        }
                    }
                }
                TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.cancel)) }
                if (onConfirm != null) {
                    Button(
                        onClick = { if (confirmResend) askResend = true else onConfirm() },
                        enabled = !busy && draft.isNotBlank() && confirmEnabled,
                    ) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(confirmLabel)
                        }
                    }
                }
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
    actions: NodeActions,
    newReply: Long?,
    onReplyShown: () -> Unit,
) {
    // Do štyroch úrovní; hlbšie odpovede sa otvoria ako vetva (node odpovede) v appke.
    val thread = remember(page.children) { ThreadView.items(page.children) }
    val replies = page.childrenTotal ?: page.children.size
    // Po odoslaní pre istotu odrolovať na novú odpoveď (zoznam drží pozíciu na prvej viditeľnej položke – v pošte
    // tak nová správa ostala nad okrajom). Ak je hlbšie ako vlákno ukazuje, ostane sa na mieste (potvrdí ju snackbar).
    val listState = rememberLazyListState()
    LaunchedEffect(newReply) {
        val id = newReply ?: return@LaunchedEffect
        val index = thread.indexOfFirst { it is ThreadItem.Reply && it.child.id == id }
        if (index >= 0) listState.animateScrollToItem(index + HEADER_ITEMS)
        onReplyShown()
    }
    // Miesto pod tlačidlom „Odpovedať“, aby neprekrylo poslednú odpoveď.
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = if (actions.reply != null) 88.dp else 8.dp)) {
        item(key = "topic") { Topic(page, onLink, externalImages, onOpenNode, actions) }
        item(key = "discussion") {
            Text(
                pluralStringResource(R.plurals.node_discussion, replies, replies),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            )
            HorizontalDivider()
        }
        items(thread, key = { it.key() }) { item ->
            when (item) {
                is ThreadItem.Reply -> Child(item.child, onLink, externalImages, onOpen = { onOpenNode(item.child.id) }, actions)
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

/**
 * Samotný node – téma fóra, článok či príspevok, na ktorý sa odpovedá. V karte s orámovaním (ako `#topic` na webe),
 * aby sa nepliedol s diskusiou pod ním.
 */
@Composable
private fun Topic(page: NodePage, onLink: (String) -> Unit, externalImages: ImageLoader?, onOpenNode: (Long) -> Unit, actions: NodeActions) {
    OutlinedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            typeLabel(page.templateId)?.let {
                Text(stringResource(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
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
            if (page.content.isNotEmpty()) TopicContent(page, onLink, externalImages)
            val own = actions.me != null && page.ownerId == actions.me
            val kNode = actions.kNode
            val kDone = page.kGiven || actions.kGiven(page.id)
            if (kNode != null || own || kDone) {
                Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    if (kDone) {
                        Text(
                            stringResource(R.string.node_k_done),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    } else if (kNode != null) {
                        Action(R.string.node_k_give, actions.enabled, kNode)
                    }
                    if (own) Action(R.string.node_edit, actions.enabled) { actions.edit(page.id) }
                }
            }
        }
    }
}

/**
 * Obsah témy. Dlhý úvodník fóra či nodeshellu (nie príspevok v diskusii – ten sa číta celý) sa zbalí na pár riadkov
 * s „Zobraziť celý úvod“ – niektoré majú aj dve strany a diskusia pod nimi by sa strácala.
 */
@Composable
private fun TopicContent(page: NodePage, onLink: (String) -> Unit, externalImages: ImageLoader?) {
    val collapsible = remember(page.content, page.templateId) { page.templateId != SUBMISSION && isLong(page.content) }
    var expanded by rememberSaveable(page.id) { mutableStateOf(false) }
    val collapsed = collapsible && !expanded
    Box(if (collapsed) Modifier.heightIn(max = COLLAPSED_HEIGHT.dp).clipToBounds() else Modifier) {
        ContentView(page.content, onLink, externalImages)
        if (collapsed) {
            val fade = MaterialTheme.colorScheme.surface
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Brush.verticalGradient(listOf(fade.copy(alpha = 0f), fade))),
            )
        }
    }
    if (collapsible) {
        TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(horizontal = 0.dp)) {
            Text(stringResource(if (expanded) R.string.node_intro_less else R.string.node_intro_more))
        }
    }
}

/** Hrubý odhad dĺžky bez merania: znaky textu, obrázok či video ako ~300 znakov. */
private fun isLong(blocks: List<ContentBlock>): Boolean {
    val size = blocks.sumOf { block ->
        when (block) {
            is ContentBlock.Text -> block.spans.sumOf { it.text.length }
            else -> MEDIA_CHARS
        }
    }
    return size > LONG_CHARS
}

/** Odpoveď odsadená podľa hĺbky vo vlákne ([ThreadView] ju obmedzí na štyri úrovne). */
@Composable
private fun Child(child: NodeChild, onLink: (String) -> Unit, externalImages: ImageLoader?, onOpen: () -> Unit, actions: NodeActions) {
    val indent = indent(child.depth)
    Column(
        Modifier.padding(start = 16.dp + indent, end = 16.dp, top = 12.dp, bottom = 4.dp),
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
        val own = actions.me != null && child.authorId == actions.me
        val kChild = actions.kChild?.takeUnless { actions.kGiven(child.id) }
        val reply = actions.reply
        if (kChild != null || own || reply != null) {
            Row(Modifier.align(Alignment.End)) {
                if (kChild != null) Action(R.string.node_k_give, actions.enabled) { kChild(child) }
                if (own) Action(R.string.node_edit, actions.enabled) { actions.edit(child.id) }
                if (reply != null) Action(R.string.node_reply_short, actions.enabled) { reply(child.id) }
            }
        } else {
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Action(@StringRes label: Int, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, contentPadding = PaddingValues(horizontal = 8.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
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

/** Druh nodu podľa šablóny – kyberia rozlišuje „node“ (fórum, článok…) a „submission“ (príspevok v diskusii). */
@StringRes
private fun typeLabel(templateId: Int?): Int? = when (templateId) {
    2 -> R.string.node_type_nodeshell
    3 -> R.string.node_type_forum
    4 -> R.string.node_type_submission
    5 -> R.string.node_type_article
    else -> null
}

private fun indent(depth: Int) = (depth * INDENT_DP).dp

/** Položky zoznamu pred vláknom: téma a nadpis „Diskusia“. */
private const val HEADER_ITEMS = 2

private fun ThreadItem.key(): String = when (this) {
    is ThreadItem.Reply -> "r${child.id}"
    is ThreadItem.More -> "m$parentId"
}

@Composable
private fun Muted(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private const val INDENT_DP = 14
private const val SUBMISSION = 4
private const val LONG_CHARS = 900
private const val MEDIA_CHARS = 300
private const val COLLAPSED_HEIGHT = 220
