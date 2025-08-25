package app.pluct.web

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import app.pluct.data.entity.ArtifactKind
import app.pluct.data.repository.PluctRepository
import app.pluct.data.service.VideoMetadataExtractor
import app.pluct.ui.theme.PluctTheme
import app.pluct.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class WebTranscriptActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "PluctWeb"
        const val EXTRA_VIDEO_ID = "video_id"
        const val EXTRA_SOURCE_URL = "source_url"
        const val EXTRA_ERROR_CODE = "error_code"
        const val EXTRA_ERROR_MESSAGE = "error_message"
        
        fun createIntent(context: Context, videoId: String, sourceUrl: String): Intent {
            return Intent(context, WebTranscriptActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_ID, videoId)
                putExtra(EXTRA_SOURCE_URL, sourceUrl)
            }
        }
    }
    
    @Inject
    lateinit var repository: PluctRepository
    
    @Inject
    lateinit var metadataExtractor: VideoMetadataExtractor
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val startTime = System.currentTimeMillis()
    private var isTranscriptCompleted = false
    private var isErrorHandled = false
    private lateinit var clipboardManager: ClipboardManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "WebTranscriptActivity.onCreate() called")
        
        try {
            // Initialize clipboard manager
            clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            Log.d(TAG, "Clipboard manager initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize clipboard manager: ${e.message}", e)
        }
        
        val videoId = intent.getStringExtra(EXTRA_VIDEO_ID)
        var sourceUrl = intent.getStringExtra(EXTRA_SOURCE_URL)
        
        Log.d(TAG, "Extracted videoId: $videoId")
        Log.d(TAG, "Extracted sourceUrl: $sourceUrl")
        
        if (videoId.isNullOrBlank() || sourceUrl.isNullOrBlank()) {
            Log.e(TAG, "Missing required extras: videoId=$videoId, sourceUrl=$sourceUrl")
            setResult(RESULT_CANCELED, Intent().apply {
                putExtra(EXTRA_ERROR_CODE, "invalid_args")
                putExtra(EXTRA_ERROR_MESSAGE, "Missing video ID or source URL")
            })
            finish()
            return
        }
        
        // Clean up URL if it contains deep link format
        if (sourceUrl.contains("url=")) {
            try {
                val cleanedUrl = android.net.Uri.decode(sourceUrl.substringAfter("url="))
                Log.d(TAG, "Cleaned up URL from deep link format: $cleanedUrl")
                sourceUrl = cleanedUrl
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clean URL from deep link format: ${e.message}", e)
                // Continue with the original URL
            }
        }
        
        // Save original URL to clipboard as backup
        try {
            saveToClipboard("Original TikTok URL", sourceUrl!!) // Non-null assertion is safe here because we checked above
            Log.i(TAG, "Original URL saved to clipboard successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save original URL to clipboard: ${e.message}", e)
        }
        
        Log.i(TAG, "Starting WebTranscriptActivity for videoId=$videoId, sourceUrl=$sourceUrl")
        
        // Extract video metadata in background
        coroutineScope.launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "Starting metadata extraction for URL: $sourceUrl")
                val metadata = metadataExtractor.extractVideoMetadata(sourceUrl!!)
                Log.d(TAG, "Extracted video metadata: $metadata")
                
                // Save metadata to repository
                if (metadata.isNotEmpty()) {
                    repository.saveArtifact(
                        videoId = videoId,
                        kind = ArtifactKind.METADATA,
                        content = metadata,
                        filename = "video_metadata.json",
                        mime = "application/json"
                    )
                    Log.i(TAG, "Video metadata saved successfully")
                } else {
                    Log.w(TAG, "Metadata extraction returned empty result")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting video metadata: ${e.message}", e)
            }
        }
        
        setContent {
            PluctTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    WebTranscriptScreen(
                        videoId = videoId,
                        videoUrl = sourceUrl!!, // Non-null assertion is safe here because we checked above
                        onTranscriptReady = { transcript ->
                            handleTranscriptReady(videoId, transcript, "en")
                        },
                        onError = { error ->
                            handleTranscriptError("error", error)
                        },
                        onBackPressed = {
                            finish()
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Save text to clipboard
     */
    private fun saveToClipboard(label: String, text: String) {
        try {
            if (::clipboardManager.isInitialized) {
                val clip = ClipData.newPlainText(label, text)
                clipboardManager.setPrimaryClip(clip)
                Log.i(TAG, "Saved to clipboard: $label - ${text.take(50)}...")
            } else {
                Log.e(TAG, "Clipboard manager not initialized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to clipboard: ${e.message}", e)
        }
    }
    
    // Metadata extraction moved to VideoMetadataExtractor class
    
    private fun handleTranscriptReady(videoId: String, transcript: String, language: String?) {
        if (isTranscriptCompleted) {
            Log.w(TAG, "Transcript already completed, ignoring duplicate call")
            return
        }
        
        isTranscriptCompleted = true
        val duration = System.currentTimeMillis() - startTime
        Log.i(TAG, "Transcript ready after ${duration}ms, length: ${transcript.length}")
        
        // Save to clipboard
        try {
            saveToClipboard("TikTok Transcript", transcript)
            Toast.makeText(this, "Transcript copied to clipboard", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Transcript saved to clipboard successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save transcript to clipboard: ${e.message}", e)
        }
        
        // Generate a simple summary for notification
        val summary = generateSimpleSummary(transcript)
        Log.d(TAG, "Generated summary: $summary")
        
        // Save to database
        coroutineScope.launch {
            try {
                repository.saveTranscript(videoId, transcript, language ?: "en")
                Log.i(TAG, "Transcript saved to database")
                
                // Save as artifact
                repository.saveArtifact(
                    videoId = videoId,
                    kind = ArtifactKind.TRANSCRIPT,
                    content = transcript,
                    filename = "transcript.txt",
                    mime = "text/plain"
                )
                Log.i(TAG, "Transcript saved as artifact")
                
                // Set result and finish with transcript data to auto-paste
                withContext(Dispatchers.Main) {
                    // Return the transcript in the result intent so the main activity can use it
                    setResult(RESULT_OK, Intent().apply {
                        putExtra("transcript_text", transcript)
                        putExtra("transcript_summary", summary)
                        putExtra("transcript_language", language ?: "en")
                        putExtra("auto_paste", true) // Flag to indicate we want to auto-paste
                    })
                    
                    Log.d(TAG, "Returning to main activity with transcript for auto-paste")
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving transcript: ${e.message}", e)
                handleTranscriptError("save_error", "Failed to save transcript: ${e.message}")
            }
        }
    }
    
    private fun handleTranscriptError(errorCode: String, errorMessage: String) {
        if (isErrorHandled) {
            Log.w(TAG, "Error already handled, ignoring duplicate call")
            return
        }
        
        isErrorHandled = true
        Log.e(TAG, "Transcript error: $errorCode - $errorMessage")
        
        val videoId = intent.getStringExtra(EXTRA_VIDEO_ID)
        val sourceUrl = intent.getStringExtra(EXTRA_SOURCE_URL)
        
        // Check if this is an invalid URL error
        val isInvalidUrlError = errorMessage.contains("Invalid URL", ignoreCase = true) || 
                               errorMessage.contains("valid link", ignoreCase = true)
        
        // Check if this is a no transcript error
        val isNoTranscriptError = errorMessage.contains("No transcript", ignoreCase = true) || 
                                 errorMessage.contains("Subtitles Not Available", ignoreCase = true)
        
        // Extract URL from error message if available
        val urlMatch = errorMessage.let {
            val regex = "URL used: '([^']+)'".toRegex()
            regex.find(it)?.groupValues?.get(1)
        }
        
        // Determine the error type and user-friendly message
        val userMessage = when {
            isInvalidUrlError -> {
                val urlInfo = if (urlMatch != null) {
                    "\n\nURL used: '$urlMatch'"
                } else if (sourceUrl != null) {
                    "\n\nURL used: '$sourceUrl'"
                } else {
                    ""
                }
                "Invalid TikTok URL. Please try a different video link.$urlInfo"
            }
            isNoTranscriptError -> "No transcript available for this video. The video may not have captions."
            errorMessage.contains("timeout", ignoreCase = true) -> "Request timed out. Please check your internet connection and try again."
            else -> "Error processing transcript: $errorMessage"
        }
        
        // Save URL to clipboard for manual testing if it's an invalid URL error
        if (isInvalidUrlError) {
            val urlToCopy = urlMatch ?: sourceUrl
            if (urlToCopy != null) {
                saveToClipboard("TikTok URL for manual testing", urlToCopy)
                Log.i(TAG, "Saved URL to clipboard for manual testing: $urlToCopy")
            }
            
            // Mark this URL as invalid in the database if we have a videoId
            if (videoId != null) {
                coroutineScope.launch {
                    try {
                        // Save the invalid status to the database
                        repository.markUrlAsInvalid(videoId, sourceUrl ?: "", errorMessage)
                        Log.i(TAG, "Marked URL as invalid in database: $sourceUrl")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to mark URL as invalid: ${e.message}", e)
                    }
                }
            }
        }
        
        setResult(RESULT_CANCELED, Intent().apply {
            putExtra(EXTRA_ERROR_CODE, errorCode)
            putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
            // Add flag to indicate error type
            if (isInvalidUrlError) {
                putExtra("is_invalid_url", true)
            }
            if (isNoTranscriptError) {
                putExtra("is_no_transcript", true)
            }
            // Add the URL that was used
            putExtra("url_used", urlMatch ?: sourceUrl)
        })
        
        val totalTime = System.currentTimeMillis() - startTime
        Log.i(TAG, "Web automation failed after ${totalTime}ms: $errorCode")
        
        // Show toast with user-friendly message
        Toast.makeText(this, "Error: See dialog for details", Toast.LENGTH_LONG).show()
        
        // Show a dialog for better user feedback with options
        showErrorDialog(userMessage, urlMatch ?: sourceUrl)
    }
    
    private fun showErrorDialog(message: String, url: String? = null) {
        try {
            val builder = android.app.AlertDialog.Builder(this)
                .setTitle("Transcript Error")
                .setMessage(message)
                
            if (url != null) {
                // Add option to try manually in browser
                builder.setNeutralButton("Try in Browser") { _, _ ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error opening URL in browser: ${e.message}", e)
                        Toast.makeText(this, "Failed to open browser", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }
            }
            
            builder.setPositiveButton("Return to Main Page") { _, _ ->
                finish()
            }
            .setCancelable(false)
            
            builder.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing dialog: ${e.message}", e)
            finish()
        }
    }
    
    private fun openInExternalBrowser(url: String) {
        try {
            Log.d(TAG, "Opening URL in external browser: $url")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            
            setResult(RESULT_CANCELED, Intent().apply {
                putExtra(EXTRA_ERROR_CODE, "browser_fallback")
                putExtra(EXTRA_ERROR_MESSAGE, "Opened in external browser due to WebView issues")
            })
            finish()
        } catch (e: android.content.ActivityNotFoundException) {
            Log.e(TAG, "No browser app found to open URL", e)
            Toast.makeText(this, "No browser app found", Toast.LENGTH_SHORT).show()
            handleTranscriptError("no_browser", "No browser app found to open URL")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening external browser: ${e.message}", e)
            handleTranscriptError("browser_error", "Error opening external browser: ${e.message}")
        }
    }
    
    private fun generateSimpleSummary(transcript: String): String {
        val sentences = transcript.split(Regex("[.!?]+")).filter { it.trim().isNotEmpty() }
        return when {
            sentences.size <= 4 -> transcript
            else -> {
                val firstTwo = sentences.take(2).joinToString(". ") + "."
                val lastTwo = sentences.takeLast(2).joinToString(". ") + "."
                "$firstTwo ... $lastTwo"
            }
        }
    }
    
    /**
     * Handle error from WebTranscriptScreen
     */
    private fun handleError(_videoId: String, errorMessage: String) {
        handleTranscriptError("transcript_error", errorMessage)
    }
}
