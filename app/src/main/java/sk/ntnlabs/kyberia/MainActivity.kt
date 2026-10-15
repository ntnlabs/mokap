package sk.ntnlabs.kyberia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import sk.ntnlabs.kyberia.ui.home.HomeScreen
import sk.ntnlabs.kyberia.ui.login.LoginScreen
import sk.ntnlabs.kyberia.ui.login.LoginViewModel
import sk.ntnlabs.kyberia.ui.theme.MokapTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as MokapApp).container

        setContent {
            MokapTheme {
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
