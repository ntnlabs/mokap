package sk.ntnlabs.kyberia.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import sk.ntnlabs.kyberia.AppContainer
import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.data.RefreshTimer
import sk.ntnlabs.kyberia.data.SeenArea
import sk.ntnlabs.kyberia.data.Settings
import sk.ntnlabs.kyberia.data.SharedMusic
import sk.ntnlabs.kyberia.notify.Notifier
import sk.ntnlabs.kyberia.ui.bookmarks.BookmarksScreen
import sk.ntnlabs.kyberia.ui.common.Web
import sk.ntnlabs.kyberia.ui.friends.FriendsScreen
import sk.ntnlabs.kyberia.ui.friends.PeopleViewModel
import sk.ntnlabs.kyberia.ui.k.KHoursViewModel
import sk.ntnlabs.kyberia.ui.k.KMode
import sk.ntnlabs.kyberia.ui.k.KScreen
import sk.ntnlabs.kyberia.ui.k.TopKViewModel
import sk.ntnlabs.kyberia.ui.last.LastScreen
import sk.ntnlabs.kyberia.ui.mail.MailScreen
import sk.ntnlabs.kyberia.ui.mail.MailViewModel
import sk.ntnlabs.kyberia.ui.node.NodeScreen
import sk.ntnlabs.kyberia.ui.node.NodesViewModel
import sk.ntnlabs.kyberia.ui.overview.OverviewViewModel
import sk.ntnlabs.kyberia.ui.replies.RepliesScreen
import sk.ntnlabs.kyberia.ui.settings.SettingsScreen

