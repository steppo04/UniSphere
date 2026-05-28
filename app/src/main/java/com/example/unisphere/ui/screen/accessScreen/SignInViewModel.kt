package com.example.unisphere.ui.screen.accessScreen

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.SupabaseClient
import com.example.unisphere.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignInState(
    val name: String = "",
    val surname: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isButtonEnabled: Boolean = false,
    val profilePictureUri: String? = null,
    val theme: String = "Default",
    val isError: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface SignInAction {
    data class OnNameChanged(val value: String) : SignInAction
    data class OnSurnameChanged(val value: String) : SignInAction
    data class OnUsernameChanged(val value: String) : SignInAction
    data class OnEmailChanged(val value: String) : SignInAction
    data class OnPasswordChanged(val value: String) : SignInAction
    data class OnConfirmPasswordChanged(val value: String) : SignInAction
    data class OnImageSelected(val uri: Uri?) : SignInAction
    data object OnCreateAccountClicked : SignInAction
}

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    var state by mutableStateOf(SignInState())
        private set

    private val passwordRegex = "^(?=.*[A-Z])(?=.*\\d).{8,}$".toRegex()

    fun onAction(action: SignInAction, onSuccess: () -> Unit = {}) {
        when (action) {
            is SignInAction.OnNameChanged -> {
                state = state.copy(name = action.value)
                validateFormState()
            }
            is SignInAction.OnSurnameChanged -> {
                state = state.copy(surname = action.value)
                validateFormState()
            }
            is SignInAction.OnUsernameChanged -> {
                state = state.copy(username = action.value)
                validateFormState()
            }
            is SignInAction.OnEmailChanged -> {
                state = state.copy(email = action.value)
                validateFormState()
            }
            is SignInAction.OnPasswordChanged -> {
                state = state.copy(password = action.value)
                validateFormState()
            }
            is SignInAction.OnConfirmPasswordChanged -> {
                state = state.copy(confirmPassword = action.value)
                validateFormState()
            }
            is SignInAction.OnImageSelected -> {
                state = state.copy(profilePictureUri = action.uri?.toString())
            }
            is SignInAction.OnCreateAccountClicked -> validateAndCreate(onSuccess)
        }
    }

    private fun validateFormState() {
        val name = state.name
        val surname = state.surname
        val username = state.username
        val email = state.email
        val pass = state.password
        val confirmPass = state.confirmPassword

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

        // Controllo Regex sui requisiti della password se entrambi i campi sono compilati
        val isPasswordRegexValid = pass.matches(passwordRegex)
        if (pass.isNotBlank() && confirmPass.isNotBlank() && !isPasswordRegexValid) {
            state = state.copy(
                isError = true,
                errorMessage = "La password deve contenere almeno 8 caratteri, una lettera maiuscola e un numero.",
                isButtonEnabled = false
            )
            return
        }

        //Controllo dell'email
        val isEmailFormatValid = email.isBlank() || (email.contains("@") && email.contains("."))
        if (!isEmailFormatValid) {
            state = state.copy(
                isError = true,
                errorMessage = "Inserisci un indirizzo email valido.",
                isButtonEnabled = false
            )
            return
        }

        // 4. l'abilitazione del bottone richiede che tutti i campi siano pieni e validi
        val allFieldsFilled = name.isNotBlank() && surname.isNotBlank() &&
                username.isNotBlank() && email.isNotBlank() &&
                pass.isNotBlank() && confirmPass.isNotBlank()

        val shouldEnable = allFieldsFilled && isPasswordRegexValid && pass == confirmPass

        state = state.copy(
            isError = false,
            errorMessage = null,
            isButtonEnabled = shouldEnable
        )
    }

    private fun validateAndCreate(onSuccess: () -> Unit) {
        if (state.password != state.confirmPassword || !state.password.matches(passwordRegex)) {
            state = state.copy(isError = true, errorMessage = "Dati non validi.", isButtonEnabled = false)
            return
        }

        if (state.isLoading) return

        viewModelScope.launch {
            state = state.copy(isLoading = true, isError = false)
            try {
                val isEmailTaken = userRepository.isEmailTaken(state.email)
                if (isEmailTaken) {
                    setError("Questa email è già associata a un account.")
                    return@launch
                }

                val isUsernameTaken = userRepository.isUsernameTaken(state.username)
                if (isUsernameTaken) {
                    setError("Questo username è già in uso. Scegline un altro.")
                    return@launch
                }

                SupabaseClient.client.auth.signUpWith(Email) {
                    email = state.email
                    password = state.password
                }

                val uid = SupabaseClient.client.auth.currentUserOrNull()?.id

                if (uid != null) {
                    userRepository.saveUserProfile(
                        uid = uid,
                        email = state.email,
                        name = state.name,
                        surname = state.surname,
                        username = state.username,
                        profilePictureUri = state.profilePictureUri,
                        theme = state.theme
                    )

                    state = state.copy(isLoading = false)
                    onSuccess()
                } else {
                    throw Exception("Impossibile recuperare l'identificativo utente.")
                }

            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    isError = true,
                    errorMessage = e.localizedMessage ?: "Errore durante la registrazione.",
                    isButtonEnabled = true
                )
            }
        }
    }

    private fun setError(message: String) {
        state = state.copy(
            isError = true,
            isLoading = false,
            errorMessage = message,
            isButtonEnabled = false
        )
    }
}