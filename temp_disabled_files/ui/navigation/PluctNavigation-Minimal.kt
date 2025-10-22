package app.pluct.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

/**
 * Minimal navigation component for basic functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@Composable
fun PluctNavigationMinimal(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable(route = "home") {
            PluctHomeScreenMinimal()
        }
        composable(route = "onboarding") {
            PluctOnboardingScreenMinimal()
        }
        composable(route = "settings") {
            PluctSettingsScreenMinimal()
        }
        composable(route = "ingest") {
            PluctIngestScreenMinimal()
        }
    }
}

@Composable
fun PluctHomeScreenMinimal() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Pluct",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No transcripts yet",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Process your first TikTok video to see transcripts here",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun PluctOnboardingScreenMinimal() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to Pluct",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Get started by processing your first TikTok video",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun PluctSettingsScreenMinimal() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Settings screen coming soon",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun PluctIngestScreenMinimal() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Ingest",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Ingest screen coming soon",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
