package app.pluct.ui.components

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.pluct.ui.utils.WebViewUtils

/**
 * Simplified WebView component for transcript extraction
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptTokAuditWebView(
    videoUrl: String,
    onTranscriptReceived: (String) -> Unit,
    onClose: () -> Unit
) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var isProcessing by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Get Transcript") },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        )
        
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webView = this
                    
                    // Use the WebViewUtils to configure the WebView
                    WebViewUtils.configureWebViewForTranscript(
                        webView = this,
                        videoUrl = videoUrl,
                        onTranscriptReceived = { transcript ->
                            Log.d("ScriptTokAuditWebView", "Received transcript")
                            onTranscriptReceived(transcript)
                        },
                        onPageLoaded = {
                            Log.d("ScriptTokAuditWebView", "Page loaded")
                        },
                        onError = { error ->
                            Log.e("ScriptTokAuditWebView", "Error: $error")
                        },
                        onProcessing = { processing ->
                            Log.d("ScriptTokAuditWebView", "Processing state changed: $processing")
                            isProcessing = processing
                        }
                    )
                }
            }
        )
        
        // Show loading indicator while processing
        if (isProcessing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
