package com.example.unisphere.ui.screen.accessScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.unisphere.ui.composables.NavigationRoute
import com.example.unisphere.ui.composables.UniSphereButton
import com.example.unisphere.ui.composables.UniSphereTextField

@Composable
fun ResetPasswordScreen(
    navController: NavHostController,
    viewModel: ResetPasswordViewModel = hiltViewModel()
) {
    // Estrazione dello stato reattivo dal ViewModel
    val state = viewModel.state

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Sfondo adattivo Light/Dark Mode
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Scegli la nuova Password",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Imposta una password robusta per mettere in sicurezza il tuo account UniSphere.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        UniSphereTextField(
            value = state.newPasswordCode,
            onValueChange = { viewModel.onPasswordChanged(it) },
            label = "Nuova Password",
            leadingIcon = Icons.Outlined.Lock,
            modifier = Modifier.fillMaxWidth(),
            isError = state.isError,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password) // Forza la tastiera protetta
        )

        // Rendering condizionale del messaggio di errore (validato lato ViewModel tramite Regex)
        if (state.isError) {
            Text(
                text = state.errorMessage ?: "Errore di validazione.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        UniSphereButton(
            text = "Aggiorna Password",
            isLoading = state.isLoading,
            enabled = state.newPasswordCode.isNotBlank(), // Vincolo logico di compilazione del campo
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                viewModel.finalizePasswordReset {
                    // Al successo, pulizia del backstack per evitare che l'utente torni qui con il tasto indietro
                    navController.navigate(NavigationRoute.LoginScreen) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        )
    }
}