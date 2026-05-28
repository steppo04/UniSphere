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
    val confirmPasswordCode: String = "",
    val isButtonEnabled: Boolean = false,
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
        state = state.copy(newPasswordCode = value)
        validateFormState()
    }

    fun onConfirmPasswordChanged(value: String) {
        state = state.copy(confirmPasswordCode = value)
        validateFormState()
    }

    private fun validateFormState() {
        val pass = state.newPasswordCode
        val confirmPass = state.confirmPasswordCode

        val passwordsDoNotMatch = confirmPass.isNotBlank() &&
                confirmPass.length >= pass.length &&
                pass != confirmPass

        if (passwordsDoNotMatch) {
            state = state.copy(
                isError = true,
                errorMessage = "Le password inserite non corrispondono.",
                isButtonEnabled = false
            )
            return
        }

        val isRegexValid = pass.matches(passwordRegex)

        if (pass.isNotBlank() && confirmPass.isNotBlank() && !isRegexValid) {
            state = state.copy(
                isError = true,
                errorMessage = "La password deve contenere almeno 8 caratteri, una lettera maiuscola e un numero.",
                isButtonEnabled = false
            )
            return
        }

        val shouldEnableButton = isRegexValid && pass == confirmPass

        state = state.copy(
            isError = false,
            errorMessage = null,
            isButtonEnabled = shouldEnableButton
        )
    }

    fun finalizePasswordReset(onSuccess: () -> Unit) {
        if (state.newPasswordCode != state.confirmPasswordCode || !state.newPasswordCode.matches(passwordRegex)) {
            state = state.copy(isError = true, errorMessage = "Dati non validi.", isButtonEnabled = false)
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
                val rawMessage = e.localizedMessage ?: ""

                val translatedErrorMessage = when {
                    rawMessage.contains("different", ignoreCase = true) && rawMessage.contains("old", ignoreCase = true) -> {
                        "La nuova password deve essere diversa da quella precedente."
                    }
                    else -> rawMessage.ifBlank { "Impossibile aggiornare la password." }
                }

                state = state.copy(
                    isLoading = false,
                    isError = true,
                    errorMessage = translatedErrorMessage,
                    isButtonEnabled = true
                )
            }
        }
    }
}