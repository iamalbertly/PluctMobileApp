package app.pluct.ui.utils

import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import app.pluct.R
import app.pluct.utils.Constants

/**
 * WebView configuration utilities - extracted from WebViewUtils
 */
object WebViewConfig {
    private const val TAG = "WebViewConfig"
    
    /**
     * Configure WebView settings for optimal performance
     */
    fun configureSettings(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
            
            // Minimal cookie management
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.setAcceptThirdPartyCookies(webView, true)
            }
            
            // Allow mixed content for TokAudit
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            
            // Speed optimizations - disable heavy features
            cacheMode = WebSettings.LOAD_NO_CACHE
            allowFileAccess = false
            allowContentAccess = false
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            
            // Disable heavy features for speed
            databaseEnabled = false
            setGeolocationEnabled(false)
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            
            // Ensure images are not blocked (site may rely on them)
            blockNetworkImage = false
            blockNetworkLoads = false
        }
        
        Log.d(TAG, "WebView settings configured for speed")
    }
    
    /**
     * Set WebView tags for automation
     */
    fun setWebViewTags(webView: WebView, videoUrl: String, runId: String) {
        webView.setTag(R.id.tag_video_url, videoUrl)
        webView.setTag(R.id.tag_run_id, runId)
        Log.d(TAG, "WebView tags set: url=$videoUrl runId=$runId")
    }
}
