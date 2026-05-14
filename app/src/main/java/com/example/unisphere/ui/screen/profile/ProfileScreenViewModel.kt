package com.example.unisphere.ui.screen.profile

import android.util.Log
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    var state by mutableStateOf(ProfileState())
        private set

    private var cachedUid: String? = null

    init {
        observeSession()
    }

    private fun observeSession() = viewModelScope.launch {
        SupabaseClient.client.auth.sessionStatus.collect { status ->
            state = when (status) {
                is SessionStatus.Authenticated -> {
                    val uid = status.session.user?.id
                    if (uid != null && uid != cachedUid) {
                        cachedUid = uid
                        loadUserProfileFromDb(uid)
                    }
                    state.copy(isLoggedIn = true, isLoading = false)
                }
                else -> {
                    cachedUid = null
                    state.copy(isLoggedIn = false, isLoading = false, currentTheme = "Default")
                }
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
                        profilePictureUri = it.profilePictureUri,
                        currentTheme = it.currentTheme
                    )
                }
            }
        }
    }

    fun setTheme(theme: String) {
        state = state.copy(currentTheme = theme)
        cachedUid?.let { uid ->
            viewModelScope.launch(Dispatchers.IO) {
                userRepository.updateLocalTheme(uid, theme)
            }
        }
    }

    fun updateProfileImage(uriString: String) {
        state = state.copy(profilePictureUri = uriString)
        cachedUid?.let { uid ->
            viewModelScope.launch(Dispatchers.IO) {
                withContext(NonCancellable) {
                    userRepository.updateProfileImage(uid, uriString)
                }
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