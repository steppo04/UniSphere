package com.example.unisphere.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.unisphere.ui.composables.AppBar
import com.example.unisphere.ui.composables.BottomNavigationBar
import com.example.unisphere.ui.composables.NavigationRoute
import com.example.unisphere.ui.utils.rememberImagePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var refreshTrigger by remember { mutableStateOf(System.currentTimeMillis()) }

    val imageModel = remember(state.profilePictureUri, refreshTrigger) {
        ImageRequest.Builder(context)
            .data(state.profilePictureUri)
            .crossfade(true)
            .setParameter("refresh", refreshTrigger.toString())
            .build()
    }

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

            // --- AVATAR CON MONOGRAMMA DI RISERVA ---
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clickable { openImagePicker() },
                contentAlignment = Alignment.BottomEnd
            ) {
                if (!state.profilePictureUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(1.dp, Color.LightGray.copy(alpha = 0.4f), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val initial = state.username.take(1).uppercase()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Brush.linearGradient(colors = listOf(Color(0xFFCFD8DC), Color(0xFF90A4AE)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = initial.ifBlank { "U" }, fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 2.dp
                ) {
                    Icon(Icons.Default.CameraAlt, "Modifica", modifier = Modifier.padding(7.dp), tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "${state.name} ${state.surname}".trim().ifBlank { "Utente UniSphere" }, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = "@${state.username}", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(28.dp))

            // --- GRUPPO DETTAGLI ACCOUNT ---
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

            // --- GRUPPO PREFERENZE DI SISTEMA ---
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

            // --- RIPRISTINATO: BOTTONE LOGOUT ELEVATED ROSSO CLASSICO ---
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

    // --- POPUP DIALOGS INTERATTIVI ---

    if (showEditUsernameDialog) {
        var tempUsername by remember { mutableStateOf(state.username) }
        AlertDialog(
            onDismissRequest = { showEditUsernameDialog = false },
            title = { Text("Modifica Username", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(value = tempUsername, onValueChange = { tempUsername = it }, label = { Text("Nuovo Username") }, singleLine = true, modifier = Modifier.fillMaxWidth(), isError = state.dialogError != null)
                    if (state.dialogError != null) {
                        Text(state.dialogError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateUsername(tempUsername) { success -> if (success) showEditUsernameDialog = false }
                }) { Text("Salva") }
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
                    OutlinedTextField(value = tempEmail, onValueChange = { tempEmail = it }, label = { Text("Nuovo indirizzo email") }, singleLine = true, modifier = Modifier.fillMaxWidth(), isError = state.dialogError != null)
                    if (state.dialogError != null) {
                        Text(state.dialogError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateEmail(tempEmail) { success -> if (success) showEditEmailDialog = false }
                }) { Text("Salva") }
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
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.setTheme(theme); showThemeDialog = false }.padding(vertical = 12.dp),
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

    if (showAppInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAppInfoDialog = false },
            title = { Text("Info UniSphere", fontWeight = FontWeight.Bold) },
            text = { Text("UniSphere Coabitazione Smart Hub.\nSviluppato con Jetpack Compose, Hilt, Room e Supabase.\n\n© 2026 - Tutti i diritti riservati.", fontSize = 14.sp) },
            confirmButton = { Button(onClick = { showAppInfoDialog = false }) { Text("Chiudi") } }
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
        modifier = Modifier.fillMaxWidth().clickable(enabled = onClick != null) { onClick?.invoke() }.padding(horizontal = 16.dp, vertical = 12.dp),
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
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp),
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