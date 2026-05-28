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

// Stato immutabile dell'intero profilo, inclusi i dialoghi di editing
data class ProfileState(
    val isLoading: Boolean = true,
    val name: String = "",
    val surname: String = "",
    val username: String = "",
    val email: String = "",
    val profilePictureUri: String? = null,
    val currentTheme: String = "Default",
    val isLoggedIn: Boolean = false,
    val dialogError: String? = null,

    val showEditUsernameDialog: Boolean = false,
    val showEditEmailDialog: Boolean = false,
    val showThemeDialog: Boolean = false,
    val showAppInfoDialog: Boolean = false,
    val tempUsernameText: String = "",
    val tempEmailText: String = ""
)

sealed interface ProfileAction {
    data class OnUsernameDialogToggle(val show: Boolean) : ProfileAction
    data class OnEmailDialogToggle(val show: Boolean) : ProfileAction
    data class OnThemeDialogToggle(val show: Boolean) : ProfileAction
    data class OnAppInfoDialogToggle(val show: Boolean) : ProfileAction
    data class OnTempUsernameChanged(val value: String) : ProfileAction
    data class OnTempEmailChanged(val value: String) : ProfileAction
    data class OnThemeSelected(val theme: String) : ProfileAction
    data class OnProfileImageUpdated(val uri: String) : ProfileAction
    data object OnSaveUsernameClicked : ProfileAction
    data object OnSaveEmailClicked : ProfileAction
    data class OnLogoutClicked(val onSuccess: () -> Unit) : ProfileAction
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    var state by mutableStateOf(ProfileState())
        private set

    private var cachedUid: String? = null
    private var pendingProfileImage: String? = null

    init {
        observeSession()
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.OnUsernameDialogToggle -> {
                state = state.copy(
                    showEditUsernameDialog = action.show,
                    tempUsernameText = if (action.show) state.username else "",
                    dialogError = null
                )
            }
            is ProfileAction.OnEmailDialogToggle -> {
                state = state.copy(
                    showEditEmailDialog = action.show,
                    tempEmailText = if (action.show) state.email else "",
                    dialogError = null
                )
            }
            is ProfileAction.OnThemeDialogToggle -> state = state.copy(showThemeDialog = action.show)
            is ProfileAction.OnAppInfoDialogToggle -> state = state.copy(showAppInfoDialog = action.show)
            is ProfileAction.OnTempUsernameChanged -> state = state.copy(tempUsernameText = action.value)
            is ProfileAction.OnTempEmailChanged -> state = state.copy(tempEmailText = action.value)
            is ProfileAction.OnThemeSelected -> setTheme(action.theme)
            is ProfileAction.OnProfileImageUpdated -> updateProfileImage(action.uri)
            ProfileAction.OnSaveUsernameClicked -> saveUsername()
            ProfileAction.OnSaveEmailClicked -> saveEmail()
            is ProfileAction.OnLogoutClicked -> logout(action.onSuccess)
        }
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
                        profilePictureUri = pendingProfileImage ?: it.profilePictureUri,
                        currentTheme = it.currentTheme
                    )
                }
            }
        }
    }

    private fun updateProfileImage(uriString: String) {
        pendingProfileImage = uriString
        state = state.copy(profilePictureUri = uriString)

        viewModelScope.launch(Dispatchers.IO) {
            var uid = cachedUid ?: SupabaseClient.client.auth.currentUserOrNull()?.id
            var retries = 0
            while (uid == null && retries < 20) {
                delay(100)
                uid = cachedUid ?: SupabaseClient.client.auth.currentUserOrNull()?.id
                retries++
            }
            if (uid != null) {
                userRepository.updateProfileImage(uid, uriString)
            }
            pendingProfileImage = null
        }
    }

    private fun saveUsername() {
        val uid = cachedUid ?: return
        val newUsername = state.tempUsernameText
        if (newUsername.trim().isBlank()) {
            state = state.copy(dialogError = "Lo username non può essere vuoto.")
            return
        }

        viewModelScope.launch {
            if (newUsername != state.username && userRepository.isUsernameTaken(newUsername)) {
                state = state.copy(dialogError = "Questo username è già in uso.")
            } else {
                state = state.copy(username = newUsername, dialogError = null, showEditUsernameDialog = false)
                launch(Dispatchers.IO) {
                    userRepository.updateUsername(uid, newUsername)
                }
            }
        }
    }

    private fun saveEmail() {
        val uid = cachedUid ?: return
        val newEmail = state.tempEmailText
        if (!newEmail.contains("@") || !newEmail.contains(".")) {
            state = state.copy(dialogError = "Inserisci un'email valida.")
            return
        }

        viewModelScope.launch {
            if (newEmail != state.email && userRepository.isEmailTaken(newEmail)) {
                state = state.copy(dialogError = "Questa email è già associata a un account.")
            } else {
                state = state.copy(email = newEmail, dialogError = null, showEditEmailDialog = false)
                launch(Dispatchers.IO) {
                    userRepository.updateEmail(uid, newEmail)
                }
            }
        }
    }

    private fun setTheme(theme: String) {
        state = state.copy(currentTheme = theme, showThemeDialog = false)
        cachedUid?.let { uid ->
            viewModelScope.launch(Dispatchers.IO) {
                userRepository.updateLocalTheme(uid, theme)
            }
        }
    }

    private fun logout(onSuccess: () -> Unit) {
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