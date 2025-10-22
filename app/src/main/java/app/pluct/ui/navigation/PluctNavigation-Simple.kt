package app.pluct.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.pluct.PluctUIMain01ActivitySimple
import app.pluct.ui.screens.SettingsScreen
import app.pluct.ui.screens.OnboardingScreen
import app.pluct.ui.screens.IngestScreen

/**
 * Simple navigation component for Pluct.
 * Minimal working version to get the app building
 */
@Composable
fun PluctNavigationSimple(navController: NavHostController) {
    // Store a reference to the NavController in the MainActivity
    val context = LocalContext.current

    LaunchedEffect(navController) {
        if (context is PluctUIMain01ActivitySimple) {
            context.setNavController(navController)
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = "home" // Default route to Home screen
    ) {
        composable(route = "home") {
            // Simple home screen placeholder
            androidx.compose.material3.Text("Pluct Home Screen - Simple Version")
        }
        
        composable(route = "settings") {
            SettingsScreen(navController = navController)
        }

        composable(route = "onboarding") {
            OnboardingScreen(navController = navController)
        }

        composable(route = "ingest") {
            // Simple ingest screen placeholder
            androidx.compose.material3.Text("Ingest Screen - Simple Version")
        }
    }
}
