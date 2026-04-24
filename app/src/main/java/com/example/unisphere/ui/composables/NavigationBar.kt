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
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        NavigationBarItem(
            selected = currentRoute?.contains("Homescreen") == true,
            onClick = {
                navController.navigate(NavigationRoute.Homescreen) {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentRoute?.contains("WalletScreen") == true,
            onClick = {
                navController.navigate(NavigationRoute.WalletScreen) {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet") },
            label = { Text("Wallet") }
        )
        NavigationBarItem(
            selected = currentRoute?.contains("CalendarScreen") == true,
            onClick = {
                navController.navigate(NavigationRoute.CalendarScreen) {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "calendar") },
            label = { Text("Calendar") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* Navigazione futura */ },
            icon = { Icon(Icons.Default.Restaurant, contentDescription = "SmartCook") },
            label = { Text("CookAI") }
        )
        NavigationBarItem(
            selected = currentRoute?.contains("MapScreen") == true,
            onClick = {
                navController.navigate(NavigationRoute.MapScreen) {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Default.Place, contentDescription = "Maps") },
            label = { Text("Maps") }
        )
    }
}