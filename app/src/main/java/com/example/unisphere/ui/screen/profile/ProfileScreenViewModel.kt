package com.example.unisphere.ui.screen.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.SupabaseClient
import com.example.unisphere.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val isLoading: Boolean = true,
    val name: String = "",
    val surname: String = "",
    val username: String = "",
    val email: String = "",
    val profilePictureUri: String? = null,
    val currentTheme: String = "Default",
    val isLoggedIn: Boolean = false,
    val dialogError: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    var state by mutableStateOf(ProfileState())
        private set

    private var cachedUid: String? = null

    // VARIABILE SCUDO: Blocca il DB dal sovrascrivere la foto mentre aspettiamo che l'app si risvegli
    private var pendingProfileImage: String? = null

    init {
        observeSession()
    }

    private fun observeSession() = viewModelScope.launch {
        SupabaseClient.client.auth.sessionStatus.collect { status ->
            if (status is SessionStatus.Authenticated) {
                val uid = status.session.user?.id
                state = state.copy(isLoggedIn = true)
                if (uid != null && uid != cachedUid) {
                    cachedUid = uid
                    loadUserProfileFromDb(uid)
                }
            } else {
                cachedUid = null
                state = ProfileState(isLoading = false, isLoggedIn = false)
            }
        }
    }

    private fun loadUserProfileFromDb(uid: String) {
        viewModelScope.launch {
            userRepository.getCurrentUserProfile(uid).collect { user ->
                user?.let {
                    state = state.copy(
                        isLoading = false,
                        name = it.name,
                        surname = it.surname,
                        username = it.username,
                        email = it.email,
                        // SE stiamo caricando una foto nuova, ignoriamo quella vecchia del Database!
                        profilePictureUri = pendingProfileImage ?: it.profilePictureUri,
                        currentTheme = it.currentTheme
                    )
                }
            }
        }
    }

    fun updateProfileImage(uriString: String) {
        // 1. Attiviamo lo scudo e aggiorniamo subito la grafica per non far sparire l'immagine
        pendingProfileImage = uriString
        state = state.copy(profilePictureUri = uriString)

        viewModelScope.launch(Dispatchers.IO) {
            // 2. Loop di resistenza: se l'app è morta in background, l'ID è temporaneamente null.
            // Aspettiamo fino a 2 secondi che Supabase si ricarichi in memoria.
            var uid = cachedUid ?: SupabaseClient.client.auth.currentUserOrNull()?.id
            var retries = 0

            while (uid == null && retries < 20) {
                delay(100) // Aspetta 1 decimo di secondo
                uid = cachedUid ?: SupabaseClient.client.auth.currentUserOrNull()?.id
                retries++
            }

            // 3. Ora che abbiamo recuperato l'ID al 100%, salviamo la foto nel DB locale.
            if (uid != null) {
                userRepository.updateProfileImage(uid, uriString)
            }

            // 4. Disattiviamo lo scudo. Ora il DB è allineato e sicuro.
            pendingProfileImage = null
        }
    }

    // --- METODI ACCOUNT (INVARIATI) ---

    fun updateUsername(newUsername: String, onComplete: (Boolean) -> Unit) {
        val uid = cachedUid ?: return
        if (newUsername.trim().isBlank()) {
            state = state.copy(dialogError = "Lo username non può essere vuoto.")
            onComplete(false)
            return
        }

        viewModelScope.launch {
            if (newUsername != state.username && userRepository.isUsernameTaken(newUsername)) {
                state = state.copy(dialogError = "Questo username è già in uso.")
                onComplete(false)
            } else {
                state = state.copy(username = newUsername, dialogError = null)
                launch(Dispatchers.IO) {
                    userRepository.updateUsername(uid, newUsername)
                }
                onComplete(true)
            }
        }
    }

    fun updateEmail(newEmail: String, onComplete: (Boolean) -> Unit) {
        val uid = cachedUid ?: return
        if (!newEmail.contains("@") || !newEmail.contains(".")) {
            state = state.copy(dialogError = "Inserisci un'email valida.")
            onComplete(false)
            return
        }

        viewModelScope.launch {
            if (newEmail != state.email && userRepository.isEmailTaken(newEmail)) {
                state = state.copy(dialogError = "Questa email è già associata a un account.")
                onComplete(false)
            } else {
                state = state.copy(email = newEmail, dialogError = null)
                launch(Dispatchers.IO) {
                    userRepository.updateEmail(uid, newEmail)
                }
                onComplete(true)
            }
        }
    }

    fun clearDialogError() {
        state = state.copy(dialogError = null)
    }

    fun setTheme(theme: String) {
        state = state.copy(currentTheme = theme)
        cachedUid?.let { uid ->
            viewModelScope.launch(Dispatchers.IO) {
                userRepository.updateLocalTheme(uid, theme)
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                SupabaseClient.client.auth.signOut()
                onSuccess()
            } catch (e: Exception) {
                userRepository.clearLocalData()
                onSuccess()
            }
        }
    }
}