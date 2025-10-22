package app.pluct.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun IngestScreen(
    navController: androidx.navigation.NavController,
    initialUrl: String? = null,
    initialCaption: String? = null
) {
    Text(
        text = "Ingest Screen - Simple Version",
        modifier = Modifier
    )
}
