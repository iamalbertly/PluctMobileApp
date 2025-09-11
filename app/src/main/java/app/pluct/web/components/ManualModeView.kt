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

private const val TAG = "ManualModeView"

/**
 * Manual mode view for transcript extraction
 */
@Composable
fun ManualModeView(
    manualUrl: String,
    isLoading: Boolean,
    onTranscriptReceived: (String) -> Unit,
    onError: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Instructions card at the top
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Manual Transcript Extraction",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Follow these steps to extract the transcript:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "1. The TikTok URL has been copied to your clipboard\n" +
                           "2. Paste the URL into the 'Enter Video URL' field\n" +
                           "3. Click the 'START' button\n" +
                           "4. Wait for the transcript to appear\n" +
                           "5. Click the 'Copy' button to copy the transcript",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "URL: $manualUrl",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ).padding(8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // WebView container with proper sizing
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // WebView for manual interaction
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            // Manual mode not implemented in simplified version
                            Log.d(TAG, "Manual mode not implemented")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Loading indicator for manual mode
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Loading script.tokaudit.io...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
