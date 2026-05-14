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
    val errorMessage: String? = null
)

sealed interface LoginAction {
    data class OnEmailChanged(val value: String) : LoginAction
    data class OnPasswordChanged(val value: String) : LoginAction
    data object OnLoginClicked : LoginAction
}

@HiltViewModel // Fondamentale per far funzionare hiltViewModel() nella UI
class LoginViewModel @Inject constructor() : ViewModel() {
    var state by mutableStateOf(LoginState())
        private set

    fun onAction(action: LoginAction, onSuccess: () -> Unit = {}) {
        when (action) {
            is LoginAction.OnEmailChanged -> state = state.copy(email = action.value, isError = false)
            is LoginAction.OnPasswordChanged -> state = state.copy(password = action.value, isError = false)
            is LoginAction.OnLoginClicked -> performLogin(onSuccess)
        }
    }

    private fun performLogin(onSuccess: () -> Unit) {
        if (state.isLoading) return
        viewModelScope.launch {
            state = state.copy(isLoading = true, isError = false)
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
}