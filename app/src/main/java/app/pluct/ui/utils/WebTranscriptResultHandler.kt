package app.pluct.ui.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import app.pluct.web.WebTranscriptActivity
import app.pluct.viewmodel.IngestViewModel

/**
 * Handles WebTranscriptActivity result processing and clipboard operations
 */
object WebTranscriptResultHandler {
    
    /**
     * Process WebTranscriptActivity result and update view model accordingly
     */
    fun handleWebTranscriptResult(
        context: Context,
        resultCode: Int,
        data: Intent?,
        viewModel: IngestViewModel,
        onErrorShown: () -> Unit
    ) {
        Log.d("PluctIngest", "WebTranscriptActivity returned with resultCode: $resultCode")
        
        // Handle transcript result
        if (resultCode == android.app.Activity.RESULT_OK) {
            // Success - transcript was extracted
            val transcriptText = data?.getStringExtra("transcript_text")
            val transcriptSummary = data?.getStringExtra("transcript_summary")
            val transcriptLanguage = data?.getStringExtra("transcript_language") ?: "en"
            val autoPaste = data?.getBooleanExtra("auto_paste", false) ?: false
            
            if (transcriptText != null) {
                Log.d("PluctIngest", "Transcript received, length: ${transcriptText.length}")
                
                // Auto-paste transcript to clipboard if requested
                if (autoPaste) {
                    pasteTranscriptToClipboard(context, transcriptText)
                }
                
                // Save transcript to view model
                viewModel.saveTranscript(transcriptText, setStateToReady = false)
                viewModel.showTranscriptSuccess(transcriptText)
            }
        } else {
            // Error or cancellation
            val errorCode = data?.getStringExtra(WebTranscriptActivity.EXTRA_ERROR_CODE)
            val errorMessage = data?.getStringExtra(WebTranscriptActivity.EXTRA_ERROR_MESSAGE)
            val failurePoint = data?.getStringExtra("failure_point")
            val urlUsed = data?.getStringExtra("url_used")
            val canRetryManually = data?.getBooleanExtra("can_retry_manually", false) ?: false
            
            Log.e("PluctIngest", "WebTranscriptActivity failed: $errorCode - $errorMessage at $failurePoint")
            
            // Handle the error in view model
            viewModel.handleWebTranscriptResult(resultCode, data)
            onErrorShown()
        }
        
        // Reset the launch flag if the result was not successful
        if (resultCode != android.app.Activity.RESULT_OK) {
            Log.d("PluctIngest", "Resetting WebActivity launch flag due to unsuccessful result")
            viewModel.resetWebActivityLaunch()
        } else {
            Log.d("PluctIngest", "Keeping WebActivity launch flag set due to successful result")
        }
    }
    
    /**
     * Paste transcript text to clipboard
     */
    private fun pasteTranscriptToClipboard(context: Context, transcriptText: String) {
        try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("TikTok Transcript", transcriptText)
            clipboardManager.setPrimaryClip(clip)
            Log.i("PluctIngest", "Transcript auto-pasted to clipboard")
        } catch (e: Exception) {
            Log.e("PluctIngest", "Failed to auto-paste transcript: ${e.message}", e)
        }
    }
    
    /**
     * Launch WebTranscriptActivity with proper error handling
     */
    fun launchWebActivity(
        context: Context,
        videoId: String?,
        processedUrl: String?,
        launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
        onProcessingStart: () -> Unit
    ) {
        if (videoId != null && processedUrl != null) {
            Log.d("PluctIngest", "Launching WebTranscriptActivity for videoId: $videoId")
            onProcessingStart()
            val intent = WebTranscriptActivity.createIntent(context, videoId, processedUrl)
            launcher.launch(intent)
        } else {
            Log.e("PluctIngest", "Cannot launch WebTranscriptActivity: missing videoId or processedUrl")
        }
    }
}
