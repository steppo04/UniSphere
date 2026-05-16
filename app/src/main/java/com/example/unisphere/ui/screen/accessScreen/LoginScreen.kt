package com.example.unisphere.ui.screen.accessScreen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.unisphere.R
import com.example.unisphere.ui.composables.NavigationRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavHostController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            snackbarHostState.showSnackbar(state.successMessage)
            viewModel.onAction(LoginAction.OnDismissMessages)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                // BANNER UNIFORMATO AI COLORI DEL TUO LOGO (STILE APPLE WIDGET)
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        // Usa il verde menta/teal del globo dal tuo tema (funziona anche in Dark Mode!)
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            // Spunta colorata con il Blu istituzionale del cappello per massimo contrasto
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = data.visuals.message,
                            // Testo adattivo calcolato da Material 3 per essere perfettamente leggibile
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.onAction(LoginAction.OnEmailChanged(it)) },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                isError = state.isError
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.onAction(LoginAction.OnPasswordChanged(it)) },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = state.isError
            )

            TextButton(
                onClick = {
                    if (state.email.trim().isBlank() || !state.email.contains("@")) {
                        Toast.makeText(context, "Inserisci una mail valida prima di continuare", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.onAction(LoginAction.OnForgotPasswordClicked)
                    }
                },
                modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
            ) {
                Text("Password dimenticata?", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            if (state.isError) {
                Text(
                    text = state.errorMessage ?: "Credenziali non corrette. Riprova.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp).align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.onAction(
                        action = LoginAction.OnLoginClicked,
                        onSuccess = {
                            navController.navigate(NavigationRoute.ProfileScreen) {
                                popUpTo(NavigationRoute.LoginScreen) { inclusive = true }
                            }
                        }
                    )
                },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Accedi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
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
}