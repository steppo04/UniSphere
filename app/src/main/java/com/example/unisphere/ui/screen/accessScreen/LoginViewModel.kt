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
    data object OnLoginClicked : LoginAction
    data object OnForgotPasswordClicked : LoginAction
    data object OnDismissMessages : LoginAction
}

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {

    var state by mutableStateOf(LoginState())
        private set

    fun onAction(action: LoginAction, onSuccess: () -> Unit = {}) {
        when (action) {
            is LoginAction.OnEmailChanged -> {
                state = state.copy(email = action.value, isError = false, successMessage = null)
            }
            is LoginAction.OnPasswordChanged -> {
                state = state.copy(password = action.value, isError = false)
            }
            LoginAction.OnLoginClicked -> performLogin(onSuccess)
            LoginAction.OnForgotPasswordClicked -> performPasswordReset()
            LoginAction.OnDismissMessages -> {
                state = state.copy(errorMessage = null, successMessage = null)
            }
        }
    }

    // Funzione helper riutilizzabile per convalidare la stringa email via codice
    private fun isEmailInvalid(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isBlank() || !trimmed.contains("@") || !trimmed.contains(".")
    }

    private fun performLogin(onSuccess: () -> Unit) {
        if (isEmailInvalid(state.email)) {
            state = state.copy(
                isError = true,
                errorMessage = "Inserisci un indirizzo email valido prima di accedere."
            )
            return
        }

        if (state.password.isBlank()) {
            state = state.copy(
                isError = true,
                errorMessage = "La password non può essere vuota."
            )
            return
        }

        if (state.isLoading) return

        viewModelScope.launch {
            state = state.copy(isLoading = true, isError = false, successMessage = null)
            try {
                SupabaseClient.client.auth.signInWith(Email) {
                    email = state.email.trim()
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
        val emailTrimmed = state.email.trim()
        if (isEmailInvalid(emailTrimmed)) {
            state = state.copy(
                isError = true,
                errorMessage = "Inserisci una mail valida nel campo di testo per ricevere il link di ripristino."
            )
            return
        }

        if (state.isLoading) return

        viewModelScope.launch {
            state = state.copy(isLoading = true, isError = false, successMessage = null)
            try {
                SupabaseClient.client.auth.resetPasswordForEmail(emailTrimmed)
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
}