package app.pluct.web

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import app.pluct.ui.theme.PluctTheme
import app.pluct.ui.screens.WebTranscriptScreen
import app.pluct.ui.components.ScriptTokAuditWebView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.UUID
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
    lateinit var transcriptHandler: TranscriptHandler
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val startTime = System.currentTimeMillis()
    private var isErrorHandled = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "WebTranscriptActivity.onCreate() called")
        
        // Initialize activity with proper validation
        WebTranscriptInitializer.initializeActivity(
            context = this,
            intent = intent,
            onSuccess = { videoId, sourceUrl ->
                setupWebView(videoId, sourceUrl)
            },
            onError = { errorCode, errorMessage ->
                setResult(RESULT_CANCELED, WebTranscriptInitializer.createErrorIntent(errorCode, errorMessage))
                finish()
            }
        )
    }
    
    private fun setupWebView(videoId: String, sourceUrl: String) {
        // Resolve videoUrl and runId in order: intent extras
        val videoUrl = intent.getStringExtra("tok_url") ?: sourceUrl
        val runId = intent.getStringExtra("run_id") ?: UUID.randomUUID().toString()
            
        Log.d(TAG, "WV:A:run_id=$runId")
        
        // Check for blank URL
        if (videoUrl.isBlank()) {
            Log.e(TAG, "WV:A:fatal_blank_url run=$runId")
            showFatalBlankUrlDialog(runId)
            return
        }
        
        // Accept any TikTok URL
        Log.d(TAG, "WV:A:url=$videoUrl run=$runId")
        
        // Save original URL to clipboard as backup
        try {
            transcriptHandler.saveToClipboard("Original TikTok URL", videoUrl)
            Log.i(TAG, "Original URL saved to clipboard successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save original URL to clipboard: ${e.message}", e)
        }
        
        Log.i(TAG, "Starting WebTranscriptActivity for videoId=$videoId, videoUrl=$videoUrl, runId=$runId")
        
        setContent {
            PluctTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScriptTokAuditWebView(
                        videoUrl = videoUrl,
                        runId = runId,
                        onTranscriptReceived = { transcript ->
                            val providerUsed = app.pluct.ui.utils.ProviderSettings.getSelectedProvider(this).name
                            transcriptHandler.handleTranscriptReady(videoId, transcript, "en", startTime, providerUsed)
                            // Close WebView once transcript is saved
                            try {
                                Log.i(TAG, "WV:A:transcript_persisted_and_closing run=$runId")
                                finish()
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to close after transcript: ${e.message}", e)
                            }
                        },
                        onError = { errorCode ->
                            // Human-readable message
                            val readable = when (errorCode) {
                                "invalid_data" -> "TokAudit reported: No valid TikTok data for this link."
                                "invalid_url" -> "The link appears invalid for transcription."
                                "no_subtitles" -> "No subtitles available for this video."
                                else -> "Provider reported an error: $errorCode"
                            }
                            // Ask user to try next provider if available
                            val available = app.pluct.ui.utils.ProviderSettings.getAvailableProviders(this)
                            val current = app.pluct.ui.utils.ProviderSettings.getSelectedProvider(this)
                            val next = available.dropWhile { it == current }.firstOrNull()
                            if (next != null) {
                                android.app.AlertDialog.Builder(this)
                                    .setTitle("Try another provider?")
                                    .setMessage(readable + "\n\nWould you like to try " + next.name + " instead?")
                                    .setPositiveButton("Try " + next.name) { _, _ ->
                                        app.pluct.ui.utils.ProviderSettings.setSelectedProvider(this, next)
                                        // Relaunch current activity with same params
                                        val relaunch = createIntent(this, videoId, videoUrl).apply {
                                            putExtra("run_id", runId)
                                        }
                                        startActivity(relaunch)
                                        finish()
                                    }
                                    .setNegativeButton("Cancel") { _, _ ->
                                        // Return error to caller
                                        setResult(RESULT_CANCELED, WebTranscriptInitializer.createErrorIntent(errorCode, readable))
                                        finish()
                                    }
                                    .setCancelable(false)
                                    .show()
                            } else {
                                // No other providers enabled, return error
                                setResult(RESULT_CANCELED, WebTranscriptInitializer.createErrorIntent(errorCode, readable))
                                finish()
                            }
                        },
                        onSaveTranscript = { runId, videoUrl, transcript ->
                            // Save transcript via repository
                            // This will be handled by the JavaScriptBridge
                        },
                        onClose = {
                            finish()
                        }
                    )
                }
            }
        }
    }
    
    private fun showFatalBlankUrlDialog(runId: String) {
        setContent {
            PluctTheme {
                Surface {
                    AlertDialog(
                        onDismissRequest = { /* Block dismissal */ },
                        title = { Text("Fatal Error") },
                        text = { 
                            Text(
                                "Blank URL detected. This should not happen.\n\n" +
                                "Run ID: $runId"
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { finish() }) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Removed wrong URL dialog; proceed with provider automation
    
    private fun handleTranscriptError(errorCode: String, errorMessage: String) {
        if (isErrorHandled) return
        isErrorHandled = true
        
        Log.e(TAG, "Transcript error: $errorCode - $errorMessage")
        
        setResult(RESULT_CANCELED, WebTranscriptInitializer.createErrorIntent(errorCode, errorMessage))
        finish()
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save videoUrl and runId to savedInstanceState
        intent.getStringExtra("tok_url")?.let { outState.putString("videoUrl", it) }
        intent.getStringExtra("run_id")?.let { outState.putString("runId", it) }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.coroutineContext.cancel()
    }
}

// Removed legacy dialog