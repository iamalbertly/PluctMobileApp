package app.pluct.ui.screens

import android.util.Log
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.TranscriptionStatusResponse
import app.pluct.ui.models.TranscriptionPhase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Pluct-UI-Screen-01MainActivity-03ProgressMonitor - Monitor and update transcription progress
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 * Single source of truth for transcription progress monitoring
 * 
 * Enhanced with speed illusion tactics:
 * - Optimistic UI updates (immediate progress start)
 * - Progressive status messages
 * - Intelligent adaptive polling
 * - Smooth progress animations
 */
object PluctUIScreen01MainActivity03ProgressMonitor {
    
    private const val TAG = "ProgressMonitor"
    
    // Adaptive polling intervals for speed optimization
    private const val POLL_INTERVAL_INITIAL_MS = 2000L // 2s for first 30s - catch quick completions
    private const val POLL_INTERVAL_ACTIVE_MS = 3000L // 3s for 30s-2min - standard processing
    private const val POLL_INTERVAL_EXTENDED_MS = 5000L // 5s for 2min+ - longer jobs
    private const val POLL_INTERVAL_BACKOFF_MS = 10000L // 10s after 5min - very long jobs
    
    private const val TIMEOUT_WARNING_THRESHOLD = 0.8f // Show warning at 80% of timeout
    private const val OPTIMISTIC_PROGRESS_START = 10 // Start at 10% immediately
    private const val PROGRESS_MICRO_UPDATE_MS = 500L // Micro-updates every 500ms
    
    // Phase thresholds for progressive status messages
    private const val PHASE_PREPARING_MAX = 15
    private const val PHASE_DOWNLOADING_MAX = 40
    private const val PHASE_EXTRACTING_MAX = 60
    private const val PHASE_TRANSCRIBING_MAX = 90
    
