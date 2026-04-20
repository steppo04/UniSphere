package com.example.unisphere.ui.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        NavigationBarItem(
            selected = true,
            onClick = { /* Navigazione */ },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* Navigazione */ },
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet") },
            label = { Text("Wallet") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate(NavigationRoute.CalendarScreen) },
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "calendar") },
            label = { Text("Calendar") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* Navigazione */ },
            icon = { Icon(Icons.Default.Restaurant, contentDescription = "SmartCook") },
            label = { Text("CookAI") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* Navigazione */ },
            icon = { Icon(Icons.Default.Place, contentDescription = "SmartCook") },
            label = { Text("Events") }
        )
    }
}