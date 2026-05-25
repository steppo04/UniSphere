package com.example.unisphere.ui.screen.accessScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.SupabaseClient
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResetPasswordState(
    val newPasswordCode: String = "",
    val confirmPasswordCode: String = "", // AGGIUNTO: Campo di sdoppiamento controllo
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class ResetPasswordViewModel @Inject constructor() : ViewModel() {

    var state by mutableStateOf(ResetPasswordState())
        private set

    private val passwordRegex = "^(?=.*[A-Z])(?=.*\\d).{8,}$".toRegex()

    fun onPasswordChanged(value: String) {
        state = state.copy(newPasswordCode = value, isError = false)
        checkPasswordsMatch()
    }

    // AGGIUNTO: Listener per i cambiamenti del secondo campo di input
    fun onConfirmPasswordChanged(value: String) {
        state = state.copy(confirmPasswordCode = value, isError = false)
        checkPasswordsMatch()
    }

    // Controllo automatico asincrono sulla corrispondenza delle password digitate
    private fun checkPasswordsMatch() {
        if (state.confirmPasswordCode.isNotBlank() && state.newPasswordCode != state.confirmPasswordCode) {
            state = state.copy(
                isError = true,
                errorMessage = "Le password inserite non corrispondono."
            )
        }
    }

    fun finalizePasswordReset(onSuccess: () -> Unit) {
        // Verifica di sicurezza prima della chiamata di rete
        if (state.newPasswordCode != state.confirmPasswordCode) {
            state = state.copy(
                isError = true,
                errorMessage = "Le password inserite non corrispondono."
            )
            return
        }

        if (!state.newPasswordCode.matches(passwordRegex)) {
            state = state.copy(
                isError = true,
                errorMessage = "La password deve contenere almeno 8 caratteri, una lettera maiuscola e un numero."
            )
            return
        }

        if (state.isLoading) return

        state = state.copy(isLoading = true, isError = false)

        viewModelScope.launch {
            try {
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