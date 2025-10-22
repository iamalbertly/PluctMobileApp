package app.pluct.ui.utils

import android.net.Uri
import android.util.Log
import android.webkit.*
import android.net.http.SslError
import app.pluct.R

/**
 * Factory for creating WebViewClient instances
 */
object WebViewClientFactory {
    private const val TAG = "WebViewClientFactory"
    
    /**
     * Create a WebViewClient with SSL fixes and automation script injection
     */
    fun createSSLWebViewClient(runId: String): WebViewClient {
        Log.d(TAG, "WV:A:creating_webview_client run=$runId")
        return object : WebViewClient() {
            private var alreadyInjected = false
            
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                val host = Uri.parse(error?.url ?: "").host.orEmpty().lowercase()
                val ok = host == "tokaudit.io" || host.endsWith(".tokaudit.io")
                
                if (ok) {
                    handler?.proceed()
                    Log.d(TAG, "WV:A:ssl_proceed host=$host run=$runId")
                } else {
                    handler?.cancel()
                    Log.d(TAG, "WV:A:ssl_cancel host=$host run=$runId")
                }
            }
            
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "WV:A:page_started url=$url run=$runId")
                alreadyInjected = false
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "WV:A:page_finished url=$url run=$runId")
                
                // Check if we should inject automation script
                val currentUrl = view?.url
                val hostOk = try {
                    val h = android.net.Uri.parse(currentUrl).host?.lowercase().orEmpty()
                    h == "tokaudit.io" || h.endsWith(".tokaudit.io")
                } catch (_: Exception) { false }
                if (view != null && hostOk &&
                    currentUrl != "about:blank" && 
                    !alreadyInjected) {
                    
                    // Post delayed injection (600-900ms as specified)
                    val v = view
                    v.postDelayed({
                        try {
                            // Get videoUrl and runId from WebView tags
                            val videoUrl = v.getTag(R.id.tag_video_url) as? String ?: ""
                            val currentRunId = v.getTag(R.id.tag_run_id) as? String ?: runId
                            Log.d(TAG, "WebViewClientFactory: Retrieved tags - videoUrl: '$videoUrl', runId: '$currentRunId'")
                            
                            if (videoUrl.isNotEmpty()) {
                                Log.d(TAG, "WebViewClientFactory: Injecting script with videoUrl: $videoUrl")
                                PluctWebViewScriptCoordinator.injectAutomationScript(v, videoUrl, currentRunId)
                                alreadyInjected = true
                                Log.d(TAG, "WV:A:inject_auto run=$currentRunId")
                            } else {
                                Log.d(TAG, "WV:A:skip_inject reason=blank_url run=$currentRunId")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "WV:A:script_injection_failed run=$runId error=${e.message}", e)
                        }
                    }, 750) // 750ms delay (within 600-900ms range)
                } else {
                    val reason = when {
                        !hostOk -> "not_script"
                        view?.url == "about:blank" -> "about_blank"
                        alreadyInjected -> "already_injected"
                        else -> "unknown"
                    }
                    Log.d(TAG, "WV:A:skip_inject reason=$reason run=$runId")
                }
            }
            
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                Log.e(TAG, "WV:A:webview_error run=$runId code=${error?.errorCode} desc=${error?.description}")
            }
        }
    }
}
