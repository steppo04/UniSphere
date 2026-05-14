package com.example.unisphere.ui.screen.profile

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.unisphere.ui.composables.AppBar
import com.example.unisphere.ui.composables.BottomNavigationBar
import com.example.unisphere.ui.composables.NavigationRoute
import com.example.unisphere.ui.utils.rememberImagePicker

@Composable
fun ProfileScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current

    // trigger che serve a forzare Coil a ricaricare l'immagine subito dopo lo scatto
    var refreshTrigger by remember { mutableStateOf(System.currentTimeMillis()) }

    val imageModel = remember(state.profilePictureUri, refreshTrigger) {
        ImageRequest.Builder(context)
            .data(state.profilePictureUri)
            .crossfade(true)
            .setParameter("refresh", refreshTrigger.toString())
            .build()
    }

    // Variabili per i dialoghi
    var showEditUsernameDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Utility per la scelta foto
    val openImagePicker = rememberImagePicker { uri ->
        viewModel.updateProfileImage(uri.toString())
        // Aggiorniamo il trigger
        refreshTrigger = System.currentTimeMillis()
    }

    Log.d("DEBUG_UI", "Immagine nello stato: ${state.profilePictureUri}")

    Scaffold(
        topBar = { AppBar(title = "Profilo Utente", navController = navController) },
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clickable { openImagePicker() },
                contentAlignment = Alignment.BottomEnd
            ) {
                if (state.profilePictureUri != null) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = Color(0xFFF5F5F5),
                    tonalElevation = 4.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change photo",
                        modifier = Modifier.padding(8.dp),
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            ProfileInfoItem(
                icon = Icons.Default.Person,
                label = "Nome Utente",
                value = state.username.ifBlank { "Admin" },
                onClick = { showEditUsernameDialog = true }
            )
            ProfileInfoItem(
                icon = Icons.Default.Email,
                label = "Email",
                value = state.email.ifBlank { "admin@unisphere.com" }
            )

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Text(
                text = "Impostazioni",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )

            // --- SETTINGS ---
            SettingsItem(
                icon = Icons.Default.Palette,
                title = "Tema",
                subtitle = state.currentTheme,
                onClick = { showThemeDialog = true }
            )
            SettingsItem(
                icon = Icons.Default.Restaurant,
                title = "Ricette preferite",
                onClick = { /* Implementazione futura */ }
            )
            SettingsItem(
                icon = Icons.Default.Star,
                title = "Eventi importanti",
                onClick = { /* Implementazione futura */ }
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    viewModel.logout {
                        navController.navigate(NavigationRoute.LoginScreen) {
                            popUpTo(0)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showEditUsernameDialog) {
        var tempName by remember { mutableStateOf(state.username) }
        AlertDialog(
            onDismissRequest = { showEditUsernameDialog = false },
            title = { Text("Modifica Nome Utente") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Nuovo Username") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    // viewModel.updateUsername(tempName)
                    showEditUsernameDialog = false
                }) { Text("Salva") }
            },
            dismissButton = {
                TextButton(onClick = { showEditUsernameDialog = false }) { Text("Annulla") }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Seleziona Tema") },
            text = {
                Column {
                    listOf("Chiaro", "Scuro", "Default").forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setTheme(theme)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (state.currentTheme == theme),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = theme)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun ProfileInfoItem(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
        if (onClick != null) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
    }
}