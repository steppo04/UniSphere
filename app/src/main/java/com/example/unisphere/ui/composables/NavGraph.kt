package com.example.unisphere.ui.composables

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.unisphere.ui.screen.HomeScreen
import com.example.unisphere.ui.screen.LandingPage
import com.example.unisphere.ui.screen.accessScreen.LoginScreen
import com.example.unisphere.ui.screen.accessScreen.SigninScreen
import com.example.unisphere.ui.screen.calendar.AddCalendarEvent
import com.example.unisphere.ui.screen.calendar.CalendarScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavigationRoute.CalendarScreen
    ) {
        composable<NavigationRoute.LoginScreen> {
            LoginScreen(navController)
        }
        composable<NavigationRoute.Homescreen> {
            HomeScreen(navController)
        }
        composable<NavigationRoute.SignInScreen> {
            SigninScreen(navController)
        }
        composable<NavigationRoute.LandingPage> {
            LandingPage(navController)
        }
        composable<NavigationRoute.CalendarScreen> {
            CalendarScreen(navController)
        }
        composable<NavigationRoute.AddCalendarEvent> {
            AddCalendarEvent(navController)
        }
    }
}