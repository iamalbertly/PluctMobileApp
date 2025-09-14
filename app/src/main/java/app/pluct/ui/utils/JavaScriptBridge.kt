package app.pluct.ui.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Simplified JavaScript interface for WebView communication
 */
class JavaScriptBridge(
    private val onTranscriptReceived: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val TAG = "JavaScriptBridge"
    private val scope = CoroutineScope(Dispatchers.Main)
    
    @android.webkit.JavascriptInterface
    fun onTranscript(transcript: String?) {
            scope.launch {
                try {
                transcript?.let { text ->
                    if (text.isNotEmpty()) {
                        Log.d(TAG, "WV:A:transcript_received length=${text.length}")
                        RunRingBuffer.addLog("", "INFO", "transcript_received length=${text.length}", "WV")
                        onTranscriptReceived(text)
                        }
                    }
                } catch (e: Exception) {
                Log.e(TAG, "Error processing transcript: ${e.message}", e)
                onError("transcript_error")
            }
        }
    }
    
    @android.webkit.JavascriptInterface
    fun onTranscriptComplete() {
        scope.launch {
            try {
                Log.d(TAG, "WV:A:transcript_complete")
                RunRingBuffer.addLog("", "INFO", "transcript_complete", "WV")
                // Transcript processing is complete
            } catch (e: Exception) {
                Log.e(TAG, "Error completing transcript: ${e.message}", e)
                onError("transcript_complete_error")
            }
        }
    }
    
    @android.webkit.JavascriptInterface
    fun onError(error: String) {
            scope.launch {
                try {
                Log.e(TAG, "WV:A:js_error=$error")
                RunRingBuffer.addLog("", "ERROR", "js_error=$error", "WV")
                    onError(error)
                } catch (e: Exception) {
                Log.e(TAG, "Error handling JS error: ${e.message}", e)
                }
        }
    }
    
    @android.webkit.JavascriptInterface
    fun onLogMessage(message: String) {
            scope.launch {
                try {
                val msg = message ?: ""
                val levelPrefix = if (msg.length >= 2 && msg[1] == ':') msg.substring(0, 2) else "I:"
                val body = if (msg.length >= 2 && msg[1] == ':') msg.substring(2) else msg
                when (levelPrefix) {
                    "E:" -> {
                        Log.e("WVConsoleErr", "WVConsoleErr:" + body)
                        RunRingBuffer.addLog("", "ERROR", body, "WVConsoleErr")
                    }
                    "W:" -> {
                        Log.w("WVConsoleWarn", "WVConsoleWarn:" + body)
                        RunRingBuffer.addLog("", "WARN", body, "WVConsoleWarn")
                    }
                    "D:" -> {
                        Log.d("WVConsoleDbg", "WVConsoleDbg:" + body)
                        RunRingBuffer.addLog("", "INFO", body, "WVConsoleDbg")
                    }
                    "L:" -> {
                        Log.i("WVConsoleLog", "WVConsoleLog:" + body)
                        RunRingBuffer.addLog("", "INFO", body, "WVConsoleLog")
                    }
                    else -> {
                        Log.i("WVConsole", "WVConsole:" + body)
                        RunRingBuffer.addLog("", "INFO", body, "WVConsole")
                    }
                }
                } catch (e: Exception) {
                Log.e(TAG, "Error logging message: ${e.message}", e)
                }
        }
    }
    
    @android.webkit.JavascriptInterface
    fun onHtmlSnapshot(html: String?) {
            scope.launch {
                try {
                val content = (html ?: "").take(2000)
                Log.d(TAG, "WV:A:html_snapshot len=${content.length}")
                Log.d("WVConsole", "WVConsole:html_head=" + content.replace("\n", " "))
                } catch (e: Exception) {
                Log.e(TAG, "Error logging HTML snapshot: ${e.message}", e)
                }
        }
    }
    
    @android.webkit.JavascriptInterface
    fun disablePerformanceBlocker() {
                scope.launch {
                    try {
                Log.d(TAG, "WV:A:disable_performance_blocker")
                RunRingBuffer.addLog("", "INFO", "disable_performance_blocker", "WV")
                // Performance blocker disabled
                    } catch (e: Exception) {
                Log.e(TAG, "Error disabling performance blocker: ${e.message}", e)
            }
        }
    }
}