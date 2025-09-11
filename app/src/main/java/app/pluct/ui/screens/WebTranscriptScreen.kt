package app.pluct.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.pluct.ui.components.WebViewComponent

@Composable
fun WebTranscriptScreen(
    videoId: String,
    videoUrl: String,
    onTranscriptReady: (String) -> Unit,
    onError: (String) -> Unit,
    onBackPressed: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Extracting Transcript",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
        
        // WebView
        WebViewComponent(
            videoUrl = videoUrl,
            onTranscriptReady = onTranscriptReady,
            onError = onError,
            modifier = Modifier.weight(1f)
        )
        
        // Back button
        Button(
            onClick = onBackPressed,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Back")
        }
    }
}
