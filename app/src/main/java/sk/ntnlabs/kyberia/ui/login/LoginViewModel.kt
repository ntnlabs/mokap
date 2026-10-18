package sk.ntnlabs.kyberia.ui.login

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.io.IOException
import kotlinx.coroutines.launch
import sk.ntnlabs.kyberia.R
import sk.ntnlabs.kyberia.data.AuthRepository
import sk.ntnlabs.kyberia.data.LoginResult
import sk.ntnlabs.kyberia.data.SettingsStore
import sk.ntnlabs.kyberia.parse.LoginType

data class LoginUiState(
    val loading: Boolean = false,
    @StringRes val error: Int? = null,
    val byId: Boolean = false,
)

class LoginViewModel(private val auth: AuthRepository, private val settings: SettingsStore) : ViewModel() {

    var state by mutableStateOf(LoginUiState(byId = settings.current.loginById))
        private set

    /** Voľbu si appka pamätá – kto má nick, ktorý sa nedá napísať, chodí vždy cez ID. */
    fun setById(byId: Boolean) {
        state = state.copy(byId = byId, error = null)
        settings.update { it.copy(loginById = byId) }
    }

    fun login(username: String, password: String) {
        if (state.loading) return
        val byId = state.byId
        state = LoginUiState(loading = true, byId = byId)
        viewModelScope.launch {
            val error = try {
                when (auth.login(username.trim(), password, if (byId) LoginType.ID else LoginType.NAME)) {
                    LoginResult.Success -> null
                    LoginResult.Rejected -> if (byId) R.string.login_rejected_id else R.string.login_rejected
                    LoginResult.FormNotFound -> R.string.login_form_not_found
                }
            } catch (e: IOException) {
                R.string.login_network_error
            }
            state = LoginUiState(loading = false, error = error, byId = byId)
        }
    }

    companion object {
        fun factory(auth: AuthRepository, settings: SettingsStore) = viewModelFactory {
            initializer { LoginViewModel(auth, settings) }
        }
    }
}
