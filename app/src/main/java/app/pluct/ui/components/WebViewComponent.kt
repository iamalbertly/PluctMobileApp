package app.pluct.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import app.pluct.ui.utils.WebViewUtils
import java.util.UUID

@Composable
fun WebViewComponent(
    videoUrl: String,
    onTranscriptReady: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val runId = remember { UUID.randomUUID().toString() }
    
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                WebViewUtils.configureWebViewForTranscript(
                    webView = this,
                    videoUrl = videoUrl,
                    onTranscriptReceived = onTranscriptReady,
                    onError = onError
                )
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
