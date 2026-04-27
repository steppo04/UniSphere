package com.example.unisphere.ui.composables

import kotlinx.serialization.Serializable

sealed interface NavigationRoute {
    @Serializable data object Homescreen : NavigationRoute
    @Serializable data object LoginScreen : NavigationRoute
    @Serializable data object SignInScreen : NavigationRoute
    @Serializable data object LandingPage : NavigationRoute
    @Serializable data object ProfileScreen : NavigationRoute
    @Serializable data object WalletScreen : NavigationRoute
    @Serializable data object CalendarScreen : NavigationRoute
    @Serializable data object AddCalendarEvent : NavigationRoute
    @Serializable data object MapScreen : NavigationRoute
    @Serializable data object CookScreen : NavigationRoute
}