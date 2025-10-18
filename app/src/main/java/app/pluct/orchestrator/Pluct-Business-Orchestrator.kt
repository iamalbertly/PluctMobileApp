package app.pluct.orchestrator

import android.content.Context
import android.util.Log
import app.pluct.api.EngineApiProvider
import app.pluct.config.AppConfig
import app.pluct.utils.BusinessEngineHealthChecker
import app.pluct.utils.BusinessEngineCreditManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Business-Orchestrator - Live Business Engine stage management
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 * Exposes live status events from each stage of processing
 */
@Singleton
class PluctBusinessOrchestrator @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "PluctBusinessOrchestrator"
    }
    
    private val _currentStage = MutableStateFlow("IDLE")
    val currentStage: StateFlow<String> = _currentStage.asStateFlow()
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val api = EngineApiProvider.instance
    private val userId = AppConfig.userId
    
    /**
     * Start processing a video URL through the Business Engine
     */
    suspend fun processVideo(url: String): OrchestratorResult<String> {
        return try {
            _isProcessing.value = true
            _currentStage.value = "TOKEN"
            _progress.value = 0.1f
            
            Log.d(TAG, "Starting video processing for: $url")
            
            // Stage 1: Health Check
            _currentStage.value = "HEALTH_CHECK"
            _progress.value = 0.2f
            val healthResult = performHealthCheck()
            if (healthResult is OrchestratorResult.Failure) {
                _currentStage.value = "FAILED"
                _progress.value = 0f
                _isProcessing.value = false
                return healthResult
            }
            
            // Stage 2: Credit Check
            _currentStage.value = "CREDIT_CHECK"
            _progress.value = 0.3f
            val creditResult = performCreditCheck()
            if (creditResult is OrchestratorResult.Failure) {
                _currentStage.value = "FAILED"
                _progress.value = 0f
                _isProcessing.value = false
                return creditResult
            }
            
            // Stage 3: Vending Token
            _currentStage.value = "TOKEN"
            _progress.value = 0.4f
            val tokenResult = vendToken()
            if (tokenResult is OrchestratorResult.Failure) {
                _currentStage.value = "FAILED"
                _progress.value = 0f
                _isProcessing.value = false
                return tokenResult
            }
            val token = when (tokenResult) {
                is OrchestratorResult.Success -> tokenResult.data
                is OrchestratorResult.Failure -> throw Exception(tokenResult.reason)
            }
            
            // Stage 4: Submit Transcription Request
            _currentStage.value = "TRANSCRIBE"
            _progress.value = 0.5f
            val transcribeResult = submitTranscription(url, token)
            if (transcribeResult is OrchestratorResult.Failure) {
                _currentStage.value = "FAILED"
                _progress.value = 0f
                _isProcessing.value = false
                return transcribeResult
            }
            val requestId = when (transcribeResult) {
                is OrchestratorResult.Success -> transcribeResult.data
                is OrchestratorResult.Failure -> throw Exception(transcribeResult.reason)
            }
            
            // Stage 5: Poll for Status
            _currentStage.value = "SUMMARIZE"
            _progress.value = 0.6f
            val finalResult = pollForCompletion(requestId, token)
            
            _currentStage.value = "COMPLETE"
            _progress.value = 1.0f
            _isProcessing.value = false
            
            Log.d(TAG, "Video processing completed successfully")
            OrchestratorResult.Success(finalResult)
            
        } catch (e: Exception) {
            Log.e(TAG, "Video processing failed: ${e.message}", e)
            _currentStage.value = "FAILED"
            _progress.value = 0f
            _isProcessing.value = false
            OrchestratorResult.Failure(
                reason = e.message ?: "Unknown error occurred",
                logId = generateLogId(),
                errorCode = "PROCESSING_ERROR",
                retryable = true
            )
        }
    }
    
    /**
     * Poll for completion status
     */
    private suspend fun pollForCompletion(requestId: String, token: String): String {
        var attempts = 0
        val maxAttempts = 20 // 20 * 3 seconds = 1 minute timeout
        
        while (attempts < maxAttempts) {
            try {
                val response = api.status("Bearer $token", requestId)
                
                if (response.isSuccessful && response.body() != null) {
                    val status = response.body()!!
                    val statusValue = status["status"] as? String ?: "unknown"
                    
                    when (statusValue) {
                        "completed" -> {
                            _progress.value = 1.0f
                            return status["transcript"] as? String ?: "No transcript available"
                        }
                        "failed" -> {
                            throw Exception("Transcription failed: ${status["error"]}")
                        }
                        "processing" -> {
                            _progress.value = 0.6f + (attempts * 0.02f) // Gradual progress
                            delay(3000) // Wait 3 seconds before next poll
                            attempts++
                        }
                        else -> {
                            delay(3000)
                            attempts++
                        }
                    }
                } else {
                    Log.w(TAG, "Status polling failed: ${response.code()}")
                    delay(3000)
                    attempts++
                }
            } catch (e: Exception) {
                Log.w(TAG, "Status polling error: ${e.message}")
                delay(3000)
                attempts++
            }
        }
        
        throw Exception("Processing timeout after 1 minute")
    }
    
    /**
     * Reset processing state
     */
    fun reset() {
        _currentStage.value = "IDLE"
        _progress.value = 0f
        _isProcessing.value = false
    }
    
    /**
     * Get current processing status
     */
    fun getCurrentStatus(): ProcessingStatus {
        return ProcessingStatus(
            stage = _currentStage.value,
            progress = _progress.value,
            isProcessing = _isProcessing.value
        )
    }
    
    /**
     * Individual stage methods with error handling
     */
    private suspend fun performHealthCheck(): OrchestratorResult<Unit> {
        return try {
            BusinessEngineHealthChecker.performFullHealthCheck()
            OrchestratorResult.Success(Unit)
        } catch (e: Exception) {
            OrchestratorResult.Failure(
                reason = "Health check failed: ${e.message}",
                logId = generateLogId(),
                errorCode = "HEALTH_CHECK_FAILED",
                retryable = true
            )
        }
    }
    
    private suspend fun performCreditCheck(): OrchestratorResult<Unit> {
        return try {
            // Try primary credit management first
            val primaryResult = BusinessEngineCreditManager.ensureUserCredits(userId)
            if (primaryResult) {
                OrchestratorResult.Success(Unit)
            } else {
                // Fallback to fallback credit management
                Log.w(TAG, "Primary credit check failed, trying fallback...")
                val fallbackResult = BusinessEngineCreditManager.ensureUserCreditsFallback(userId)
                if (fallbackResult) {
                    OrchestratorResult.Success(Unit)
                } else {
                    OrchestratorResult.Failure(
                        reason = "Both primary and fallback credit checks failed",
                        logId = generateLogId(),
                        errorCode = "CREDIT_CHECK_FAILED",
                        retryable = false
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Credit check exception, trying fallback: ${e.message}")
            try {
                val fallbackResult = BusinessEngineCreditManager.ensureUserCreditsFallback(userId)
                if (fallbackResult) {
                    OrchestratorResult.Success(Unit)
                } else {
                    OrchestratorResult.Failure(
                        reason = "Credit check failed: ${e.message}",
                        logId = generateLogId(),
                        errorCode = "CREDIT_CHECK_FAILED",
                        retryable = false
                    )
                }
            } catch (fallbackException: Exception) {
                OrchestratorResult.Failure(
                    reason = "Credit check failed: ${e.message}",
                    logId = generateLogId(),
                    errorCode = "CREDIT_CHECK_FAILED",
                    retryable = false
                )
            }
        }
    }
    
    private suspend fun vendToken(): OrchestratorResult<String> {
        return try {
            val reqId = UUID.randomUUID().toString()
            val userJwt = System.getenv("USER_JWT") ?: ""
            val requestBody = mapOf("clientRequestId" to reqId)
            val response = api.vendToken("Bearer $userJwt", reqId, requestBody)
            if (response.isSuccessful && response.body() != null) {
                val token = response.body()!!["token"] as String
                OrchestratorResult.Success(token)
            } else {
                OrchestratorResult.Failure(
                    reason = "Token vending failed: ${response.code()}",
                    logId = generateLogId(),
                    errorCode = "TOKEN_VENDING_FAILED",
                    retryable = true
                )
            }
        } catch (e: Exception) {
            OrchestratorResult.Failure(
                reason = "Token vending failed: ${e.message}",
                logId = generateLogId(),
                errorCode = "TOKEN_VENDING_FAILED",
                retryable = true
            )
        }
    }
    
    private suspend fun submitTranscription(url: String, token: String): OrchestratorResult<String> {
        return try {
            val requestBody = mapOf("url" to url)
            val response = api.transcribe("Bearer $token", requestBody)
            if (response.isSuccessful && response.body() != null) {
                val requestId = response.body()!!["request_id"] as String
                OrchestratorResult.Success(requestId)
            } else {
                OrchestratorResult.Failure(
                    reason = "Transcription submission failed: ${response.code()}",
                    logId = generateLogId(),
                    errorCode = "TRANSCRIPTION_SUBMISSION_FAILED",
                    retryable = true
                )
            }
        } catch (e: Exception) {
            OrchestratorResult.Failure(
                reason = "Transcription submission failed: ${e.message}",
                logId = generateLogId(),
                errorCode = "TRANSCRIPTION_SUBMISSION_FAILED",
                retryable = true
            )
        }
    }
    
    private fun generateLogId(): String = UUID.randomUUID().toString()
}

data class ProcessingStatus(
    val stage: String,
    val progress: Float,
    val isProcessing: Boolean
)
