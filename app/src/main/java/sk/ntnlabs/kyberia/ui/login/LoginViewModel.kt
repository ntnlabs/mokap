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

data class LoginUiState(val loading: Boolean = false, @StringRes val error: Int? = null)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    var state by mutableStateOf(LoginUiState())
        private set

    fun login(username: String, password: String) {
        if (state.loading) return
        state = LoginUiState(loading = true)
        viewModelScope.launch {
            val error = try {
                when (auth.login(username.trim(), password)) {
                    LoginResult.Success -> null
                    LoginResult.Rejected -> R.string.login_rejected
                    LoginResult.FormNotFound -> R.string.login_form_not_found
                }
            } catch (e: IOException) {
                R.string.login_network_error
            }
            state = LoginUiState(loading = false, error = error)
        }
    }

    companion object {
        fun factory(auth: AuthRepository) = viewModelFactory {
            initializer { LoginViewModel(auth) }
        }
    }
}