    /**
     * Progress update data class with enhanced fields
     */
    data class ProgressUpdate(
        val jobId: String,
        val status: String,
        val progress: Int,
        val currentOperation: String,
        val estimatedTimeRemaining: String? = null,
        val showTimeoutWarning: Boolean = false,
        val phase: TranscriptionPhase = TranscriptionPhase.PREPARING,
        val isOptimistic: Boolean = false // True if progress is optimistic (not from API)
    )
    
    
    /**
     * Monitor transcription progress with optimistic UI and intelligent polling
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
            var lastProgress = OPTIMISTIC_PROGRESS_START // Start optimistically
            var optimisticProgressJob: Job? = null
            var lastKnownProgress = 0
            var lastKnownPhase = TranscriptionPhase.PREPARING
            
            // SPEED ILLUSION: Immediate optimistic progress start
            onProgressUpdate(
                ProgressUpdate(
                    jobId = jobId,
                    status = "processing",
                    progress = OPTIMISTIC_PROGRESS_START,
                    currentOperation = getPhaseMessage(TranscriptionPhase.PREPARING),
                    estimatedTimeRemaining = formatTimeRemaining(maxDurationMs),
                    showTimeoutWarning = false,
                    phase = TranscriptionPhase.PREPARING,
                    isOptimistic = true
                )
            )
            
            // Start optimistic progress animation (micro-updates)
            optimisticProgressJob = launch {
                var currentOptimistic = OPTIMISTIC_PROGRESS_START
                while (isActive && currentOptimistic < 99) {
                    delay(PROGRESS_MICRO_UPDATE_MS)
                    val elapsed = System.currentTimeMillis() - startTime
                    val timeBasedProgress = ((elapsed.toFloat() / maxDurationMs) * 100).toInt().coerceIn(0, 99)
                    
                    // Only update if optimistic progress is less than actual or time-based
                    // This ensures progress never goes backwards
                    val newProgress = maxOf(currentOptimistic, lastKnownProgress, timeBasedProgress)
                    if (newProgress > currentOptimistic && newProgress < 99) {
                        currentOptimistic = newProgress
                        val phase = getPhaseFromProgress(currentOptimistic)
                        onProgressUpdate(
                            ProgressUpdate(
                                jobId = jobId,
                                status = "processing",
                                progress = currentOptimistic,
                                currentOperation = getPhaseMessage(phase),
                                estimatedTimeRemaining = formatTimeRemaining(maxDurationMs - elapsed),
                                showTimeoutWarning = (elapsed.toFloat() / maxDurationMs) >= TIMEOUT_WARNING_THRESHOLD,
                                phase = phase,
                                isOptimistic = true
                            )
                        )
                    }
                }
            }
            
            try {
                // First poll immediately (no delay) for cache hits
                delay(100) // Tiny delay to let optimistic UI render
                
                while (isActive) {
                    val elapsedTime = System.currentTimeMillis() - startTime
                    
                    // Adaptive polling interval based on elapsed time
                    val pollInterval = getAdaptivePollInterval(elapsedTime)
                    val showTimeoutWarning = (elapsedTime.toFloat() / maxDurationMs) >= TIMEOUT_WARNING_THRESHOLD
                    
                    // Check status
                    val statusResult = apiService.checkTranscriptionStatus(jobId, serviceToken)
                    
                    if (statusResult.isSuccess) {
                        val status = statusResult.getOrNull()!!
                        attempt++
                        
                        // Extract actual progress from API
                        val apiProgress = status.progress ?: lastKnownProgress
                        val apiPhase = getPhaseFromProgress(apiProgress)
                        
                        // Use API progress if available, otherwise use optimistic
                        val finalProgress = if (apiProgress > 0) {
                            lastKnownProgress = apiProgress
                            lastKnownPhase = apiPhase
                            apiProgress
                        } else {
                            // Continue with optimistic progress
                            lastKnownProgress
                        }
                        
                        // Update with actual progress from API
                        onProgressUpdate(
                            ProgressUpdate(
                                jobId = jobId,
                                status = status.status,
                                progress = finalProgress.coerceIn(0, 99),
                                currentOperation = getStatusMessage(status.status, apiPhase),
                                estimatedTimeRemaining = formatTimeRemaining(maxDurationMs - elapsedTime),
                                showTimeoutWarning = showTimeoutWarning,
                                phase = apiPhase,
                                isOptimistic = false
                            )
                        )
                        
                        when (status.status) {
                            "completed" -> {
                                optimisticProgressJob?.cancel()
                                onProgressUpdate(
                                    ProgressUpdate(
                                        jobId = jobId,
                                        status = "completed",
                                        progress = 100,
                                        currentOperation = "Transcription complete!",
                                        estimatedTimeRemaining = null,
                                        showTimeoutWarning = false,
                                        phase = TranscriptionPhase.COMPLETED,
                                        isOptimistic = false
                                    )
                                )
                                onComplete(status)
                                return@launch
                            }
                            "failed" -> {
                                optimisticProgressJob?.cancel()
                                val errorMsg = status.transcript ?: "Transcription failed"
                                onError(errorMsg)
                                return@launch
                            }
                        }
                    } else {
                        // Status check failed, but continue polling with optimistic progress
                        Log.w(TAG, "Status check failed: ${statusResult.exceptionOrNull()?.message}")
                    }
                    
                    // Check timeout
                    if (elapsedTime >= maxDurationMs) {
                        optimisticProgressJob?.cancel()
                        val errorMsg = "Transcription timed out after ${maxDurationMs / 1000} seconds"
                        Log.e(TAG, errorMsg)
                        onError(errorMsg)
                        return@launch
                    }
                    
                    // Wait for next poll with adaptive interval
                    delay(pollInterval)
                }
                
            } catch (e: Exception) {
                optimisticProgressJob?.cancel()
                Log.e(TAG, "Progress monitoring error: ${e.message}", e)
                onError("Progress monitoring failed: ${e.message ?: "Unknown error"}")
            }
        }
    }
    
    /**
     * Get adaptive polling interval based on elapsed time
     */
    private fun getAdaptivePollInterval(elapsedMs: Long): Long {
        return when {
            elapsedMs < 30000 -> POLL_INTERVAL_INITIAL_MS // First 30s: 2s intervals
            elapsedMs < 120000 -> POLL_INTERVAL_ACTIVE_MS // 30s-2min: 3s intervals
            elapsedMs < 300000 -> POLL_INTERVAL_EXTENDED_MS // 2min-5min: 5s intervals
            else -> POLL_INTERVAL_BACKOFF_MS // After 5min: 10s intervals
        }
    }
    
    /**
     * Get phase from progress percentage
     */
    private fun getPhaseFromProgress(progress: Int): TranscriptionPhase {
        return when {
            progress < PHASE_PREPARING_MAX -> TranscriptionPhase.PREPARING
            progress < PHASE_DOWNLOADING_MAX -> TranscriptionPhase.DOWNLOADING
            progress < PHASE_EXTRACTING_MAX -> TranscriptionPhase.EXTRACTING
            progress < PHASE_TRANSCRIBING_MAX -> TranscriptionPhase.TRANSCRIBING
            progress < 99 -> TranscriptionPhase.FINALIZING
            else -> TranscriptionPhase.COMPLETED
        }
    }
    
    /**
     * Get phase-specific message
     */
    private fun getPhaseMessage(phase: TranscriptionPhase): String {
        return when (phase) {
            TranscriptionPhase.PREPARING -> "Preparing transcription..."
            TranscriptionPhase.DOWNLOADING -> "Downloading video from TikTok..."
            TranscriptionPhase.EXTRACTING -> "Extracting audio..."
            TranscriptionPhase.TRANSCRIBING -> "Transcribing with AI..."
            TranscriptionPhase.FINALIZING -> "Finalizing..."
            TranscriptionPhase.COMPLETED -> "Transcription complete!"
        }
    }
    
    /**
     * Get status message combining API status and phase
     */
    private fun getStatusMessage(status: String, phase: TranscriptionPhase): String {
        return when (status) {
            "queued" -> "Waiting to start..."
            "processing" -> getPhaseMessage(phase)
            "completed" -> "Transcription complete!"
            "failed" -> "Transcription failed"
            else -> getPhaseMessage(phase)
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

