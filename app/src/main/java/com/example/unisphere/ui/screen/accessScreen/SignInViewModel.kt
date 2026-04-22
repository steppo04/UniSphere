package com.example.unisphere.ui.screen.accessScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class SignInState(
    val name: String = "",
    val surname: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val isError: Boolean = false,
)

sealed interface SignInAction {
    data class OnNameChanged(val value: String) : SignInAction
    data class OnSurnameChanged(val value: String) : SignInAction
    data class OnUsernameChanged(val value: String) : SignInAction
    data class OnEmailChanged(val value: String) : SignInAction
    data class OnPasswordChanged(val value: String) : SignInAction
    data object OnCreateAccountClicked : SignInAction
}

class SignInViewModel : ViewModel() {

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
                state.name.isNotBlank()

        if (isValid) {
            state = state.copy(isError = false)
            onSuccess()
        } else {
            state = state.copy(isError = true)
        }
    }
}