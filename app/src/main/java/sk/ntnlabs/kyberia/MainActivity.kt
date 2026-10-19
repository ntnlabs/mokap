package sk.ntnlabs.kyberia

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import sk.ntnlabs.kyberia.ui.common.AvatarOptions
import sk.ntnlabs.kyberia.ui.common.LocalAvatarOptions
import sk.ntnlabs.kyberia.ui.common.rememberMeteredNetwork
import sk.ntnlabs.kyberia.ui.home.HomeScreen
import sk.ntnlabs.kyberia.ui.login.LoginScreen
import sk.ntnlabs.kyberia.ui.login.LoginViewModel
import sk.ntnlabs.kyberia.ui.theme.MokapTheme
import sk.ntnlabs.kyberia.ui.theme.isDarkTheme

class MainActivity : ComponentActivity() {

    /** Záložka, ktorú si vyžiadala notifikácia; HomeScreen ju prepne a obnoví dáta. */
    private val requestedTab = MutableStateFlow<String?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as MokapApp).container
        if (savedInstanceState == null) requestedTab.value = intent?.getStringExtra(EXTRA_TAB)

        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle()
            val dark = isDarkTheme(settings.theme)
            // Ikony v stavovej lište podľa zvolenej témy, nie len podľa systému.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose {}
            }
            val metered by rememberMeteredNetwork(enabled = settings.avatarsOnlyWifi)
            val avatars = AvatarOptions(
                animate = settings.animateAvatars,
                network = settings.avatarDownloadAllowed(metered),
            )
            MokapTheme(settings.theme) {
                CompositionLocalProvider(LocalAvatarOptions provides avatars) {
                    Surface(Modifier.fillMaxSize()) {
                        val loggedIn by container.auth.loggedIn.collectAsStateWithLifecycle()
                        val tab by requestedTab.collectAsStateWithLifecycle()
                        if (loggedIn) {
                            // Ak systém plán kontrol zrušil (napr. force stop), obnov ho.
                            LaunchedEffect(Unit) { container.scheduler.schedule() }
                            HomeScreen(container, requestedTab = tab, onTabHandled = { requestedTab.value = null })
                        } else {
                            LoginScreen(viewModel(factory = LoginViewModel.factory(container.auth, container.settings)))
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_TAB)?.let { requestedTab.value = it }
    }

    companion object {
        const val TAB_BOOKMARKS = "BOOKMARKS"
        const val TAB_REPLIES = "REPLIES"
        const val TAB_FRIENDS = "FRIENDS"
        private const val EXTRA_TAB = "sk.ntnlabs.kyberia.TAB"

        fun intent(context: Context, tab: String?): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .apply { if (tab != null) putExtra(EXTRA_TAB, tab) }
    }
}
