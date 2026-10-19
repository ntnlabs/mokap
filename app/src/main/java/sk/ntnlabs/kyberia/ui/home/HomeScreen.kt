package sk.ntnlabs.kyberia.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
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
import sk.ntnlabs.kyberia.ui.bookmarks.BookmarksScreen
import sk.ntnlabs.kyberia.ui.common.Web
import sk.ntnlabs.kyberia.ui.friends.FriendsScreen
import sk.ntnlabs.kyberia.ui.friends.PeopleViewModel
import sk.ntnlabs.kyberia.ui.last.LastScreen
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
fun HomeScreen(container: AppContainer, requestedTab: String?, onTabHandled: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Tab.BOOKMARKS) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val openWeb: (String) -> Unit = { Web.open(context, it) }
    val overview: OverviewViewModel = viewModel(factory = OverviewViewModel.factory(container))
    val people: PeopleViewModel = viewModel(factory = PeopleViewModel.factory(container))

    // Otvorené z notifikácie: prepni na správnu záložku a stiahni čerstvé dáta.
    LaunchedEffect(requestedTab) {
        val target = Tab.entries.firstOrNull { it.name == requestedTab } ?: return@LaunchedEffect
        tab = target
        settingsOpen = false
        overview.refresh()
        onTabHandled()
    }
    // Pri každom návrate do appky obnov, ak je to zapnuté a dáta nie sú čerstvé.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (container.settings.current.refreshOnOpen) overview.refreshIfStale()
    }
    // Otvorená záložka je videná – aj keď na nej pribudnú dáta.
    LaunchedEffect(tab, overview.state.loadedAtMillis) { tab.area?.let(overview::markSeen) }
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val changed = overview.state.changed
    val kWallet = overview.state.user?.kWallet
    val tagline = overview.state.user?.tagline
    val friendsOnline = overview.state.friends.size
    // Čítanie nodov v appke príde neskôr; dovtedy sa node otvorí na webe.
    val openNode: (Long) -> Unit = { openWeb(KyberiaUrls.node(it)) }

    NotificationPermissionRequest()

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
                NavigationBar {
                    Tab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = item == tab,
                            onClick = { tab = item },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        when {
                                            item == Tab.FRIENDS && friendsOnline > 0 -> Badge { Text("$friendsOnline") }
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
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.BOOKMARKS -> BookmarksScreen(overview, container.settings, onOpenNode = openNode)
                Tab.REPLIES -> RepliesScreen(overview, settings.hideNsfw, onOpenNode = openNode)
                Tab.LAST -> LastScreen(overview, settings.hideNsfw, onOpenNode = openNode, onOpenAll = { openWeb(KyberiaUrls.LAST) })
                Tab.FRIENDS -> FriendsScreen(overview, people, settings.hideNsfw, onOpenNode = openNode)
                Tab.MAIL -> OnWeb(R.string.mail_open) { openWeb(KyberiaUrls.MAIL) }
                Tab.K -> OnWeb(R.string.k_open) { openWeb(KyberiaUrls.TOP_K) }
            }
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

/** Kým appka nevie stránku zobraziť sama, aspoň ju jedným ťuknutím otvorí na webe. */
@Composable
private fun OnWeb(@StringRes button: Int, onOpen: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.work_in_progress), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onOpen) { Text(stringResource(button)) }
    }
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
