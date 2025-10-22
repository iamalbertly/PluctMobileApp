package app.pluct.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun OnboardingScreen(
    navController: androidx.navigation.NavController
) {
    Text(
        text = "Onboarding Screen - Simple Version",
        modifier = Modifier
    )
}
