package com.example.unisphere.ui.screen.accessScreen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.unisphere.R
import com.example.unisphere.ui.composables.NavigationRoute
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    navController: NavHostController,
    viewModel: LoginViewModel = viewModel()
) {
    val state = viewModel.state
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.logo_completo),
            contentDescription = "App Logo",
            modifier = Modifier.size(if (scrollState.maxValue > 0) 150.dp else 250.dp),
            tint = Color.Unspecified
        )

        Text(
            text = "Accedi al tuo Account",
            fontSize = 16.sp,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- USERNAME ---
        OutlinedTextField(
            value = state.username,
            onValueChange = {
                viewModel.onAction(LoginAction.OnUsernameChanged(it))
            },
            label = { Text("Username") },
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            singleLine = true,
            isError = state.isError
        )

        Spacer(modifier = Modifier.height(16.dp))


        OutlinedTextField(
            value = state.password,
            onValueChange = {
                viewModel.onAction(LoginAction.OnPasswordChanged(it)) // Invia azione
            },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = state.isError
        )
        if (state.isError) {
            Text(
                text = "Credenziali non corrette. Riprova.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp).align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.onAction(
                    action = LoginAction.OnLoginClicked,
                    onSuccess = {
                        navController.navigate(NavigationRoute.ProfileScreen) {
                            popUpTo(NavigationRoute.LoginScreen) {
                                inclusive = true
                            }
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Accedi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Nuovo su UniSphere?", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { navController.navigate(NavigationRoute.SignInScreen) }) {
                Text("Crea account", fontWeight = FontWeight.Bold)
            }
        }
    }
}