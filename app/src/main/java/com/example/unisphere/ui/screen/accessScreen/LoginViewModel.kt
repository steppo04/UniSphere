package com.example.unisphere.ui.screen.accessScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.SupabaseClient
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isError: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

sealed interface LoginAction {
    data class OnEmailChanged(val value: String) : LoginAction
    data class OnPasswordChanged(val value: String) : LoginAction
    object OnLoginClicked : LoginAction
    object OnForgotPasswordClicked : LoginAction
    object OnDismissMessages : LoginAction
}

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {
    var state by mutableStateOf(LoginState())
        private set

    fun onAction(action: LoginAction, onSuccess: () -> Unit = {}) {
        when (action) {
            is LoginAction.OnEmailChanged -> state = state.copy(email = action.value, isError = false, successMessage = null)
            is LoginAction.OnPasswordChanged -> state = state.copy(password = action.value, isError = false)
            is LoginAction.OnLoginClicked -> performLogin(onSuccess)
            is LoginAction.OnForgotPasswordClicked -> performPasswordReset()
            is LoginAction.OnDismissMessages -> state = snackbarMessageCleaned()
        }
    }

    private fun performLogin(onSuccess: () -> Unit) {
        if (state.isLoading) return
        viewModelScope.launch {
            state = state.copy(isLoading = true, isError = false, successMessage = null)
            try {
                SupabaseClient.client.auth.signInWith(Email) {
                    email = state.email
                    password = state.password
                }
                state = state.copy(isLoading = false)
                onSuccess()
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    isError = true,
                    errorMessage = "Credenziali errate o errore di rete"
                )
            }
        }
    }

    private fun performPasswordReset() {
        // Ulteriore barriera di sicurezza sul ViewModel
        if (state.email.trim().isBlank() || !state.email.contains("@")) {
            state = state.copy(
                isError = true,
                errorMessage = "Inserisci una mail valida."
            )
            return
        }

        if (state.isLoading) return

        viewModelScope.launch {
            state = state.copy(isLoading = true, isError = false, successMessage = null)
            try {
                SupabaseClient.client.auth.resetPasswordForEmail(state.email.trim())
                state = state.copy(
                    isLoading = false,
                    successMessage = "Email di ripristino inviata! Controlla la posta."
                )
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    isError = true,
                    errorMessage = e.localizedMessage ?: "Impossibile inviare l'email di recupero."
                )
            }
        }
    }

    private fun snackbarMessageCleaned(): LoginState {
        return state.copy(errorMessage = null, successMessage = null)
    }
}