package com.example.unisphere

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.unisphere.db.SupabaseClient
import com.example.unisphere.ui.composables.NavGraph
import com.example.unisphere.ui.screen.profile.ProfileViewModel
import com.example.unisphere.ui.theme.UniSphereTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.handleDeeplinks // <--- IMPORTAZIONE CORRETTA SUL CLIENT GENERALE

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ProfileViewModel = hiltViewModel()
            val state = viewModel.state

            val useDarkTheme = when {
                !state.isLoggedIn -> isSystemInDarkTheme()
                state.currentTheme == "Scuro" -> true
                state.currentTheme == "Chiaro" -> false
                else -> isSystemInDarkTheme()
            }

            UniSphereTheme(darkTheme = useDarkTheme) {
                Surface {
                    val controller = rememberNavController()
                    navController = controller

                    NavGraph(navController = controller)

                    LaunchedEffect(Unit) {
                        intent?.let { handleSupabaseIntent(it) }
                    }
                }
            }
        }
    }

    // CORRETTO: Rimosso il '?' da Intent per rispettare la firma non-null di AndroidX
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSupabaseIntent(intent)
    }

    private fun handleSupabaseIntent(intent: Intent) {
        val data = intent.data
        if (data != null && data.scheme == "unisphere" && data.host == "reset-password") {
            try {
                // CORRETTO: Chiamato direttamente sul client e non su client.auth
                SupabaseClient.client.handleDeeplinks(intent)

                navController?.navigate("reset_password_screen") {
                    popUpTo(0) { inclusive = true }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}