package sk.ntnlabs.kyberia.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import sk.ntnlabs.kyberia.AppContainer
import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.ui.bookmarks.BookmarksScreen
import sk.ntnlabs.kyberia.ui.friends.FriendsScreen
import sk.ntnlabs.kyberia.ui.last.LastScreen
import sk.ntnlabs.kyberia.ui.overview.OverviewViewModel
import sk.ntnlabs.kyberia.ui.replies.RepliesScreen
import sk.ntnlabs.kyberia.ui.settings.SettingsScreen

private enum class Tab(@StringRes val label: Int, @DrawableRes val icon: Int) {
    BOOKMARKS(R.string.tab_bookmarks, R.drawable.ic_tab_bookmarks),
    REPLIES(R.string.tab_replies, R.drawable.ic_tab_replies),
    LAST(R.string.tab_last, R.drawable.ic_tab_last),
    MAIL(R.string.tab_mail, R.drawable.ic_tab_mail),
    K(R.string.tab_k, R.drawable.ic_tab_k),
    FRIENDS(R.string.tab_friends, R.drawable.ic_tab_friends),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(container: AppContainer, requestedTab: String?, onTabHandled: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Tab.BOOKMARKS) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val overview: OverviewViewModel = viewModel(factory = OverviewViewModel.factory(container))

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
    val kWallet = overview.state.user?.kWallet
    val tagline = overview.state.user?.tagline
    val friendsOnline = overview.state.friends.size
    // Čítanie nodov v appke príde neskôr; dovtedy sa node otvorí na webe.
    val openNode: (Long) -> Unit = { uriHandler.openUri(KyberiaUrls.node(it)) }

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
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == tab,
                        onClick = { tab = item },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (item == Tab.FRIENDS && friendsOnline > 0) Badge { Text("$friendsOnline") }
                                },
                            ) {
                                // Bez textu pod ikonou – záložiek je veľa; názov je v hornej lište a pre čítačku tu.
                                Icon(painterResource(item.icon), contentDescription = stringResource(item.label))
                            }
                        },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.BOOKMARKS -> BookmarksScreen(overview, container.settings, onOpenNode = openNode)
                Tab.REPLIES -> RepliesScreen(overview, onOpenNode = openNode)
                Tab.LAST -> LastScreen(overview, onOpenNode = openNode, onOpenAll = { uriHandler.openUri(KyberiaUrls.LAST) })
                Tab.FRIENDS -> FriendsScreen(overview, onOpenNode = openNode)
                Tab.MAIL -> OnWeb(R.string.mail_open) { uriHandler.openUri(KyberiaUrls.MAIL) }
                Tab.K -> OnWeb(R.string.k_open) { uriHandler.openUri(KyberiaUrls.TOP_K) }
            }
        }
    }
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
