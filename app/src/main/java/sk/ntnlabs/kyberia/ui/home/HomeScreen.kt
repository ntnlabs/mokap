package sk.ntnlabs.kyberia.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import sk.ntnlabs.kyberia.AppContainer
import sk.ntnlabs.kyberia.KyberiaUrls
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.ui.bookmarks.BookmarksScreen
import sk.ntnlabs.kyberia.ui.bookmarks.BookmarksViewModel

private enum class Tab(@StringRes val label: Int, @DrawableRes val icon: Int) {
    BOOKMARKS(R.string.tab_bookmarks, R.drawable.ic_tab_bookmarks),
    MAIL(R.string.tab_mail, R.drawable.ic_tab_mail),
    K(R.string.tab_k, R.drawable.ic_tab_k),
    FRIENDS(R.string.tab_friends, R.drawable.ic_tab_friends),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(container: AppContainer) {
    var tab by rememberSaveable { mutableStateOf(Tab.BOOKMARKS) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val bookmarks: BookmarksViewModel = viewModel(factory = BookmarksViewModel.factory(container))
    val kWallet = bookmarks.state.user?.kWallet

    NotificationPermissionRequest()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(tab.label)) },
                actions = {
                    if (kWallet != null) {
                        Text("K $kWallet", color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = { scope.launch { container.auth.logout() } }) {
                        Text(stringResource(R.string.logout))
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
                        icon = { Icon(painterResource(item.icon), contentDescription = null) },
                        label = { Text(stringResource(item.label)) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                // Čítanie nodov v appke príde neskôr; dovtedy sa node otvorí na webe.
                Tab.BOOKMARKS -> BookmarksScreen(bookmarks, onOpen = { uriHandler.openUri(KyberiaUrls.node(it.id)) })
                Tab.MAIL, Tab.K, Tab.FRIENDS -> ComingSoon()
            }
        }
    }
}

@Composable
private fun ComingSoon() {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.coming_soon), textAlign = TextAlign.Center)
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
