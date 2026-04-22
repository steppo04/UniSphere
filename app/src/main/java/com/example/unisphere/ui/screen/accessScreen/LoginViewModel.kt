package com.example.unisphere.ui.screen.accessScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class LoginState(
    val username: String = "",
    val password: String = "",
    val isError: Boolean = false,
)

sealed interface LoginAction {
    data class OnUsernameChanged(val value: String) : LoginAction
    data class OnPasswordChanged(val value: String) : LoginAction
    data object OnLoginClicked : LoginAction
}
class LoginViewModel : ViewModel() {
    var state by mutableStateOf(LoginState())
        private set

    fun onAction(action: LoginAction, onSuccess: () -> Unit = {}) {
        when (action) {
            is LoginAction.OnUsernameChanged -> {
                state = state.copy(username = action.value, isError = false)
            }
            is LoginAction.OnPasswordChanged -> {
                state = state.copy(password = action.value, isError = false)
            }
            is LoginAction.OnLoginClicked -> {
                performLogin(onSuccess)
            }
        }
    }

    private fun performLogin(onSuccess: () -> Unit) {
        if (state.username == "admin" && state.password == "admin") {
            state = state.copy(isError = false)
            onSuccess()
        } else {
            state = state.copy(isError = true)
        }
    }
}