package app.pluct.ui.utils

import android.util.Log
import android.webkit.WebView
import kotlinx.coroutines.*

/**
 * Manages WebView focus to ensure proper interaction and clipboard access
 */
object WebViewFocusManager {
    private const val TAG = "WebViewFocusManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    /**
     * Ensure WebView maintains focus for clipboard operations
     */
    fun ensureFocus(webView: WebView, runId: String) {
        scope.launch {
            try {
                Log.d(TAG, "WV:A:ensuring_focus run=$runId")
                RunRingBuffer.addLog(runId, "INFO", "ensuring_focus", "WV")
                
                // Request focus on the WebView
                webView.requestFocus()
                webView.requestFocusFromTouch()
                
                // Inject JavaScript to ensure document focus
                val focusScript = """
                    (function() {
                        try {
                            document.body.focus();
                            window.focus();
                            document.activeElement?.blur();
                            document.body.focus();
                            console.log('WV:J:focus_ensured');
                        } catch(e) {
                            console.log('WV:J:focus_error=' + e.message);
                        }
                    })();
                """.trimIndent()
                
                webView.evaluateJavascript(focusScript) { result ->
                    Log.d(TAG, "WV:A:focus_script_result=$result run=$runId")
                }
                
                // Schedule periodic focus maintenance
                scheduleFocusMaintenance(webView, runId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error ensuring focus: ${e.message}", e)
                RunRingBuffer.addLog(runId, "ERROR", "focus_error: ${e.message}", "WV")
            }
        }
    }
    
    /**
     * Schedule periodic focus maintenance during automation
     */
    private fun scheduleFocusMaintenance(webView: WebView, runId: String) {
        scope.launch {
            repeat(10) { attempt ->
                delay(5000) // Every 5 seconds
                try {
                    webView.requestFocus()
                    webView.requestFocusFromTouch()
                    
                    val maintenanceScript = """
                        (function() {
                            try {
                                if (document.hasFocus && !document.hasFocus()) {
                                    document.body.focus();
                                    window.focus();
                                }
                                console.log('WV:J:focus_maintenance_attempt=${attempt + 1}');
                            } catch(e) {
                                console.log('WV:J:focus_maintenance_error=' + e.message);
                            }
                        })();
                    """.trimIndent()
                    
                    webView.evaluateJavascript(maintenanceScript, null)
                    Log.d(TAG, "WV:A:focus_maintenance_attempt=${attempt + 1} run=$runId")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Focus maintenance error: ${e.message}", e)
                }
            }
        }
    }
    
    /**
     * Prepare WebView for clipboard operations
     */
    fun prepareForClipboard(webView: WebView, runId: String) {
        scope.launch {
            try {
                Log.d(TAG, "WV:A:preparing_clipboard run=$runId")
                RunRingBuffer.addLog(runId, "INFO", "preparing_clipboard", "WV")
                
                // Ensure focus first
                ensureFocus(webView, runId)
                
                // Inject clipboard preparation script
                val clipboardPrepScript = """
                    (function() {
                        try {
                            // Create a temporary input to establish user interaction context
                            const tempInput = document.createElement('input');
                            tempInput.style.position = 'absolute';
                            tempInput.style.left = '-9999px';
                            tempInput.style.opacity = '0';
                            document.body.appendChild(tempInput);
                            tempInput.focus();
                            tempInput.select();
                            
                            // Store reference for later cleanup
                            window._tempClipboardInput = tempInput;
                            
                            console.log('WV:J:clipboard_prepared');
                        } catch(e) {
                            console.log('WV:J:clipboard_prep_error=' + e.message);
                        }
                    })();
                """.trimIndent()
                
                webView.evaluateJavascript(clipboardPrepScript) { result ->
                    Log.d(TAG, "WV:A:clipboard_prep_result=$result run=$runId")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error preparing clipboard: ${e.message}", e)
                RunRingBuffer.addLog(runId, "ERROR", "clipboard_prep_error: ${e.message}", "WV")
            }
        }
    }
    
    /**
     * Clean up temporary elements after clipboard operations
     */
    fun cleanupClipboard(webView: WebView, runId: String) {
        scope.launch {
            try {
                val cleanupScript = """
                    (function() {
                        try {
                            if (window._tempClipboardInput) {
                                document.body.removeChild(window._tempClipboardInput);
                                window._tempClipboardInput = null;
                            }
                            console.log('WV:J:clipboard_cleaned');
                        } catch(e) {
                            console.log('WV:J:clipboard_cleanup_error=' + e.message);
                        }
                    })();
                """.trimIndent()
                
                webView.evaluateJavascript(cleanupScript) { result ->
                    Log.d(TAG, "WV:A:clipboard_cleanup_result=$result run=$runId")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up clipboard: ${e.message}", e)
            }
        }
    }
    
    /**
     * Cancel all ongoing focus operations
     */
    fun cancel() {
        scope.cancel()
    }
}
