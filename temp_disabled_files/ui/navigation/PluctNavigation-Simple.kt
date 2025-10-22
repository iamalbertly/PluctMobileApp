package app.pluct.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.pluct.ui.screens.OnboardingScreen
import app.pluct.ui.screens.SettingsScreen

@Composable
fun PluctNavigationSimple(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "onboarding" // Start with a simple screen
    ) {
        composable(route = "onboarding") {
            OnboardingScreen(navController = navController)
        }
        composable(route = "settings") {
            SettingsScreen(navController = navController)
        }
        composable(route = "ingest") {
            // Simple ingest screen placeholder
            androidx.compose.material3.Text("Ingest Screen - Simple Version")
        }
    }
}