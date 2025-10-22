package app.pluct.ui.utils

import android.webkit.WebView
import app.pluct.ui.utils.scripts.PluctWebViewScriptInjection

/**
 * Pluct-WebView-Script-Coordinator - Simplified coordinator for WebView scripts
 */
object PluctWebViewScriptCoordinator {
    
    fun injectAutomationScript(
        webView: WebView, 
        videoUrl: String, 
        runId: String
    ) {
        PluctWebViewScriptInjection.injectAutomationScript(webView, videoUrl, runId)
    }
}
