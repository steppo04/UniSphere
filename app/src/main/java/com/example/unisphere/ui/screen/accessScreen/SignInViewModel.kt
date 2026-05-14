package com.example.unisphere.ui.screen.accessScreen

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

// Stato aggiornato con il campo theme
data class SignInState(
    val name: String = "",
    val surname: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val theme: String = "Default",          // Aggiunto per la persistenza del tema
    val isError: Boolean = false,
    val isLoading: Boolean = false,         // Per l'animazione di caricamento
    val errorMessage: String? = null        // Per gli errori dinamici
)

sealed interface SignInAction {
    data class OnNameChanged(val value: String) : SignInAction
    data class OnSurnameChanged(val value: String) : SignInAction
    data class OnUsernameChanged(val value: String) : SignInAction
    data class OnEmailChanged(val value: String) : SignInAction
    data class OnPasswordChanged(val value: String) : SignInAction
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
            is SignInAction.OnNameChanged -> {
                state = state.copy(name = action.value, isError = false)
            }
            is SignInAction.OnSurnameChanged -> {
                state = state.copy(surname = action.value, isError = false)
            }
            is SignInAction.OnUsernameChanged -> {
                state = state.copy(username = action.value, isError = false)
            }
            is SignInAction.OnEmailChanged -> {
                state = state.copy(email = action.value, isError = false)
            }
            is SignInAction.OnPasswordChanged -> {
                state = state.copy(password = action.value, isError = false)
            }
            is SignInAction.OnCreateAccountClicked -> {
                validateAndCreate(onSuccess)
            }
        }
    }

    private fun validateAndCreate(onSuccess: () -> Unit) {
        val isValid = state.email.contains("@") &&
                state.password.length >= 6 &&
                state.name.isNotBlank() &&
                state.username.isNotBlank()

        if (!isValid) {
            state = state.copy(
                isError = true,
                errorMessage = "Compila tutti i campi correttamente (Password min. 6 caratteri)"
            )
            return
        }

        if (state.isLoading) return

        viewModelScope.launch {
            state = state.copy(isLoading = true, isError = false)
            try {
                // 1. Registrazione su Supabase
                SupabaseClient.client.auth.signUpWith(Email) {
                    email = state.email
                    password = state.password
                }

                // 2. Recupero l'UID dell'utente appena creato
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                val uid = currentUser?.id

                if (uid != null) {
                    // 3. Salvo i dati aggiuntivi in Room tramite il Repository
                    userRepository.saveUserProfile(
                        uid = uid,
                        email = state.email,
                        name = state.name,
                        surname = state.surname,
                        username = state.username,
                        theme = state.theme
                    )

                    state = state.copy(isLoading = false)
                    onSuccess()
                } else {
                    throw Exception("Errore nel recupero dell'identificativo utente da Supabase.")
                }

            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    isError = true,
                    errorMessage = "Errore: ${e.localizedMessage}"
                )
            }
        }
    }
}