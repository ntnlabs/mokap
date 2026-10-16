package sk.ntnlabs.kyberia

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import sk.ntnlabs.kyberia.ui.home.HomeScreen
import sk.ntnlabs.kyberia.ui.login.LoginScreen
import sk.ntnlabs.kyberia.ui.login.LoginViewModel
import sk.ntnlabs.kyberia.ui.theme.MokapTheme
import sk.ntnlabs.kyberia.ui.theme.isDarkTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as MokapApp).container

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
            MokapTheme(settings.theme) {
                Surface(Modifier.fillMaxSize()) {
                    val loggedIn by container.auth.loggedIn.collectAsStateWithLifecycle()
                    if (loggedIn) {
                        // Ak systém plán kontrol zrušil (napr. force stop), obnov ho.
                        LaunchedEffect(Unit) { container.scheduler.schedule() }
                        HomeScreen(container)
                    } else {
                        LoginScreen(viewModel(factory = LoginViewModel.factory(container.auth)))
                    }
                }
            }
        }
    }
}
