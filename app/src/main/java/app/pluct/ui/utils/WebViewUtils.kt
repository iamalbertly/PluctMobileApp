package app.pluct.ui.utils

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import app.pluct.R
import java.util.UUID

/**
 * Simplified WebView utilities for transcript extraction
 */
object WebViewUtils {
    private const val TAG = "WebViewUtils"
    
    /**
     * Configure WebView for transcript extraction with strict URL validation
     */
    fun configureWebViewForTranscript(
        webView: WebView,
        videoUrl: String,
        onTranscriptReceived: (String) -> Unit,
        onError: (String) -> Unit,
        onPageLoaded: (() -> Unit)? = null
    ) {
        try {
            // Check if already configured to prevent duplicate runs
            val existingRunId = webView.getTag(R.id.tag_run_id) as? String
            if (existingRunId != null) {
                Log.d(TAG, "WV:A:already_configured run=$existingRunId")
                return
            }
            
            // Generate run ID
            val runId = UUID.randomUUID().toString()
            Log.d(TAG, "WV:A:run_id=$runId")
            RunRingBuffer.addLog(runId, "INFO", "run_id=$runId", "WV")
            
            // Validate URL (non-blank; allow any provider input)
            if (videoUrl.isBlank()) {
                Log.e(TAG, "WV:A:fatal_blank_url run=$runId")
                RunRingBuffer.addLog(runId, "ERROR", "fatal_blank_url", "WV")
                onError("blank_url")
                return
            }
            
            Log.d(TAG, "WV:A:url=$videoUrl run=$runId")
            RunRingBuffer.addLog(runId, "INFO", "url=$videoUrl", "WV")
            
            // Set WebView tags
            webView.setTag(R.id.tag_video_url, videoUrl)
            webView.setTag(R.id.tag_run_id, runId)
            
            // Configure WebView
            WebViewConfiguration.configureWebView(webView, runId)
            
            // Add JavaScript bridge
            webView.addJavascriptInterface(JavaScriptBridge(onTranscriptReceived, onError), "AndroidBridge")
            
            // Load first available provider (in order of preference)
            val availableProviders = ProviderSettings.getAvailableProviders(webView.context)
            
            if (availableProviders.isEmpty()) {
                Log.e(TAG, "No providers enabled")
                onError("no_providers_enabled")
                return
            }
            
            val selectedProvider = availableProviders.first()
            
            val baseUrl = when (selectedProvider) {
                TranscriptProvider.TOKAUDIT -> "https://script.tokaudit.io/"
                TranscriptProvider.GETTRANSCRIBE -> "https://www.gettranscribe.ai"
                TranscriptProvider.OPENAI -> "https://platform.openai.com/" // OpenAI doesn't have direct web interface, but we can handle it differently
            }
            val headers = mapOf("Accept-Language" to "en-US,en;q=0.9")
            webView.loadUrl(baseUrl, headers)
            
            Log.d(TAG, "WV:A:webview_configured_for_transcript run=$runId")
            RunRingBuffer.addLog(runId, "INFO", "webview_configured_for_transcript", "WV")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring WebView: ${e.message}", e)
            onError("configuration_error")
        }
    }
    
    /**
     * Configure WebView for manual mode (fallback)
     */
    fun configureWebViewForManualMode(
        webView: WebView,
        videoUrl: String,
        onTranscriptReceived: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val runId = UUID.randomUUID().toString()
            Log.d(TAG, "WV:A:manual_mode run=$runId")
            RunRingBuffer.addLog(runId, "INFO", "manual_mode", "WV")
            
            // Set WebView tags
            webView.setTag(R.id.tag_video_url, videoUrl)
            webView.setTag(R.id.tag_run_id, runId)
            
            // Configure WebView
            WebViewConfiguration.configureWebView(webView, runId)
            
            // Add JavaScript bridge
            webView.addJavascriptInterface(JavaScriptBridge(onTranscriptReceived, onError), "AndroidBridge")
            
            // Load TokAudit
            webView.loadUrl("https://script.tokaudit.io/")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring WebView for manual mode: ${e.message}", e)
            onError("manual_configuration_error")
        }
    }

    /**
     * Get video URL from WebView tag
     */
    fun getVideoUrlFromWebView(webView: WebView): String? {
        return webView.getTag(R.id.tag_video_url) as? String
    }
    
    /**
     * Get run ID from WebView tag
     */
    fun getRunIdFromWebView(webView: WebView): String? {
        return webView.getTag(R.id.tag_run_id) as? String
    }
    
    /**
     * Clear WebView tags
     */
    fun clearWebViewTags(webView: WebView) {
        webView.setTag(R.id.tag_video_url, null)
        webView.setTag(R.id.tag_run_id, null)
    }
    
    /**
     * Check if WebView is configured
     */
    fun isWebViewConfigured(webView: WebView): Boolean {
        return webView.getTag(R.id.tag_run_id) != null
    }
}