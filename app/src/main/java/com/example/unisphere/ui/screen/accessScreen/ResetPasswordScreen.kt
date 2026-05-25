package com.example.unisphere.ui.screen.accessScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    val state = viewModel.state
    val scrollState = rememberScrollState()

    // Stati locali per gestire gli occhi di visibilità separatamente
    var isPassVisible by remember { mutableStateOf(false) }
    var isConfirmPassVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(scrollState),
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

        // PRIMO CAMPO: Nuova Password
        UniSphereTextField(
            value = state.newPasswordCode,
            onValueChange = { viewModel.onPasswordChanged(it) },
            label = "Nuova Password",
            leadingIcon = Icons.Outlined.Lock,
            modifier = Modifier.fillMaxWidth(),
            isError = state.isError,
            visualTransformation = if (isPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            trailingIcon = {
                val image = if (isPassVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { isPassVisible = !isPassVisible }) {
                    Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // SECONDO CAMPO: Conferma Password
        UniSphereTextField(
            value = state.confirmPasswordCode,
            onValueChange = { viewModel.onConfirmPasswordChanged(it) },
            label = "Conferma Nuova Password",
            leadingIcon = Icons.Outlined.Lock,
            modifier = Modifier.fillMaxWidth(),
            isError = state.isError,
            visualTransformation = if (isConfirmPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            trailingIcon = {
                val image = if (isConfirmPassVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { isConfirmPassVisible = !isConfirmPassVisible }) {
                    Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        )

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
            // Il bottone si abilita solo se i campi non sono vuoti e coincidono perfettamente
            enabled = state.newPasswordCode.isNotBlank() &&
                    state.confirmPasswordCode.isNotBlank() &&
                    state.newPasswordCode == state.confirmPasswordCode,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                viewModel.finalizePasswordReset {
                    navController.navigate(NavigationRoute.LoginScreen) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        )
    }
}