package app.pluct.ui.utils

import android.util.Log
import android.webkit.WebView
import app.pluct.utils.Constants

/**
 * WebView configuration utilities
 */
object WebViewConfiguration {
    private const val TAG = "WebViewConfiguration"
    
    /**
     * Configure WebView with optimal settings for transcript extraction
     */
    fun configureWebView(webView: WebView, runId: String) {
        try {
            Log.d(TAG, "Configuring WebView for run: $runId")
            
            // Enable JavaScript
            webView.settings.javaScriptEnabled = true
            
            // Set user agent for better compatibility
            webView.settings.userAgentString = Constants.WebView.USER_AGENT
            
            // Enable DOM storage
            webView.settings.domStorageEnabled = true
            
            // Enable database storage
            webView.settings.databaseEnabled = true
            
            // Set cache mode for better performance
            webView.settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            
            // Set maximum cache size (deprecated in newer Android versions)
            // webView.settings.setAppCacheMaxSize(Constants.WebView.MAX_CACHE_SIZE)
            
            // Enable mixed content (for HTTPS/HTTP mixed content)
            webView.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            
            // Enable file access
            webView.settings.allowFileAccess = true
            webView.settings.allowContentAccess = true
            
            // Disable zoom for better mobile experience
            webView.settings.setSupportZoom(false)
            webView.settings.builtInZoomControls = false
            webView.settings.displayZoomControls = false
            
            // Set viewport meta tag support
            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = true
            
            // Enable hardware acceleration
            webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
            
            Log.d(TAG, "WebView configured successfully for run: $runId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring WebView: ${e.message}", e)
        }
    }
}