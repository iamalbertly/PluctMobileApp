package app.pluct.ui.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelStoreOwner
import app.pluct.ui.viewmodels.TokAuditViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import android.webkit.WebView
import app.pluct.R

/**
 * Utility class for TokAudit flow URL resolution and validation
 */
object TokAuditUtils {
    private const val TAG = "TokAuditUtils"
    
    // Intent extra keys
    const val EXTRA_TOK_URL = "tok_url"
    const val EXTRA_RUN_ID = "run_id"
    
    // Bundle keys
    const val BUNDLE_VIDEO_URL = "video_url"
    const val BUNDLE_RUN_ID = "run_id"
    
    // Legacy expected test URL (no longer enforced)
    const val EXPECTED_TEST_URL = "https://vm.tiktok.com/ZMA2MTD9C"
    
    /**
     * Generate a new run ID and immediately log it
     */
    fun generateRunId(): String {
        val runId = java.util.UUID.randomUUID().toString()
        Log.d(TAG, "WV:A:run_id=$runId")
        RunRingBuffer.addLog(runId, "INFO", "run_id=$runId", "WV")
        return runId
    }
    
    /**
     * Validate URL is acceptable (non-blank).
     */
    fun validateTestUrl(url: String, runId: String): Boolean {
        val ok = url.isNotBlank()
        if (!ok) {
            Log.e(TAG, "WV:A:fatal_blank_url run=$runId")
            RunRingBuffer.addLog(runId, "ERROR", "fatal_blank_url", "WV")
        }
        return ok
    }
    
    /**
     * Validate URL is not blank
     */
    fun validateUrlNotBlank(url: String, runId: String): Boolean {
        if (url.isBlank()) {
            Log.e(TAG, "WV:A:fatal_blank_url run=$runId")
            RunRingBuffer.addLog(runId, "ERROR", "fatal_blank_url", "WV")
            return false
        }
        return true
    }
    
    /**
     * Log URL access
     */
    fun logUrlAccess(url: String, runId: String) {
        Log.d(TAG, "WV:A:url=$url run=$runId")
        RunRingBuffer.addLog(runId, "INFO", "url=$url", "WV")
    }
    
    /**
     * Tag WebView with videoUrl and runId
     */
    fun tagWebView(webView: WebView, videoUrl: String, runId: String) {
        webView.setTag(R.id.tag_video_url, videoUrl)
        webView.setTag(R.id.tag_run_id, runId)
        Log.d(TAG, "WV:A:webview_tagged url=$videoUrl run=$runId")
        RunRingBuffer.addLog(runId, "INFO", "webview_tagged url=$videoUrl", "WV")
    }
    
    /**
     * Add URL and run ID to intent extras
     */
    fun addToIntentExtras(intent: Intent, videoUrl: String, runId: String) {
        intent.putExtra(EXTRA_TOK_URL, videoUrl)
        intent.putExtra(EXTRA_RUN_ID, runId)
        Log.d(TAG, "WV:A:intent_extras_added url=$videoUrl run=$runId")
        RunRingBuffer.addLog(runId, "INFO", "intent_extras_added url=$videoUrl", "WV")
    }
    
    /**
     * Save URL and run ID to savedInstanceState
     */
    fun saveToInstanceState(outState: Bundle, videoUrl: String, runId: String) {
        outState.putString(BUNDLE_VIDEO_URL, videoUrl)
        outState.putString(BUNDLE_RUN_ID, runId)
        Log.d(TAG, "WV:A:saved_instance_state url=$videoUrl run=$runId")
        RunRingBuffer.addLog(runId, "INFO", "saved_instance_state url=$videoUrl", "WV")
    }
    
