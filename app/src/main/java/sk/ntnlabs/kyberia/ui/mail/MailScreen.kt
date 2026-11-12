package sk.ntnlabs.kyberia.ui.mail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import coil3.ImageLoader
import kotlinx.coroutines.launch
import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.UrlPolicy
import sk.ntnlabs.kyberia.parse.Mail
import sk.ntnlabs.kyberia.parse.MailPage
import sk.ntnlabs.kyberia.ui.common.Avatar
import sk.ntnlabs.kyberia.ui.node.ContentView
import sk.ntnlabs.kyberia.ui.node.ReplyProblem
import sk.ntnlabs.kyberia.ui.node.TextSheet

/**
 * Pošta v appke: prvá stránka (32 správ, najnovšie hore), odpoveď druhej strane a nová správa. Stiahne sa pri
 * otvorení záložky – kyberia ju tým označí ako prečítanú, ako na webe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailScreen(
    viewModel: MailViewModel,
    externalImages: ImageLoader?,
    onOpenNode: (Long) -> Unit,
    onOpenWeb: (String) -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.refreshIfStale() }
    val state = viewModel.state
    val page = state.page
    val openLink: (String) -> Unit = { url ->
        val id = if (UrlPolicy.isInternal(url)) KyberiaUrls.nodeId(url) else null
        if (id != null) onOpenNode(id) else onOpenWeb(url)
    }

    // Editor: adresát a text prežijú otočenie aj chybu; zahodia sa len po potvrdenom odoslaní.
    var composing by rememberSaveable { mutableStateOf(false) }
    var to by rememberSaveable { mutableStateOf("") }
    var byId by rememberSaveable { mutableStateOf(false) }
    /** Odpoveď: komu (nick), adresát je pevný a ide na ID. Null = nová správa, adresát sa píše. */
    var replyName by rememberSaveable { mutableStateOf<String?>(null) }
    var draft by rememberSaveable { mutableStateOf("") }
    var noHtml by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    LaunchedEffect(state.sent) {
        if (state.sent) {
            composing = false
            draft = ""
            viewModel.sendHandled()
            val text = resources.getString(R.string.mail_sent)
            scope.launch { snackbar.showSnackbar(text) }
        }
    }
    val reply: (Mail) -> Unit = { mail ->
        val (id, name) = requireNotNull(page).counterpart(mail)
        if (id != null) {
            to = id.toString()
            byId = true
        } else {
            to = name
            byId = false
        }
        replyName = name
        composing = true
    }

    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(isRefreshing = state.loading, onRefresh = viewModel::refresh, modifier = Modifier.fillMaxSize()) {
            when {
                page != null -> MailList(page, openLink, externalImages, reply, onOpenAll = { onOpenWeb(KyberiaUrls.MAIL) })
                state.error -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.mail_error), textAlign = TextAlign.Center)
                    Button(onClick = viewModel::refresh) { Text(stringResource(R.string.node_retry)) }
                }
                else -> Box(Modifier.fillMaxSize())
            }
        }
        if (page?.form != null && !composing) {
            ExtendedFloatingActionButton(
                onClick = {
                    to = ""
                    byId = false
                    replyName = null
                    composing = true
                },
                icon = { Icon(painterResource(R.drawable.ic_tab_mail), contentDescription = null) },
                text = { Text(stringResource(R.string.mail_new)) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (composing && page?.form != null) {
        val name = replyName
        TextSheet(
            header = if (name != null) stringResource(R.string.mail_reply_to, name) else stringResource(R.string.mail_new),
            draft = draft,
            onDraft = { draft = it },
            busy = state.sending,
            message = state.problem?.let {
                when (it) {
                    is ReplyProblem.Rejected -> stringResource(R.string.mail_rejected, it.message)
                    ReplyProblem.Unconfirmed -> stringResource(R.string.mail_unconfirmed)
                    ReplyProblem.Network -> stringResource(R.string.mail_network)
                }
            },
            hint = stringResource(R.string.node_reply_hint),
            confirmLabel = stringResource(R.string.node_reply_send),
            onConfirm = { viewModel.send(to, byId, draft, noHtml) },
            confirmResend = state.problem == ReplyProblem.Unconfirmed || state.problem == ReplyProblem.Network,
            onDismiss = {
                composing = false
                viewModel.sendHandled()
            },
            noHtml = noHtml,
            onNoHtml = { noHtml = it },
            confirmEnabled = to.isNotBlank(),
            // Pri odpovedi je adresát pevný (ID druhej strany), pri novej správe sa píše.
            extra = { if (name == null) Recipient(to, byId, enabled = !state.sending, onTo = { to = it }, onById = { byId = it }) },
        )
    }
}

/** Adresát novej správy – nick alebo číslo profilu (niektoré nicky sa napísať nedajú, ako pri prihlásení). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Recipient(to: String, byId: Boolean, enabled: Boolean, onTo: (String) -> Unit, onById: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(false, true).forEachIndexed { index, id ->
                SegmentedButton(
                    selected = byId == id,
                    onClick = { onById(id) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                ) { Text(stringResource(if (id) R.string.login_by_id else R.string.login_by_nick)) }
            }
        }
        OutlinedTextField(
            value = to,
            onValueChange = { value -> onTo(if (byId) value.filter(Char::isDigit) else value) },
            enabled = enabled,
            singleLine = true,
            label = { Text(stringResource(if (byId) R.string.mail_to_id else R.string.mail_to_nick)) },
            keyboardOptions = KeyboardOptions(keyboardType = if (byId) KeyboardType.Number else KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MailList(
    page: MailPage,
    onLink: (String) -> Unit,
    externalImages: ImageLoader?,
    onReply: (Mail) -> Unit,
    onOpenAll: () -> Unit,
) {
    // Miesto pod tlačidlom „Nová správa“, aby neprekrylo poslednú správu.
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
        if (page.mails.isEmpty()) {
            item { Text(stringResource(R.string.mail_empty), Modifier.padding(16.dp)) }
        }
        items(page.mails, key = { "mail:${it.id}" }) { mail ->
            MailRow(mail, page.outgoing(mail), onLink, externalImages, onReply = if (page.form != null) ({ onReply(mail) }) else null)
            HorizontalDivider()
        }
        item {
            TextButton(onClick = onOpenAll, modifier = Modifier.padding(8.dp)) { Text(stringResource(R.string.mail_more_on_web)) }
        }
    }
}

/** Správa: moje odoslané sú jemne podfarbené, aby sa dali rozoznať od prijatých. */
@Composable
private fun MailRow(mail: Mail, outgoing: Boolean, onLink: (String) -> Unit, externalImages: ImageLoader?, onReply: (() -> Unit)?) {
    val background = if (outgoing) Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) else Modifier
    Column(
        background.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Avatar(mail.avatarUrl, size = 32.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    if (outgoing) stringResource(R.string.mail_to_someone, mail.to) else mail.from,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                mail.time?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            when {
                mail.isNew -> Text(stringResource(R.string.mail_new_badge), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                mail.unreadByRecipient -> Text(
                    stringResource(R.string.mail_unread_badge),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        ContentView(mail.content, onLink, externalImages)
        if (onReply != null) {
            TextButton(onClick = onReply, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.mail_reply), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
