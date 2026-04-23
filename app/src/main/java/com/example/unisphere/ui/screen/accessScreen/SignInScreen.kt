package com.example.unisphere.ui.screen.accessScreen

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.unisphere.ui.composables.NavigationRoute
import com.example.unisphere.ui.utils.rememberImagePicker
import com.example.unisphere.R

@Composable
fun SignInScreen(
    navController: NavHostController,
    viewModel: SignInViewModel = viewModel()
) {
    val state = viewModel.state
    val scrollState = rememberScrollState()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val openImagePicker = rememberImagePicker { uri ->
        selectedImageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(20.dp))

        // --- SELEZIONE IMMAGINE PROFILO CON ICONA FOTOCAMERA ---
        Box(
            modifier = Modifier
                .size(120.dp)
                .clickable { openImagePicker() },
            contentAlignment = Alignment.BottomEnd
        ) {
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
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
                color = Color.Gray,
                tonalElevation = 4.dp
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Change photo",
                    modifier = Modifier.padding(8.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Crea il tuo account per iniziare",
            fontSize = 16.sp,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = state.name,
            onValueChange = { viewModel.onAction(SignInAction.OnNameChanged(it)) },
            label = { Text("Nome") },
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            singleLine = true,
            isError = state.isError
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.surname,
            onValueChange = { viewModel.onAction(SignInAction.OnSurnameChanged(it)) },
            label = { Text("Cognome") },
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            singleLine = true,
            isError = state.isError
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.username,
            onValueChange = { viewModel.onAction(SignInAction.OnUsernameChanged(it)) },
            label = { Text("Username") },
            leadingIcon = { Icon(Icons.Outlined.Badge, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            singleLine = true,
            isError = state.isError
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = { viewModel.onAction(SignInAction.OnEmailChanged(it)) },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = state.isError
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = { viewModel.onAction(SignInAction.OnPasswordChanged(it)) },
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
                text = "Controlla i dati inseriti (Password min. 6 caratteri)",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp).align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.onAction(SignInAction.OnCreateAccountClicked) {
                    navController.navigate(NavigationRoute.Homescreen)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
        ) {

            Text("Crea Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Hai già un profilo?", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { navController.navigate(NavigationRoute.LoginScreen) }) {
                Text("Accedi", fontWeight = FontWeight.Bold)
            }
        }
    }
    }