    /**
     * Resolve video URL by precedence: savedInstanceState → ViewModel → Intent
     */
    fun resolveVideoUrl(
        savedInstanceState: Bundle?,
        viewModel: TokAuditViewModel?,
        intent: Intent?,
        runId: String
    ): String? {
        // 1. Try savedInstanceState first
        savedInstanceState?.getString(BUNDLE_VIDEO_URL)?.let { url ->
            if (validateUrlNotBlank(url, runId)) {
                logUrlAccess(url, runId)
                Log.d(TAG, "WV:A:resolved_from_saved_instance url=$url run=$runId")
                RunRingBuffer.addLog(runId, "INFO", "resolved_from_saved_instance url=$url", "WV")
                return url
            }
        }
        
        // 2. Try ViewModel
        viewModel?.getVideoUrl()?.let { url ->
            if (validateUrlNotBlank(url, runId)) {
                logUrlAccess(url, runId)
                Log.d(TAG, "WV:A:resolved_from_viewmodel url=$url run=$runId")
                RunRingBuffer.addLog(runId, "INFO", "resolved_from_viewmodel url=$url", "WV")
                return url
            }
        }
        
        // 3. Try Intent extras
        intent?.getStringExtra(EXTRA_TOK_URL)?.let { url ->
            if (validateUrlNotBlank(url, runId)) {
                logUrlAccess(url, runId)
                Log.d(TAG, "WV:A:resolved_from_intent url=$url run=$runId")
                RunRingBuffer.addLog(runId, "INFO", "resolved_from_intent url=$url", "WV")
                return url
            }
        }
        
        // No valid URL found
        Log.e(TAG, "WV:A:no_valid_url_found run=$runId")
        RunRingBuffer.addLog(runId, "ERROR", "no_valid_url_found", "WV")
        return null
    }
    
    /**
     * Resolve run ID by precedence: savedInstanceState → ViewModel → Intent
     */
    fun resolveRunId(
        savedInstanceState: Bundle?,
        viewModel: TokAuditViewModel?,
        intent: Intent?
    ): String? {
        // 1. Try savedInstanceState first
        savedInstanceState?.getString(BUNDLE_RUN_ID)?.let { runId ->
            if (runId.isNotEmpty()) {
                Log.d(TAG, "WV:A:resolved_run_id_from_saved_instance run=$runId")
                return runId
            }
        }
        
        // 2. Try ViewModel
        viewModel?.getRunId()?.let { runId ->
            if (runId.isNotEmpty()) {
                Log.d(TAG, "WV:A:resolved_run_id_from_viewmodel run=$runId")
                return runId
            }
        }
        
        // 3. Try Intent extras
        intent?.getStringExtra(EXTRA_RUN_ID)?.let { runId ->
            if (runId.isNotEmpty()) {
                Log.d(TAG, "WV:A:resolved_run_id_from_intent run=$runId")
                return runId
            }
        }
        
        // No valid run ID found
        Log.e(TAG, "WV:A:no_valid_run_id_found")
        return null
    }
    
    /**
     * Initialize ViewModel with URL and run ID
     */
    fun initializeViewModel(viewModel: TokAuditViewModel?, videoUrl: String, runId: String) {
        viewModel?.initialize(videoUrl, runId)
        Log.d(TAG, "WV:A:viewmodel_initialized url=$videoUrl run=$runId")
        RunRingBuffer.addLog(runId, "INFO", "viewmodel_initialized url=$videoUrl", "WV")
    }
    
    /**
     * Complete URL validation and resolution
     */
    fun validateAndResolveUrl(
        savedInstanceState: Bundle?,
        viewModel: TokAuditViewModel?,
        intent: Intent?,
        runId: String
    ): String? {
        val videoUrl = resolveVideoUrl(savedInstanceState, viewModel, intent, runId)
        
        if (videoUrl == null) {
            Log.e(TAG, "WV:A:fatal_blank_url run=$runId")
            RunRingBuffer.addLog(runId, "ERROR", "fatal_blank_url", "WV")
            return null
        }
        // No strict test URL check anymore
        
        return videoUrl
    }
}
