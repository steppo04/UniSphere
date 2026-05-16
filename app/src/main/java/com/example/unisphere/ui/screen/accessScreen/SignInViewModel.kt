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

// Stato immutabile centralizzato
data class SignInState(
    val name: String = "",
    val surname: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val profilePictureUri: String? = null, // <--- ORA L'IMMAGINE È PARTE DELLO STATO GLOBALE
    val theme: String = "Default",
    val isError: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

// Interfaccia delle intenzioni utente
sealed interface SignInAction {
    data class OnNameChanged(val value: String) : SignInAction
    data class OnSurnameChanged(val value: String) : SignInAction
    data class OnUsernameChanged(val value: String) : SignInAction
    data class OnEmailChanged(val value: String) : SignInAction
    data class OnPasswordChanged(val value: String) : SignInAction
    data class OnImageSelected(val uri: Uri?) : SignInAction // <--- NUOVA AZIONE
    data object OnCreateAccountClicked : SignInAction
}

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    var state by mutableStateOf(SignInState())
        private set

    fun onAction(action: SignInAction, onSuccess: () -> Unit = {}) {
        when (action) {
            is SignInAction.OnNameChanged -> state = state.copy(name = action.value, isError = false)
            is SignInAction.OnSurnameChanged -> state = state.copy(surname = action.value, isError = false)
            is SignInAction.OnUsernameChanged -> state = state.copy(username = action.value, isError = false)
            is SignInAction.OnEmailChanged -> state = state.copy(email = action.value, isError = false)
            is SignInAction.OnPasswordChanged -> state = state.copy(password = action.value, isError = false)
            is SignInAction.OnImageSelected -> state = state.copy(profilePictureUri = action.uri?.toString())
            is SignInAction.OnCreateAccountClicked -> validateAndCreate(onSuccess)
        }
    }

    private fun validateAndCreate(onSuccess: () -> Unit) {
        // 1. VERIFICA CAMPI VUOTI
        if (state.name.isBlank() || state.surname.isBlank() || state.username.isBlank() || state.email.isBlank()) {
            setError("Tutti i campi sono obbligatori.")
            return
        }

        // 2. VERIFICA FORMATO EMAIL
        if (!state.email.contains("@") || !state.email.contains(".")) {
            setError("Inserisci un indirizzo email valido.")
            return
        }

        // 3. REGEX PASSWORD: Minimo 8 caratteri, almeno una Maiuscola e almeno un Numero
        val passwordRegex = "^(?=.*[A-Z])(?=.*\\d).{8,}$".toRegex()
        if (!state.password.matches(passwordRegex)) {
            setError("La password deve contenere almeno 8 caratteri, una lettera maiuscola e un numero.")
            return
        }

        if (state.isLoading) return

        viewModelScope.launch {
            state = state.copy(isLoading = true, isError = false)
            try {
                // 4. VERIFICA DI UNICITÀ SUL DATABASE LOCALE/REMOTO VIA REPOSITORY
                // Nota: Assicurati che il tuo UserRepository esponga questi metodi di controllo
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

                // 5. REGISTRAZIONE SU SUPABASE AUTH
                SupabaseClient.client.auth.signUpWith(Email) {
                    email = state.email
                    password = state.password
                }

                val uid = SupabaseClient.client.auth.currentUserOrNull()?.id

                if (uid != null) {
                    // 6. SALVATAGGIO COMPLETO DEI DATI COMPRESA LA FOTO PROFILO
                    // Nota: Aggiorna la firma di saveUserProfile nel tuo repository per accettare anche profilePictureUri
                    userRepository.saveUserProfile(
                        uid = uid,
                        email = state.email,
                        name = state.name,
                        surname = state.surname,
                        username = state.username,
                        profilePictureUri = state.profilePictureUri,
                        theme = state.theme,
                         // <--- SALVATO CORRETTAMENTE
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
                    errorMessage = e.localizedMessage ?: "Errore durante la registrazione."
                )
            }
        }
    }

    private fun setError(message: String) {
        state = state.copy(
            isError = true,
            isLoading = false,
            errorMessage = message
        )
    }
}