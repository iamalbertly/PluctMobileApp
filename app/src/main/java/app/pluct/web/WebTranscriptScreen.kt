package app.pluct.web

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.pluct.ui.utils.WebViewUtils
import app.pluct.utils.Constants

private const val TAG = "WebTranscriptScreen"

/**
 * Main screen for WebTranscriptActivity
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebTranscriptScreen(
    videoId: String,
    videoUrl: String,
    onTranscriptReady: (String) -> Unit,
    onError: (String) -> Unit,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    
    // State variables
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var transcript by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0) }
    var webView: WebView? by remember { mutableStateOf(null) }
    
    // Service timeout effect
    LaunchedEffect(Unit) {
        // Set a timeout to detect if the service is unavailable
        kotlinx.coroutines.delay(Constants.Timeouts.SERVICE_AVAILABILITY_TIMEOUT)
        if (isLoading && !isError && transcript.isEmpty()) {
            val timeoutError = "The transcript service appears to be unavailable. Please try again later."
            Log.e(TAG, timeoutError)
            errorMessage = timeoutError
            isError = true
            isLoading = false
            onError(timeoutError)
        }
    }
    
    // Effects
    LaunchedEffect(transcript) {
        if (transcript.isNotEmpty()) {
            onTranscriptReady(transcript)
        }
    }
    
    // UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transcript Extraction") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (transcript.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                copyToClipboard(context, transcript, "TikTok Transcript")
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy transcript")
                        }
                    }
                    
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error opening browser: ${e.message}")
                            }
                        }
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isError) {
                // Error state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Error",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Error extracting transcript",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            isError = false
                            isLoading = true
                            webView?.reload()
                        }
                    ) {
                        Text("Retry")
                    }
                }
            } else if (transcript.isNotEmpty()) {
                // Transcript ready
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Transcript extracted successfully!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = transcript,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                // Loading state with WebView
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webView = this
                            
                            WebViewUtils.configureWebViewForTranscript(
                                webView = this,
                                videoUrl = videoUrl,
                                onTranscriptReceived = { extractedTranscript ->
                                    Log.d(TAG, "Transcript received")
                                    transcript = extractedTranscript
                                    isLoading = false
                                },
                                onPageLoaded = {
                                    Log.d(TAG, "Page loaded")
                                },
                                onError = { error ->
                                    Log.e(TAG, "Error: $error")
                                    errorMessage = error
                                    isError = true
                                    isLoading = false
                                    onError(error)
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Loading overlay
                if (isLoading) {
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
        }
    }
}

/**
 * Helper function to copy text to clipboard
 */
private fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Log.d(TAG, "Copied to clipboard: $label")
}
