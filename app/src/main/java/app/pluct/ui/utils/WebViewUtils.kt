package app.pluct.ui.utils

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import app.pluct.utils.Constants
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Utility class for WebView operations
 */
object WebViewUtils {
    private const val TAG = "WebViewUtils"
    private val TRANSCRIPT_CHECK_INTERVAL = Constants.Timeouts.TRANSCRIPT_CHECK_INTERVAL
    private val MAX_WAIT_TIME = Constants.Timeouts.MAX_TRANSCRIPT_WAIT_TIME
    
    /**
     * Configure a WebView for transcript extraction
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun configureWebViewForTranscript(
        webView: WebView,
        videoUrl: String,
        onTranscriptReceived: (String) -> Unit,
        onPageLoaded: () -> Unit = {},
        onError: (String) -> Unit = {},
        onProcessing: ((Boolean) -> Unit)? = null
    ) {
        val isProcessing = AtomicBoolean(false)
        val handler = Handler(Looper.getMainLooper())
        var startTime = 0L
        try {
            // Configure WebView settings
            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadsImagesAutomatically = true
                mediaPlaybackRequiresUserGesture = false
                setSupportMultipleWindows(false)
                
                // Enhanced settings for better performance and compatibility
                javaScriptCanOpenWindowsAutomatically = true
                allowFileAccess = true
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                
                // Enable debugging if in debug build
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    if (0 != webView.context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) {
                        android.webkit.WebView.setWebContentsDebuggingEnabled(true)
                    }
                }
            }
            
            // Set a more permissive web chrome client
            webView.webChromeClient = android.webkit.WebChromeClient()
            
            // Add JavaScript interface for communication
            webView.addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun onTranscriptReceived(transcript: String) {
                        Log.d(TAG, "Received transcript: ${transcript.take(100)}...")
                        
                        // Cancel any pending checks
                        handler.removeCallbacksAndMessages(null)
                        
                        // Update processing state
                        if (isProcessing.getAndSet(false)) {
                            onProcessing?.invoke(false)
                        }
                        
                        // Notify success
                        onTranscriptReceived(transcript)
                    }
                    
                    @JavascriptInterface
                    fun onError(error: String) {
                        Log.e(TAG, "WebView error: $error")
                        
                        // Cancel any pending checks
                        handler.removeCallbacksAndMessages(null)
                        
                        // Update processing state
                        if (isProcessing.getAndSet(false)) {
                            onProcessing?.invoke(false)
                        }
                        
                        // Notify error
                        onError(error)
                    }
                    
                    @JavascriptInterface
                    fun onNoTranscript() {
                        Log.d(TAG, "No transcript available")
                        
                        // Cancel any pending checks
                        handler.removeCallbacksAndMessages(null)
                        
                        // Update processing state
                        if (isProcessing.getAndSet(false)) {
                            onProcessing?.invoke(false)
                        }
                        
                        // Notify no transcript
                        onError("No transcript available for this video")
                    }
                    
                                @JavascriptInterface
            fun getClipboardContent(): String {
                // Ensure the URL doesn't contain the deep link prefix
                val cleanUrl = if (videoUrl.contains("url=")) {
                    try {
                        // Try to extract the actual URL from the deep link format
                        val decodedUrl = android.net.Uri.decode(videoUrl.substringAfter("url="))
                        Log.d(TAG, "Extracted URL from deep link: $decodedUrl")
                        decodedUrl
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to extract URL from deep link: ${e.message}", e)
                        videoUrl
                    }
                } else {
                    videoUrl
                }
                
                // Convert TikTok URLs to a format that script.tokaudit.io accepts
                val formattedUrl = when {
                    // For vm.tiktok.com links (short links)
                    cleanUrl.contains("vm.tiktok.com") -> {
                        val vmUrl = cleanUrl.replace(Regex("https?://"), "https://")
                        val vmUrlWithSlash = if (!vmUrl.endsWith("/")) "$vmUrl/" else vmUrl
                        Log.d(TAG, "Formatted vm.tiktok.com URL: $vmUrlWithSlash")
                        vmUrlWithSlash
                    }
                    
                    // For www.tiktok.com/@username/video/12345 links (full links)
                    cleanUrl.contains("tiktok.com") && cleanUrl.contains("/video/") -> {
                        val videoId = cleanUrl.substringAfter("/video/").substringBefore("?")
                        val shortUrl = "https://vm.tiktok.com/$videoId/"
                        Log.d(TAG, "Converted full TikTok URL to short format: $shortUrl")
                        shortUrl
                    }
                    
                    // For any other TikTok URLs
                    cleanUrl.contains("tiktok.com") -> {
                        val httpsUrl = cleanUrl.replace(Regex("https?://"), "https://")
                        Log.d(TAG, "Using standard TikTok URL format: $httpsUrl")
                        httpsUrl
                    }
                    
                    // For non-TikTok URLs
                    else -> {
                        Log.d(TAG, "Non-TikTok URL detected: $cleanUrl")
                        cleanUrl
                    }
                }
                
                Log.d(TAG, "JavaScript requested clipboard URL. Original: $videoUrl, Cleaned: $cleanUrl, Formatted: $formattedUrl")
                return formattedUrl
            }
                    
                    @JavascriptInterface
                    fun onProcessingUpdate(isCurrentlyProcessing: Boolean) {
                        Log.d(TAG, "Processing update: $isCurrentlyProcessing")
                        
                        if (isProcessing.getAndSet(isCurrentlyProcessing) != isCurrentlyProcessing) {
                            onProcessing?.invoke(isCurrentlyProcessing)
                            
                            if (isCurrentlyProcessing) {
                                // Start the timeout timer
                                startTime = System.currentTimeMillis()
                                
                                // Schedule periodic checks
                                scheduleTimeoutCheck()
                            } else {
                                // Cancel any pending checks
                                handler.removeCallbacksAndMessages(null)
                            }
                        }
                    }
                    
                    private fun scheduleTimeoutCheck() {
                        handler.postDelayed({
                            val elapsedTime = System.currentTimeMillis() - startTime
                            if (isProcessing.get() && elapsedTime > MAX_WAIT_TIME) {
                                // Timeout reached
                                Log.w(TAG, "Transcript generation timed out after ${elapsedTime}ms")
                                
                                // Update processing state
                                if (isProcessing.getAndSet(false)) {
                                    onProcessing?.invoke(false)
                                }
                                
                                // Notify timeout
                                onError("Timeout waiting for transcript generation")
                            } else if (isProcessing.get()) {
                                // Still processing, schedule next check
                                scheduleTimeoutCheck()
                            }
                        }, TRANSCRIPT_CHECK_INTERVAL)
                    }
                },
                "Android"
            )
            
            // Set up WebViewClient
            webView.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    Log.d(TAG, "Page started loading: $url")
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "Page finished loading: $url")
                    
                    if (url?.startsWith(Constants.Urls.SCRIPT_TOKAUDIT_URL) == true) {
                        onPageLoaded()
                        
                        // Update processing state
                        if (!isProcessing.getAndSet(true)) {
                            onProcessing?.invoke(true)
                        }
                        
                        // Inject automation script with a slight delay to ensure page is ready
                        handler.postDelayed({
                            injectAutomationScript(view, videoUrl)
                        }, 1000) // Increased delay for better reliability
                    }
                }
                
                override fun onReceivedError(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        Log.e(TAG, "WebView error: ${error?.errorCode} - ${error?.description}")
                        
                        // Only handle main frame errors
                        if (request?.isForMainFrame == true) {
                            onError("WebView error: ${error?.description}")
                        }
                    } else {
                        Log.e(TAG, "WebView error received")
                        onError("WebView error received")
                    }
                }
                
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?
                ): Boolean {
                    // Allow navigation within script.tokaudit.io
                    val url = request?.url?.toString() ?: ""
                    return if (url.startsWith("https://www.script.tokaudit.io")) {
                        false // Let the WebView handle it
                    } else {
                        // For other URLs, open in browser
                        try {
                            view?.context?.startActivity(
                                Intent(Intent.ACTION_VIEW, request?.url)
                            )
                            true
                        } catch (e: Exception) {
                            Log.e(TAG, "Error opening external URL: ${e.message}")
                            false
                        }
                    }
                }
            }
            
            // Load the URL
            webView.loadUrl(Constants.Urls.SCRIPT_TOKAUDIT_URL)
            Log.d(TAG, "WebView configured and loaded URL")
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring WebView: ${e.message}")
            onError("Failed to configure WebView: ${e.message}")
        }
    }
    
    /**
     * Inject automation script into the WebView
     */
    private fun injectAutomationScript(webView: WebView?, videoUrl: String) {
        try {
            // Load the JavaScript file from assets
            val inputStream = webView?.context?.assets?.open("scripttokaudit_automation.js")
            val scriptContent = inputStream?.bufferedReader().use { it?.readText() }
            
            if (scriptContent != null) {
                // Replace the placeholder with the actual URL
                val finalScript = scriptContent.replace("{{TIKTOK_URL}}", videoUrl)
                
                // Inject the script
                webView?.evaluateJavascript(finalScript) { result ->
                    Log.d(TAG, "Script injection result: $result")
                }
                Log.d(TAG, "Injected automation script from assets")
            } else {
                Log.e(TAG, "Failed to load automation script from assets")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting automation script: ${e.message}")
        }
    }
}
