package app.pluct.data.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import app.pluct.web.WebTranscriptActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages WebView activities and web-based transcript extraction
 */
class WebActivityManager(private val context: Context) {
    companion object {
        private const val TAG = "WebActivityManager"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TRANSCRIPT_MODE = "extra_transcript_mode"
        const val EXTRA_PROVIDER = "extra_provider"
        
        // Transcript providers
        const val PROVIDER_TOKAUDIT = "tokaudit"
        const val PROVIDER_GETTRANSCRIBE = "gettranscribe"
    }
    
    private val _webActivityState = MutableStateFlow<WebActivityState>(WebActivityState.Idle)
    val webActivityState: StateFlow<WebActivityState> = _webActivityState.asStateFlow()
    
    /**
     * Launch WebView for transcript extraction using tokaudit
     */
    fun launchTokauditTranscript(url: String) {
        try {
            Log.d(TAG, "Launching tokaudit transcript for URL: $url")
            
            val intent = Intent(context, WebTranscriptActivity::class.java).apply {
                putExtra(EXTRA_URL, "https://script.tokaudit.io")
                putExtra(EXTRA_TRANSCRIPT_MODE, true)
                putExtra(EXTRA_PROVIDER, PROVIDER_TOKAUDIT)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            _webActivityState.value = WebActivityState.Loading("Opening tokaudit transcript...")
            context.startActivity(intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error launching tokaudit transcript: ${e.message}", e)
            _webActivityState.value = WebActivityState.Error("Failed to open transcript service: ${e.message}")
        }
    }
    
    /**
     * Launch WebView for transcript extraction using gettranscribe.ai
     */
    fun launchGettranscribeTranscript(transcriptionId: String) {
        try {
            val transcribeUrl = "https://www.gettranscribe.ai/transcription?id=$transcriptionId"
            Log.d(TAG, "Launching gettranscribe transcript for ID: $transcriptionId")
            
            val intent = Intent(context, WebTranscriptActivity::class.java).apply {
                putExtra(EXTRA_URL, transcribeUrl)
                putExtra(EXTRA_TRANSCRIPT_MODE, true)
                putExtra(EXTRA_PROVIDER, PROVIDER_GETTRANSCRIBE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            _webActivityState.value = WebActivityState.Loading("Opening gettranscribe.ai...")
            context.startActivity(intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error launching gettranscribe transcript: ${e.message}", e)
            _webActivityState.value = WebActivityState.Error("Failed to open transcript service: ${e.message}")
        }
    }
    
    /**
     * Launch WebView for general URL browsing
     */
    fun launchWebView(url: String) {
        try {
            Log.d(TAG, "Launching WebView for URL: $url")
            
            val intent = Intent(context, WebTranscriptActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TRANSCRIPT_MODE, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            _webActivityState.value = WebActivityState.Loading("Opening web page...")
            context.startActivity(intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error launching WebView: ${e.message}", e)
            _webActivityState.value = WebActivityState.Error("Failed to open web page: ${e.message}")
        }
    }
    
    /**
     * Launch external browser for URL
     */
    fun launchExternalBrowser(url: String) {
        try {
            Log.d(TAG, "Launching external browser for URL: $url")
            
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            _webActivityState.value = WebActivityState.Loading("Opening in browser...")
            context.startActivity(intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error launching external browser: ${e.message}", e)
            _webActivityState.value = WebActivityState.Error("Failed to open browser: ${e.message}")
        }
    }
    
    /**
     * Handle transcript result from WebView
     */
    fun handleTranscriptResult(transcript: String, provider: String) {
        try {
            Log.d(TAG, "Received transcript from $provider, length: ${transcript.length}")
            
            if (transcript.isBlank()) {
                _webActivityState.value = WebActivityState.Error("Received empty transcript from $provider")
                return
            }
            
            _webActivityState.value = WebActivityState.TranscriptReceived(transcript, provider)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling transcript result: ${e.message}", e)
            _webActivityState.value = WebActivityState.Error("Failed to process transcript: ${e.message}")
        }
    }
    
    /**
     * Handle WebView error
     */
    fun handleWebViewError(error: String, provider: String? = null) {
        Log.e(TAG, "WebView error from ${provider ?: "unknown"}: $error")
        _webActivityState.value = WebActivityState.Error("WebView error: $error")
    }
    
    /**
     * Reset state to idle
     */
    fun resetState() {
        _webActivityState.value = WebActivityState.Idle
    }
    
    /**
     * Check if a URL is supported for transcript extraction
     */
    fun isTranscriptSupported(url: String): TranscriptSupport {
        return when {
            url.contains("tiktok.com") || url.contains("vm.tiktok.com") -> {
                TranscriptSupport.Supported(PROVIDER_TOKAUDIT, "TikTok videos supported via tokaudit")
            }
            url.contains("gettranscribe.ai/transcription") -> {
                val transcriptionId = extractTranscriptionId(url)
                if (transcriptionId != null) {
                    TranscriptSupport.Supported(PROVIDER_GETTRANSCRIBE, "Gettranscribe.ai transcription")
                } else {
                    TranscriptSupport.NotSupported("Invalid gettranscribe.ai URL format")
                }
            }
            else -> {
                TranscriptSupport.NotSupported("Transcript extraction not supported for this URL")
            }
        }
    }
    
    /**
     * Extract transcription ID from gettranscribe.ai URL
     */
    private fun extractTranscriptionId(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            uri.getQueryParameter("id")
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting transcription ID: ${e.message}")
            null
        }
    }
}

/**
 * State of WebView activity operations
 */
sealed class WebActivityState {
    object Idle : WebActivityState()
    data class Loading(val message: String) : WebActivityState()
    data class TranscriptReceived(val transcript: String, val provider: String) : WebActivityState()
    data class Error(val message: String) : WebActivityState()
}

/**
 * Transcript support information for a URL
 */
sealed class TranscriptSupport {
    data class Supported(val provider: String, val description: String) : TranscriptSupport()
    data class NotSupported(val reason: String) : TranscriptSupport()
}