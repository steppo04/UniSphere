package com.example.unisphere.ui.screen.accessScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

data class ResetPasswordState(
    val newPasswordCode: String = "",
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class ResetPasswordViewModel : ViewModel() {
    var state by mutableStateOf(ResetPasswordState())
        private set

    fun onPasswordChanged(value: String) {
        state = state.copy(newPasswordCode = value, isError = false)
    }

    fun finalizePasswordReset(onSuccess: () -> Unit) {
        // Applichiamo lo stesso regex robusto della registrazione (Min. 8 caratteri, 1 Maiuscola, 1 Numero)
        val passwordRegex = "^(?=.*[A-Z])(?=.*\\d).{8,}$".toRegex()
        if (!state.newPasswordCode.matches(passwordRegex)) {
            state = state.copy(
                isError = true,
                errorMessage = "La password deve contenere almeno 8 caratteri, una lettera maiuscola e un numero."
            )
            return
        }

        state = state.copy(isLoading = true, isError = false)
        viewModelScope.launch {
            try {
                // Aggiorna sul server di Supabase la password dell'utente agganciato alla sessione del link
                SupabaseClient.client.auth.updateUser {
                    password = state.newPasswordCode
                }
                state = state.copy(isLoading = false, isSuccess = true)
                onSuccess()
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    isError = true,
                    errorMessage = e.localizedMessage ?: "Impossibile aggiornare la password."
                )
            }
        }
    }
}