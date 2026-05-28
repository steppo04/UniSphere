package com.example.unisphere.ui.screen.accessScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.unisphere.ui.composables.NavigationRoute
import com.example.unisphere.ui.composables.UniSphereButton
import com.example.unisphere.ui.composables.UniSphereTextField
import com.example.unisphere.ui.utils.rememberImagePicker

@Composable
fun SignInScreen(
    navController: NavHostController,
    viewModel: SignInViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val scrollState = rememberScrollState()
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    val openImagePicker = rememberImagePicker { uri ->
        viewModel.onAction(SignInAction.OnImageSelected(uri))
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

        Box(
            modifier = Modifier.size(120.dp).clickable { openImagePicker() },
            contentAlignment = Alignment.BottomEnd
        ) {
            if (!state.profilePictureUri.isNullOrEmpty()) {
                AsyncImage(
                    model = state.profilePictureUri,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.AccountCircle, "Profile Picture", modifier = Modifier.fillMaxSize().clip(CircleShape), tint = MaterialTheme.colorScheme.primary)
            }

            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 4.dp
            ) {
                Icon(Icons.Default.CameraAlt, "Change photo", modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
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

        UniSphereTextField(
            value = state.name,
            onValueChange = { viewModel.onAction(SignInAction.OnNameChanged(it)) },
            label = "Nome",
            leadingIcon = Icons.Outlined.Person,
            modifier = Modifier.fillMaxWidth(),
            isError = state.isError,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(12.dp))

        UniSphereTextField(
            value = state.surname,
            onValueChange = { viewModel.onAction(SignInAction.OnSurnameChanged(it)) },
            label = "Cognome",
            leadingIcon = Icons.Outlined.Person,
            modifier = Modifier.fillMaxWidth(),
            isError = state.isError,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(12.dp))

        UniSphereTextField(
            value = state.username,
            onValueChange = { viewModel.onAction(SignInAction.OnUsernameChanged(it)) },
            label = "Username",
            leadingIcon = Icons.Outlined.Badge,
            modifier = Modifier.fillMaxWidth(),
            isError = state.isError,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(12.dp))

        UniSphereTextField(
            value = state.email,
            onValueChange = { viewModel.onAction(SignInAction.OnEmailChanged(it)) },
            label = "Email",
            leadingIcon = Icons.Outlined.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            isError = state.isError
        )

        Spacer(modifier = Modifier.height(12.dp))

        UniSphereTextField(
            value = state.password,
            onValueChange = { viewModel.onAction(SignInAction.OnPasswordChanged(it)) },
            label = "Password",
            leadingIcon = Icons.Outlined.Lock,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            isError = state.isError,
            trailingIcon = {
                val image = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        UniSphereTextField(
            value = state.confirmPassword,
            onValueChange = { viewModel.onAction(SignInAction.OnConfirmPasswordChanged(it)) },
            label = "Conferma Password",
            leadingIcon = Icons.Outlined.Lock,
            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
            isError = state.isError,
            trailingIcon = {
                val image = if (isConfirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                    Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        )

        if (state.isError) {
            Text(
                text = state.errorMessage ?: "Controlla i dati inseriti.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp).align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        UniSphereButton(
            text = "Crea Account",
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.isButtonEnabled,
            onClick = {
                viewModel.onAction(SignInAction.OnCreateAccountClicked) {
                    navController.navigate(NavigationRoute.Homescreen) {
                        popUpTo(NavigationRoute.SignInScreen) { inclusive = true }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Hai già un profilo?", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { navController.navigate(NavigationRoute.LoginScreen) }) {
                Text("Accedi", fontWeight = FontWeight.Bold)
            }
        }
    }
}