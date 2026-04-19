package com.example.unisphere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.unisphere.ui.composables.NavGraph
import com.example.unisphere.ui.theme.UniSphereTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UniSphereTheme {
                val navController = rememberNavController()
                NavGraph(navController)
            }
        }
    }
}