package app.pluct.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import app.pluct.PluctUIMain01ActivitySimple
import app.pluct.ui.screens.IngestScreen
import app.pluct.ui.screens.OnboardingScreen
import app.pluct.ui.screens.SettingsScreen

/**
 * Main navigation component for Pluct.
 * 
 * Why Compose Navigation: Avoids XML overhead, provides type-safe navigation,
 * and integrates seamlessly with Compose UI for better performance.
 */
@Composable
fun PluctNavigation(navController: NavHostController) {
    // Store a reference to the NavController in the MainActivity
    val context = LocalContext.current
    
    LaunchedEffect(navController) {
        if (context is PluctUIMain01ActivitySimple) {
            // Simple activity doesn't need nav controller setup
        }
    }
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route // Default route to Home screen
    ) {
        composable(route = Screen.Home.route) {
            // Simple home screen placeholder
            androidx.compose.material3.Text("Home Screen - Simple Version")
        }
        
        composable(route = Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        
        composable(route = Screen.Onboarding.route) {
            OnboardingScreen(navController = navController)
        }
        
        composable(
            route = "ingest?url={url}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "pluct://ingest?url={url}"
                }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            IngestScreen(
                url = url,
                onNavigateBack = { 
                    android.util.Log.d("PluctNavigation", "IngestScreen onNavigateBack called - popping back stack")
                    if (!navController.popBackStack()) {
                        android.util.Log.d("PluctNavigation", "Nothing to pop, navigating to home screen")
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                        }
                    }
                }
            )
        }
    }
}
