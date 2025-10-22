package app.pluct.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import app.pluct.worker.PluctTranscriptionWorker01ProgressTracker
import app.pluct.data.entity.ProcessingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Pluct-Transcription-Progress-01ViewModel - Manages transcription progress state
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation][CoreResponsibility]
 * Tracks worker progress and updates UI accordingly
 */
class PluctTranscriptionProgressViewModel : ViewModel() {
    
    private val _progressState = MutableStateFlow(TranscriptionProgressState())
    val progressState: StateFlow<TranscriptionProgressState> = _progressState.asStateFlow()
    
    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()
    
    private val _currentWorkerId: MutableStateFlow<String?> = MutableStateFlow(null)
    val currentWorkerId: StateFlow<String?> = _currentWorkerId.asStateFlow()
    
    companion object {
        private const val TAG = "PluctTranscriptionProgressViewModel"
        private const val WORKER_TAG = "transcription_progress_worker"
    }
    
    /**
     * Start transcription with progress tracking
     */
    fun startTranscription(
        context: Context,
        videoUrl: String,
        processingTier: String = "QUICK_SCAN",
        userJwt: String? = null
    ) {
        if (_isTranscribing.value) {
            Log.w(TAG, "Transcription already in progress")
            return
        }
        
        Log.i(TAG, "🎯 Starting transcription for URL: $videoUrl")
        Log.i(TAG, "Processing tier: $processingTier")
        
        _isTranscribing.value = true
        _progressState.value = TranscriptionProgressState(
            isVisible = true,
            currentStep = "health_check",
            stepNumber = 1,
            totalSteps = 5,
            progressPercent = 0,
            status = "Starting transcription..."
        )
        
        val workerId = UUID.randomUUID().toString()
        _currentWorkerId.value = workerId
        
        // Create work request
        val workRequest = OneTimeWorkRequestBuilder<PluctTranscriptionWorker01ProgressTracker>()
            .setInputData(
                workDataOf(
                    "url" to videoUrl,
                    "processingTier" to processingTier,
                    "userJwt" to (userJwt ?: ""),
                    "videoId" to UUID.randomUUID().toString(),
                    "workerId" to workerId
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1000L,
                TimeUnit.MILLISECONDS
            )
            .addTag(WORKER_TAG)
            .addTag("video_$videoUrl.hashCode()")
            .build()
        
        // Enqueue work
        WorkManager.getInstance(context).enqueue(workRequest)
        
        // Start monitoring progress
        startProgressMonitoring(context, workerId)
        
        Log.i(TAG, "✅ Transcription worker enqueued with ID: $workerId")
    }
    
    /**
     * Monitor worker progress
     */
    private fun startProgressMonitoring(context: Context, workerId: String) {
        viewModelScope.launch {
            val workManager = WorkManager.getInstance(context)
            
            // Monitor work status
            workManager.getWorkInfoByIdLiveData(UUID.fromString(workerId))
                .observeForever { workInfo ->
                    when (workInfo?.state) {
                        WorkInfo.State.RUNNING -> {
                            val progress = workInfo.progress
                            val step = progress.getString("step") ?: ""
                            val stepNumber = progress.getInt("step_number", 0)
                            val totalSteps = progress.getInt("total_steps", 5)
                            val progressPercent = progress.getInt("progress_percent", 0)
                            val status = progress.getString("status") ?: ""
                            
                            Log.d(TAG, "Progress update: $step ($stepNumber/$totalSteps) - $progressPercent%")
                            
                            _progressState.value = _progressState.value.copy(
                                currentStep = step,
                                stepNumber = stepNumber,
                                totalSteps = totalSteps,
                                progressPercent = progressPercent,
                                status = status
                            )
                        }
                        
                        WorkInfo.State.SUCCEEDED -> {
                            val outputData = workInfo.outputData
                            val transcript = outputData.getString("transcript") ?: ""
                            val jobId = outputData.getString("job_id") ?: ""
                            val duration = outputData.getLong("duration_ms", 0L)
                            
                            Log.i(TAG, "🎉 Transcription completed successfully")
                            Log.i(TAG, "Job ID: $jobId")
                            Log.i(TAG, "Duration: ${duration}ms")
                            Log.i(TAG, "Transcript length: ${transcript.length} characters")
                            
                            _progressState.value = _progressState.value.copy(
                                isVisible = false,
                                isCompleted = true,
                                transcript = transcript,
                                jobId = jobId,
                                duration = duration
                            )
                            
                            _isTranscribing.value = false
                            _currentWorkerId.value = null
                        }
                        
                        WorkInfo.State.FAILED -> {
                            val outputData = workInfo.outputData
                            val error = outputData.getString("error") ?: "Unknown error"
                            val failedStep = outputData.getString("failed_step") ?: "unknown"
                            val duration = outputData.getLong("duration_ms", 0L)
                            
                            Log.e(TAG, "❌ Transcription failed")
                            Log.e(TAG, "Error: $error")
                            Log.e(TAG, "Failed step: $failedStep")
                            Log.e(TAG, "Duration: ${duration}ms")
                            
                            _progressState.value = _progressState.value.copy(
                                isVisible = false,
                                isError = true,
                                error = error,
                                failedStep = failedStep,
                                duration = duration
                            )
                            
                            _isTranscribing.value = false
                            _currentWorkerId.value = null
                        }
                        
                        WorkInfo.State.CANCELLED -> {
                            Log.w(TAG, "Transcription cancelled")
                            _progressState.value = _progressState.value.copy(
                                isVisible = false,
                                isCancelled = true
                            )
                            _isTranscribing.value = false
                            _currentWorkerId.value = null
                        }
                        
                        else -> {
                            Log.d(TAG, "Work state: ${workInfo?.state}")
                        }
                    }
                }
        }
    }
    
    /**
     * Cancel current transcription
     */
    fun cancelTranscription(context: Context) {
        val workerId = _currentWorkerId.value
        if (workerId != null) {
            Log.i(TAG, "Cancelling transcription worker: $workerId")
            WorkManager.getInstance(context).cancelWorkById(UUID.fromString(workerId))
            
            _progressState.value = _progressState.value.copy(
                isVisible = false,
                isCancelled = true
            )
            _isTranscribing.value = false
            _currentWorkerId.value = null
        }
    }
    
    /**
     * Clear progress state
     */
    fun clearProgress() {
        _progressState.value = TranscriptionProgressState()
        _isTranscribing.value = false
        _currentWorkerId.value = null
    }
    
    /**
     * Get current progress for testing
     */
    fun getCurrentProgress(): TranscriptionProgressState {
        return _progressState.value
    }
}

/**
 * Data class for transcription progress state
 */
data class TranscriptionProgressState(
    val isVisible: Boolean = false,
    val currentStep: String = "",
    val stepNumber: Int = 0,
    val totalSteps: Int = 5,
    val progressPercent: Int = 0,
    val status: String = "",
    val isCompleted: Boolean = false,
    val isError: Boolean = false,
    val isCancelled: Boolean = false,
    val transcript: String = "",
    val jobId: String = "",
    val error: String = "",
    val failedStep: String = "",
    val duration: Long = 0L
)
