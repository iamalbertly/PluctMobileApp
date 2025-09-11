package app.pluct.ui.utils

import android.util.Log
import android.webkit.*
import app.pluct.R

/**
 * WebView settings configuration utilities
 */
object WebViewSettings {
    private const val TAG = "WebViewSettings"
    
    /**
     * Apply standard WebView settings for modern web apps
     */
    fun applyStandardSettings(webView: WebView, runId: String) {
        webView.settings.apply {
            // Enable JavaScript and DOM storage (ESSENTIAL for React apps)
            javaScriptEnabled = true
            domStorageEnabled = true

            // Set modern mobile user agent that works better with script.tokaudit.io
            userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

            // Enable modern web features
            allowFileAccess = true
            allowContentAccess = true
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            // Enable modern JavaScript features
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)

            // Enable modern CSS features
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false

            // Enable modern storage features
            databaseEnabled = true

            // Enable modern security features
            allowUniversalAccessFromFileURLs = true
            allowFileAccessFromFileURLs = true

            // Enable modern rendering features
            cacheMode = WebSettings.LOAD_DEFAULT

            // Enable modern media features
            mediaPlaybackRequiresUserGesture = false
            setGeolocationEnabled(true)
        }

        // Configure CookieManager to accept cookies and third-party cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        Log.d(TAG, "WV:A:webview_configured run=$runId")
    }
}
