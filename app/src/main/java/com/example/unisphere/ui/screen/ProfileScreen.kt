package com.example.unisphere.ui.screen

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.unisphere.ui.composables.AppBar
import com.example.unisphere.ui.composables.BottomNavigationBar
import com.example.unisphere.ui.utils.rememberImagePicker

@Composable
fun ProfileScreen(navController: NavHostController) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val openImagePicker = rememberImagePicker { uri ->
        selectedImageUri = uri
    }

    Scaffold(
        topBar = { AppBar(title = "Profilo Utente", navController = navController) },
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- IMMAGINE PROFILO CON ICONA FOTOCAMERA ---
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
                    color = Color(0xFFF5F5F5), // Grigio chiaro per il cerchio
                    tonalElevation = 4.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change photo",
                        modifier = Modifier.padding(8.dp),
                        tint = Color.Gray // Icona grigia
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            ProfileInfoItem(icon = Icons.Default.Person, label = "Nome Utente", value = "Admin")
            ProfileInfoItem(icon = Icons.Default.Email, label = "Email", value = "admin@unisphere.com")

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    navController.navigate(com.example.unisphere.ui.composables.NavigationRoute.LoginScreen) {
                        popUpTo(0)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout")
            }
        }
    }
}

@Composable
fun ProfileInfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}