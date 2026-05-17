package com.example.unisphere.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.unisphere.ui.composables.AppBar
import com.example.unisphere.ui.composables.BottomNavigationBar
import com.example.unisphere.ui.composables.NavigationRoute
import com.example.unisphere.ui.composables.UniSphereAlertDialog
import com.example.unisphere.ui.composables.UniSphereAvatar
import com.example.unisphere.ui.composables.UniSphereButton
import com.example.unisphere.ui.composables.UniSphereTextField
import com.example.unisphere.ui.utils.rememberImagePicker

@Composable
fun ProfileScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val scrollState = rememberScrollState()

    var refreshTrigger by remember { mutableStateOf(System.currentTimeMillis()) }

    var showEditUsernameDialog by remember { mutableStateOf(false) }
    var showEditEmailDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAppInfoDialog by remember { mutableStateOf(false) }

    val openImagePicker = rememberImagePicker { uri ->
        viewModel.updateProfileImage(uri.toString())
        refreshTrigger = System.currentTimeMillis()
    }

    Scaffold(
        topBar = { AppBar(title = "Impostazioni", navController = navController) },
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            UniSphereAvatar(
                username = state.username,
                profilePictureUri = state.profilePictureUri?.let {
                    if (it.isNotEmpty()) "$it?refresh=$refreshTrigger" else it
                },
                size = 110.dp,
                onClick = openImagePicker,
                badge = {
                    Surface(
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.BottomEnd),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        tonalElevation = 2.dp
                    ) {
                        Icon(Icons.Default.CameraAlt, "Modifica", modifier = Modifier.padding(7.dp), tint = Color.White)
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "${state.name} ${state.surname}".trim().ifBlank { "Utente UniSphere" }, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = "@${state.username}", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(28.dp))

            AppleSettingsGroup(title = "Dettagli Account") {
                ProfileInfoItem(icon = Icons.Default.Badge, label = "Nome completo", value = "${state.name} ${state.surname}")
                ProfileDivider()
                ProfileInfoItem(icon = Icons.Default.Person, label = "Username", value = state.username, isEditable = true) {
                    viewModel.clearDialogError()
                    showEditUsernameDialog = true
                }
                ProfileDivider()
                ProfileInfoItem(icon = Icons.Default.Email, label = "Email", value = state.email, isEditable = true) {
                    viewModel.clearDialogError()
                    showEditEmailDialog = true
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AppleSettingsGroup(title = "Preferenze Applicazione") {
                SettingsItem(icon = Icons.Default.Palette, title = "Tema dell'applicazione", subtitle = state.currentTheme) {
                    showThemeDialog = true
                }
                ProfileDivider()
                SettingsItem(icon = Icons.Default.Lock, title = "Sicurezza Account", subtitle = "Cambia o reimposta password") {
                    navController.navigate("reset_password_screen")
                }
                ProfileDivider()
                SettingsItem(icon = Icons.Default.Info, title = "Info Applicazione", subtitle = "Versione 1.0.0 (Stable)") {
                    showAppInfoDialog = true
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = {
                    viewModel.logout {
                        navController.navigate(NavigationRoute.LoginScreen) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showEditUsernameDialog) {
        var tempUsername by remember { mutableStateOf(state.username) }
        AlertDialog(
            onDismissRequest = { showEditUsernameDialog = false },
            title = { Text("Modifica Username", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    UniSphereTextField(
                        value = tempUsername,
                        onValueChange = { tempUsername = it },
                        label = "Nuovo Username",
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.dialogError != null
                    )
                    if (state.dialogError != null) {
                        Text(state.dialogError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                UniSphereButton(
                    text = "Salva",
                    onClick = {
                        viewModel.updateUsername(tempUsername) { success -> if (success) showEditUsernameDialog = false }
                    }
                )
            },
            dismissButton = { TextButton(onClick = { showEditUsernameDialog = false }) { Text("Annulla") } }
        )
    }

    if (showEditEmailDialog) {
        var tempEmail by remember { mutableStateOf(state.email) }
        AlertDialog(
            onDismissRequest = { showEditEmailDialog = false },
            title = { Text("Modifica Email", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    UniSphereTextField(
                        value = tempEmail,
                        onValueChange = { tempEmail = it },
                        label = "Nuovo indirizzo email",
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.dialogError != null
                    )
                    if (state.dialogError != null) {
                        Text(state.dialogError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                UniSphereButton(
                    text = "Salva",
                    onClick = {
                        viewModel.updateEmail(tempEmail) { success -> if (success) showEditEmailDialog = false }
                    }
                )
            },
            dismissButton = { TextButton(onClick = { showEditEmailDialog = false }) { Text("Annulla") } }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Seleziona Tema", fontWeight = FontWeight.Bold) },
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
                            RadioButton(selected = (state.currentTheme == theme), onClick = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = theme, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // --- REFACTOR COMPLETATO: Sostituito il vecchio blocco AlertDialog nativo con UniSphereAlertDialog ---
    if (showAppInfoDialog) {
        UniSphereAlertDialog(
            title = "Info UniSphere",
            text = "UniSphere.\nSviluppato con Jetpack Compose, Hilt, Room e Supabase.\n\n© 2026 - Tutti i diritti riservati.",
            confirmText = "Chiudi",
            onConfirm = { showAppInfoDialog = false },
            onDismiss = { showAppInfoDialog = false }
        )
    }
}

@Composable
fun AppleSettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title.uppercase(), style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.padding(start = 12.dp, bottom = 6.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun ProfileInfoItem(icon: ImageVector, label: String, value: String, isEditable: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        if (isEditable) {
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 13.sp, color = Color.Gray)
            }
        }
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun ProfileDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 52.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
}