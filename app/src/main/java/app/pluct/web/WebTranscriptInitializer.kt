package app.pluct.web

import android.content.Context
import android.content.Intent
import android.util.Log
// import app.pluct.ui.utils.WebTranscriptInitializer as UtilsInitializer

/**
 * Handles WebTranscriptActivity initialization
 */
object WebTranscriptInitializer {
    private const val TAG = "WebTranscriptInitializer"
    const val EXTRA_VIDEO_ID = "video_id"
    const val EXTRA_SOURCE_URL = "source_url"
    const val EXTRA_ERROR_CODE = "error_code"
    const val EXTRA_ERROR_MESSAGE = "error_message"
    
    fun initializeActivity(
        context: Context,
        intent: Intent,
        onSuccess: (String, String) -> Unit,
        onError: (String, String) -> Unit
    ) {
        try {
            val videoId = intent.getStringExtra(EXTRA_VIDEO_ID)
            val sourceUrl = intent.getStringExtra(EXTRA_SOURCE_URL)
            
            if (videoId.isNullOrBlank() || sourceUrl.isNullOrBlank()) {
                onError("missing_data", "Missing video ID or source URL")
                return
            }
            
            onSuccess(videoId, sourceUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing activity: ${e.message}", e)
            onError("initialization_error", e.message ?: "Unknown error")
        }
    }
    
    fun createErrorIntent(errorCode: String, errorMessage: String): Intent {
        return Intent().apply {
            putExtra(EXTRA_ERROR_CODE, errorCode)
            putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
        }
    }
}