private enum class Tab(@StringRes val label: Int, @DrawableRes val icon: Int, val area: SeenArea? = null) {
    BOOKMARKS(R.string.tab_bookmarks, R.drawable.ic_tab_bookmarks, SeenArea.BOOKMARKS),
    REPLIES(R.string.tab_replies, R.drawable.ic_tab_replies, SeenArea.REPLIES),
    LAST(R.string.tab_last, R.drawable.ic_tab_last, SeenArea.LAST),
    MAIL(R.string.tab_mail, R.drawable.ic_tab_mail),
    K(R.string.tab_k, R.drawable.ic_tab_k),
    FRIENDS(R.string.tab_friends, R.drawable.ic_tab_friends),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    requestedTab: String?,
    onTabHandled: () -> Unit,
    requestedNode: Long?,
    onNodeHandled: () -> Unit,
    requestedShare: SharedMusic? = null,
    onShareHandled: () -> Unit = {},
) {
    var tab by rememberSaveable { mutableStateOf(Tab.BOOKMARKS) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    // Otvorené nody nad záložkami; späť ide o jeden node naspäť.
    var nodeStack by rememberSaveable { mutableStateOf(longArrayOf()) }
    val nodes: NodesViewModel = viewModel(factory = NodesViewModel.factory(container))
    // Zatvorené nody netreba držať v pamäti.
    LaunchedEffect(nodeStack) { nodes.keepOnly(nodeStack.toSet()) }
    val context = LocalContext.current
    val openWeb: (String) -> Unit = { Web.open(context, it) }
    val overview: OverviewViewModel = viewModel(factory = OverviewViewModel.factory(container))
    val people: PeopleViewModel = viewModel(factory = PeopleViewModel.factory(container))
    val topK: TopKViewModel = viewModel(factory = TopKViewModel.factory(container))
    val kHours: KHoursViewModel = viewModel(factory = KHoursViewModel.factory(container))
    val mail: MailViewModel = viewModel(factory = MailViewModel.factory(container))
    // Počet novej pošty z kontroly (v pozadí aj pri obnovení appky) – číslo na záložke Pošta.
    val unreadMail by container.newsTracker.unreadMail.collectAsStateWithLifecycle()

    // Otvorené z notifikácie: prepni na správnu záložku a stiahni čerstvé dáta – vždy, aj bez obnovenia pri otvorení
    // (záložka mohla byť otvorená už predtým a bez toho by ukázala starý stav). Len to, o čom notifikácia je:
    // pošta poštu, ostatné /id/19/.
    LaunchedEffect(requestedTab) {
        val target = Tab.entries.firstOrNull { it.name == requestedTab } ?: return@LaunchedEffect
        tab = target
        settingsOpen = false
        nodeStack = longArrayOf()
        if (target == Tab.MAIL) mail.refresh() else overview.refresh()
        onTabHandled()
    }
    // Notifikácia odpovede, fóra alebo K: rovno ten node v appke, stiahnutý znova – aj keď bol otvorený už predtým.
    LaunchedEffect(requestedNode) {
        val node = requestedNode ?: return@LaunchedEffect
        settingsOpen = false
        nodeStack = longArrayOf(node)
        nodes.load(node)
        onNodeHandled()
    }
    // Zdieľaná skladba: fórum z nastavení (Ciele na kyberii) s predvyplneným príspevkom – odošle sa až po potvrdení.
    // Kým ho NodeScreen nevezme, drží sa tu spolu s cieľom (otvorený node sa medzitým môže zmeniť).
    var pendingShare by remember { mutableStateOf<Pair<Long, SharedMusic>?>(null) }
    LaunchedEffect(requestedShare) {
        val share = requestedShare ?: return@LaunchedEffect
        val target = container.settings.current.musicNodeId
        if (target == null) {
            Toast.makeText(context, R.string.share_no_target, Toast.LENGTH_LONG).show()
            nodeStack = longArrayOf()
            settingsOpen = true
        } else {
            settingsOpen = false
            nodeStack = longArrayOf(target)
            pendingShare = target to share
        }
        onShareHandled()
    }
    // Návrat z nodov (zavretý posledný): /id/19/ znova – čítanie fór mení počty nových, a to aj bez „obnoviť pri
    // otvorení“. Pošta nie – tú čítanie nodov nemení a stiahne sa sama pri otvorení.
    var wasInNodes by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(nodeStack.isEmpty()) {
        if (nodeStack.isNotEmpty()) {
            wasInNodes = true
        } else if (wasInNodes) {
            wasInNodes = false
            if (tab != Tab.MAIL) overview.refresh()
        }
    }
    // Záložka K alebo Odpovede priamo na obrazovke (nie node či nastavenia nad ňou): jej notifikácie sú videné.
    val onTabItself = nodeStack.isEmpty() && !settingsOpen
    val clearSeenNotifications = {
        if (onTabItself) {
            when (tab) {
                // Na Top K tvoje K nevidno – ich notifikácie ostanú.
                Tab.K -> if (topK.state.mode != KMode.TOP) Notifier.cancelK(context)
                Tab.REPLIES -> Notifier.cancelReplies(context)
                else -> Unit
            }
        }
    }
    // Pri každom návrate do appky obnov, ak je to zapnuté a dáta nie sú čerstvé.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        clearSeenNotifications()
        if (container.settings.current.refreshOnOpen) overview.refreshIfStale()
    }
    // Otvorená záložka je videná – aj keď na nej pribudnú dáta.
    LaunchedEffect(tab, overview.state.loadedAtMillis) { tab.area?.let(overview::markSeen) }
    LaunchedEffect(tab, onTabItself, overview.state.loadedAtMillis, topK.state.mode) { clearSeenNotifications() }
    // Záložka K sa pri každom otvorení začne tým, čo je v nastaveniach (Tvoje K / Top K).
    LaunchedEffect(tab) { if (tab != Tab.K) topK.resetToStart() }
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val changed = overview.state.changed
    val kWallet = overview.state.user?.kWallet
    val tagline = overview.state.user?.tagline
    val friendsOnline = overview.state.friends.size
    val openNode: (Long) -> Unit = { nodeStack += it }
    val externalImages = if (settings.showExternalMedia) container.externalImages else null

    NotificationPermissionRequest()

    if (nodeStack.isNotEmpty()) {
        val nodeId = nodeStack.last()
        // Lišta záložiek aj v nodoch – cesta „domov“ či rovno na inú záložku. Pri čítaní (rolovanie dole) sa schová,
        // pri rolovaní hore sa vysunie; pri každom novom node je vidieť.
        var tabBarShown by remember { mutableStateOf(true) }
        LaunchedEffect(nodeStack.size) { tabBarShown = true }
        val hideOnScroll = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < -SCROLL_SLOP) tabBarShown = false else if (available.y > SCROLL_SLOP) tabBarShown = true
                    return Offset.Zero
                }
            }
        }
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(1f)
                    // Spodný okraj systému rieši lišta, kým je vidieť – inak by bola pod nodom prázdna medzera.
                    .then(if (tabBarShown) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier)
                    .nestedScroll(hideOnScroll),
            ) {
                NodeScreen(
                    nodes,
                    nodeId,
                    externalImages = externalImages,
                    onBack = { nodeStack = nodeStack.copyOf(nodeStack.size - 1) },
                    onOpenNode = openNode,
                    share = pendingShare?.takeIf { it.first == nodeId }?.second,
                    onShareHandled = { pendingShare = null },
                )
            }
            AnimatedVisibility(visible = tabBarShown, enter = expandVertically(), exit = shrinkVertically()) {
                // Ťuknutie zavrie všetky nody a prepne záložku; tá, z ktorej si prišiel, je „domov“.
                TabBar(tab, friendsOnline, unreadMail, changed) { item ->
                    tab = item
                    nodeStack = longArrayOf()
                }
            }
        }
        return
    }

    if (settingsOpen) {
        SettingsScreen(container, onBack = { settingsOpen = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(tab.label))
                        // Text z titulku kyberie („bookmarks :: …“), ktorý sa strieda.
                        tagline?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    if (kWallet != null) {
                        Text("K $kWallet", color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { settingsOpen = true }) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
        bottomBar = {
            Column {
                RefreshBar(overview.state.loadedAtMillis, settings, onDue = overview::refresh)
                TabBar(tab, friendsOnline, unreadMail, changed) { tab = it }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.BOOKMARKS -> BookmarksScreen(overview, container.settings, onOpenNode = openNode)
                Tab.REPLIES -> RepliesScreen(overview, settings.hideNsfw, onOpenNode = openNode)
                Tab.LAST -> LastScreen(overview, settings.hideNsfw, onOpenNode = openNode, onOpenAll = { openWeb(KyberiaUrls.LAST) })
                Tab.FRIENDS -> FriendsScreen(overview, people, settings.hideNsfw, onOpenNode = openNode)
                Tab.MAIL -> MailScreen(mail, externalImages, onOpenNode = openNode, onOpenWeb = openWeb)
                Tab.K -> KScreen(overview, topK, kHours, settings.kHours, settings.hideNsfw, externalImages, onOpenNode = openNode, onOpenWeb = openWeb)
            }
        }
    }
}

/** Spodná lišta záložiek – na hlavnej obrazovke aj v nodoch. */
@Composable
private fun TabBar(selected: Tab, friendsOnline: Int, unreadMail: Int?, changed: Set<SeenArea>, onSelect: (Tab) -> Unit) {
    NavigationBar {
        Tab.entries.forEach { item ->
            NavigationBarItem(
                selected = item == selected,
                onClick = { onSelect(item) },
                icon = {
                    BadgedBox(
                        badge = {
                            when {
                                item == Tab.FRIENDS && friendsOnline > 0 -> Badge { Text("$friendsOnline") }
                                item == Tab.MAIL && (unreadMail ?: 0) > 0 -> Badge { Text("$unreadMail") }
                                // Bodka: od poslednej návštevy záložky tu pribudlo niečo nové.
                                item.area != null && item.area in changed -> Badge()
                            }
                        },
                    ) {
                        // Bez textu pod ikonou – záložiek je veľa; názov je v hornej lište a pre čítačku tu.
                        Icon(painterResource(item.icon), contentDescription = stringResource(item.label))
                    }
                },
            )
        }
    }
}

/**
 * Tenký zelený pruh nad záložkami: rastie zľava doprava do ďalšieho obnovenia podľa nastaveného
 * intervalu. Keď sa naplní a appka je na obrazovke, obnoví sa; ručné obnovenie aj kontrola
 * v pozadí ho vynulujú. V tichom režime sa neukazuje a appka sa sama neobnovuje.
 */
@Composable
private fun RefreshBar(loadedAtMillis: Long?, settings: Settings, onDue: () -> Unit) {
    val now = rememberNow()
    if (settings.isQuiet(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).hour)) return
    val interval = settings.intervalMinutes * 60_000L
    val due = RefreshTimer.isDue(loadedAtMillis, now, interval)
    // Pri chybe skúsi znova najskôr o minútu, nie každú sekundu.
    LaunchedEffect(due, now / RETRY_MILLIS) { if (due) onDue() }
    Box(Modifier.fillMaxWidth().height(4.dp)) {
        Box(
            Modifier
                .fillMaxWidth(RefreshTimer.progress(loadedAtMillis, now, interval))
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

private const val RETRY_MILLIS = 60_000L

/** Najmenší posun prsta (px), pri ktorom sa lišta v node schová či vysunie – nech sa netrasie pri drobnom pohybe. */
private const val SCROLL_SLOP = 6f

/** Aktuálny čas, ktorý tiká raz za sekundu – len kým je appka na obrazovke. */
@Composable
private fun rememberNow(periodMillis: Long = 1_000L): Long {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now by produceState(System.currentTimeMillis(), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = System.currentTimeMillis()
                delay(periodMillis)
            }
        }
    }
    return now
}

/** Android 13+ chce pre notifikácie výslovný súhlas. */
@Composable
private fun NotificationPermissionRequest() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
