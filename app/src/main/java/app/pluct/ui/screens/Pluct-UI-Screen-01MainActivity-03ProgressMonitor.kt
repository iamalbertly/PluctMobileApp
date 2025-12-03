package app.pluct.ui.screens

import android.util.Log
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.TranscriptionStatusResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Pluct-UI-Screen-01MainActivity-03ProgressMonitor - Monitor and update transcription progress
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 * Single source of truth for transcription progress monitoring
 */
object PluctUIScreen01MainActivity03ProgressMonitor {
    
    private const val TAG = "ProgressMonitor"
    private const val POLL_INTERVAL_MS = 5000L // 5 seconds
    private const val TIMEOUT_WARNING_THRESHOLD = 0.8f // Show warning at 80% of timeout
    
    /**
     * Progress update data class
     */
    data class ProgressUpdate(
        val jobId: String,
        val status: String,
        val progress: Int,
        val currentOperation: String,
        val estimatedTimeRemaining: String? = null,
        val showTimeoutWarning: Boolean = false
    )
    
    /**
     * Monitor transcription progress
     */
    fun monitorProgress(
        scope: CoroutineScope,
        apiService: PluctCoreAPIUnifiedService,
        jobId: String,
        serviceToken: String,
        maxDurationMs: Long = 300000L, // 5 minutes default
        onProgressUpdate: (ProgressUpdate) -> Unit,
        onComplete: (TranscriptionStatusResponse) -> Unit,
        onError: (String) -> Unit
    ): Job {
        return scope.launch {
            val startTime = System.currentTimeMillis()
            var attempt = 0
            val maxAttempts = (maxDurationMs / POLL_INTERVAL_MS).toInt()
            
            try {
                while (attempt < maxAttempts) {
                    delay(POLL_INTERVAL_MS)
                    attempt++
                    
                    val elapsedTime = System.currentTimeMillis() - startTime
                    val progressPercent = ((elapsedTime.toFloat() / maxDurationMs) * 100).toInt().coerceIn(0, 99)
                    val showTimeoutWarning = (elapsedTime.toFloat() / maxDurationMs) >= TIMEOUT_WARNING_THRESHOLD
                    
                    // Update progress
                    onProgressUpdate(
                        ProgressUpdate(
                            jobId = jobId,
                            status = "processing",
                            progress = progressPercent,
                            currentOperation = "Checking transcription status...",
                            estimatedTimeRemaining = formatTimeRemaining(maxDurationMs - elapsedTime),
                            showTimeoutWarning = showTimeoutWarning
                        )
                    )
                    
                    // Check status
                    val statusResult = apiService.checkTranscriptionStatus(jobId, serviceToken)
                    
                    if (statusResult.isSuccess) {
                        val status = statusResult.getOrNull()!!
                        
                        // Update with actual progress from API
                        onProgressUpdate(
                            ProgressUpdate(
                                jobId = jobId,
                                status = status.status,
                                progress = status.progress ?: progressPercent,
                                currentOperation = when (status.status) {
                                    "queued" -> "Waiting to start..."
                                    "processing" -> "Transcribing video..."
                                    "completed" -> "Transcription complete!"
                                    "failed" -> "Transcription failed"
                                    else -> "Processing..."
                                },
                                estimatedTimeRemaining = formatTimeRemaining(maxDurationMs - elapsedTime),
                                showTimeoutWarning = showTimeoutWarning
                            )
                        )
                        
                        when (status.status) {
                            "completed" -> {
                                onComplete(status)
                                return@launch
                            }
                            "failed" -> {
                                val errorMsg = status.transcript ?: "Transcription failed"
                                onError(errorMsg)
                                return@launch
                            }
                        }
                    } else {
                        // Status check failed, but continue polling
                        Log.w(TAG, "Status check failed: ${statusResult.exceptionOrNull()?.message}")
                    }
                }
                
                // Timeout reached
                val errorMsg = "Transcription timed out after ${maxDurationMs / 1000} seconds"
                Log.e(TAG, errorMsg)
                onError(errorMsg)
                
            } catch (e: Exception) {
                Log.e(TAG, "Progress monitoring error: ${e.message}", e)
                onError("Progress monitoring failed: ${e.message ?: "Unknown error"}")
            }
        }
    }
    
    /**
     * Format time remaining in human-readable format
     */
    private fun formatTimeRemaining(ms: Long): String {
        val seconds = (ms / 1000).toInt()
        return when {
            seconds < 60 -> "$seconds seconds"
            seconds < 3600 -> "${seconds / 60} minutes"
            else -> "${seconds / 3600} hours"
        }
    }
}

