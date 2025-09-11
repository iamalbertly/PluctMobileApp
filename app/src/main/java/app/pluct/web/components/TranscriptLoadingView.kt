package app.pluct.web.components

import android.util.Log
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.pluct.ui.utils.WebViewUtils
import app.pluct.ui.utils.JavaScriptBridge
import app.pluct.ui.utils.WebViewScripts
import androidx.compose.ui.platform.LocalContext

private const val TAG = "TranscriptLoadingView"

/**
 * Loading state view for transcript extraction with performance optimization
 */
@Composable
fun TranscriptLoadingView(
    videoUrl: String,
    progress: Int,
    onTranscriptReceived: (String) -> Unit,
    onError: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // WebView for transcript extraction
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    // Configure WebView with enhanced SSL fixes and callbacks
                    val runId = java.util.UUID.randomUUID().toString()
                    WebViewUtils.configureWebViewForTranscript(
                        webView = this,
                        videoUrl = videoUrl,
                        onTranscriptReceived = { transcript ->
                            Log.d(TAG, "Transcript received in TranscriptLoadingView: ${transcript.take(50)}...")
                            onTranscriptReceived(transcript)
                        },
                        onError = onError
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Loading overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    progress = progress.toFloat() / 100f,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Extracting transcript...",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "This may take a minute",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
