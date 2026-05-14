package com.example.unisphere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.unisphere.ui.composables.NavGraph
import com.example.unisphere.ui.screen.profile.ProfileViewModel
import com.example.unisphere.ui.theme.UniSphereTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ProfileViewModel = hiltViewModel()
            val state = viewModel.state

            // LOGICA DINAMICA
            val useDarkTheme = when {
                // 1. Se non è loggato -> usa sempre il tema di sistema
                !state.isLoggedIn -> isSystemInDarkTheme()

                // 2. Se è loggato -> guarda la preferenza salvata
                state.currentTheme == "Scuro" -> true
                state.currentTheme == "Chiaro" -> false

                // 3. Se è loggato ma ha scelto "Default" -> torna al sistema
                else -> isSystemInDarkTheme()
            }

            UniSphereTheme(darkTheme = useDarkTheme){
                Surface {
                    val navController = rememberNavController()
                    NavGraph(navController)
                }
            }
        }
    }
}