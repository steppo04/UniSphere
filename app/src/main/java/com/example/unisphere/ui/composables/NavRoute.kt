package com.example.unisphere.ui.composables

import kotlinx.serialization.Serializable
sealed interface NavigationRoute {
    @Serializable data object Homescreen : NavigationRoute
    @Serializable data object LoginScreen : NavigationRoute
    @Serializable data object SignInScreen : NavigationRoute
    @Serializable data object LandingPage : NavigationRoute
    @Serializable data object CalendarScreen : NavigationRoute
    @Serializable data object AddCalendarEvent : NavigationRoute
}