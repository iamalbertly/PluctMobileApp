package app.pluct.ui.utils.scripts

import android.util.Log
import android.webkit.WebView
import org.json.JSONObject

/**
 * Pluct-WebView-Script-Injection - Core script injection functionality
 */
object PluctWebViewScriptInjection {
    private const val TAG = "PluctWebViewScriptInjection"

    fun injectAutomationScript(
        webView: WebView, 
        videoUrl: String, 
        runId: String
    ) {
        try {
            Log.d(TAG, "WV:A:injecting_script url=$videoUrl run=$runId")
            app.pluct.ui.utils.RunRingBuffer.addLog(runId, "INFO", "injecting_script url=$videoUrl", "WV")
            
            val script = PluctWebViewScriptBuilder.buildAutomationScript(videoUrl, runId)
            Log.d(TAG, "WV:A:script_built length=${script.length} run=$runId")
            
            webView.evaluateJavascript(script) { result ->
                Log.d(TAG, "WV:A:script_injected result=$result run=$runId")
                app.pluct.ui.utils.RunRingBuffer.addLog(runId, "INFO", "script_injected result=$result", "WV")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting automation script: ${e.message}", e)
            app.pluct.ui.utils.RunRingBuffer.addLog(runId, "ERROR", "script_injection_error: ${e.message}", "WV")
        }
    }
}
