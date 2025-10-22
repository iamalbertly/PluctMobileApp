package app.pluct.data.transcription

import app.pluct.core.error.ErrorCenter
import app.pluct.core.error.ErrorEnvelope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Data-Transcription-01ProgressTracker - Transcription progress tracking
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@Singleton
class PluctTranscriptionProgressTracker @Inject constructor(
    private val errorCenter: ErrorCenter
) {
    private val _progress = MutableStateFlow<Map<String, TranscriptionProgress>>(emptyMap())
    val progress: StateFlow<Map<String, TranscriptionProgress>> = _progress.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    fun startTracking(jobId: String, videoId: String) {
        _progress.value = _progress.value + (jobId to TranscriptionProgress(
            jobId = jobId,
            videoId = videoId,
            status = TranscriptionStatus.QUEUED,
            progress = 0,
            estimatedTime = 0,
            startTime = System.currentTimeMillis()
        ))
    }
    
    fun updateProgress(jobId: String, progress: Int, status: TranscriptionStatus) {
        val currentProgress = _progress.value[jobId]
        if (currentProgress != null) {
            _progress.value = _progress.value + (jobId to currentProgress.copy(
                progress = progress,
                status = status,
                lastUpdate = System.currentTimeMillis()
            ))
        }
    }
    
    fun completeTranscription(jobId: String, transcript: String) {
        val currentProgress = _progress.value[jobId]
        if (currentProgress != null) {
            _progress.value = _progress.value + (jobId to currentProgress.copy(
                status = TranscriptionStatus.COMPLETED,
                progress = 100,
                transcript = transcript,
                completedAt = System.currentTimeMillis()
            ))
        }
    }
    
    fun failTranscription(jobId: String, error: String) {
        val currentProgress = _progress.value[jobId]
        if (currentProgress != null) {
            _progress.value = _progress.value + (jobId to currentProgress.copy(
                status = TranscriptionStatus.FAILED,
                error = error,
                failedAt = System.currentTimeMillis()
            ))
        }
        
        errorCenter.emitError(
            ErrorEnvelope(
                code = "TRANSCRIPTION_FAILED",
                message = "Transcription failed: $error",
                details = mapOf("jobId" to jobId)
            )
        )
    }
    
    fun getProgress(jobId: String): TranscriptionProgress? {
        return _progress.value[jobId]
    }
    
    fun clearProgress(jobId: String) {
        _progress.value = _progress.value - jobId
    }
    
    fun clearAllProgress() {
        _progress.value = emptyMap()
    }
}

data class TranscriptionProgress(
    val jobId: String,
    val videoId: String,
    val status: TranscriptionStatus,
    val progress: Int,
    val estimatedTime: Int,
    val startTime: Long,
    val lastUpdate: Long = startTime,
    val completedAt: Long? = null,
    val failedAt: Long? = null,
    val transcript: String? = null,
    val error: String? = null
)

enum class TranscriptionStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED
}
