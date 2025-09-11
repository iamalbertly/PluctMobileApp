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
import app.pluct.ui.utils.JavaScriptBridge
import app.pluct.ui.utils.WebViewScripts

/**
 * Simplified WebView component for transcript extraction with performance optimization
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptTokAuditWebView(
    videoUrl: String,
    runId: String,
    onTranscriptReceived: (String) -> Unit,
    onSaveTranscript: ((String, String, String) -> Unit)? = null,
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
                    
                    // Configure WebView with performance optimization
                    WebViewUtils.configureWebViewForTranscript(
                        this, 
                        videoUrl, 
                        onTranscriptReceived = { transcript ->
                            Log.d("ScriptTokAuditWebView", "Transcript received via WebViewUtils")
                            onTranscriptReceived(transcript)
                        },
                        onError = { error ->
                            Log.e("ScriptTokAuditWebView", "Error via WebViewUtils: $error")
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
