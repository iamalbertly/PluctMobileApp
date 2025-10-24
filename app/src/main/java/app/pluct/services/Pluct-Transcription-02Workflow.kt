package app.pluct.services

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Transcription-02Workflow - Complete transcription workflow with TTTranscribe API
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation][CoreResponsibility]
 * Orchestrates the complete transcription process from URL to final transcript
 */
@Singleton
class PluctTranscriptionWorkflow @Inject constructor(
    private val apiCommunication: PluctBusinessEngineAPICommunication,
    private val jwtManager: PluctAuthenticationJWTManager,
    private val errorHandling: PluctErrorHandlingRetryLogic,
    private val statusMonitoring: PluctStatusMonitoringRealTime,
    private val metadataExtractor: PluctMetadataExtractor
) {
    
    companion object {
        private const val TAG = "PluctTranscriptionWorkflow"
        private const val WORKFLOW_TIMEOUT_MS = 300000L // 5 minutes
    }
    
    data class TranscriptionRequest(
        val url: String,
        val userId: String = "mobile",
        val clientRequestId: String? = null
    )
    
    data class TranscriptionResult(
        val success: Boolean,
        val jobId: String? = null,
        val transcript: String? = null,
        val confidence: Double? = null,
        val language: String? = null,
        val duration: Int? = null,
        val error: String? = null,
        val processingTimeMs: Long = 0
    )
    
    data class WorkflowStep(
        val step: String,
        val status: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
        val data: Map<String, Any>? = null
    )
    
    private val workflowSteps = MutableSharedFlow<WorkflowStep>()
    private val activeWorkflows = mutableMapOf<String, Job>()
    
    /**
     * Execute complete transcription workflow
     */
    suspend fun executeTranscription(
        request: TranscriptionRequest
    ): Flow<WorkflowStep> = flow {
        
        val startTime = System.currentTimeMillis()
        val workflowId = "workflow_${System.currentTimeMillis()}"
        
        Log.d(TAG, "🚀 Starting transcription workflow for URL: ${request.url}")
        Log.d(TAG, "   Workflow ID: $workflowId")
        Log.d(TAG, "   User ID: ${request.userId}")
        
        // Emit workflow start
        emit(WorkflowStep("workflow_start", "started", "Starting transcription workflow", System.currentTimeMillis(), mapOf(
            "workflowId" to workflowId,
            "url" to request.url,
            "userId" to request.userId
        )))
        
        try {
            // Step 1: Health Check
            emit(WorkflowStep("health_check", "started", "Checking system health..."))
            val healthResult = executeHealthCheck()
            if (!healthResult) {
                throw Exception("Health check failed")
            }
            emit(WorkflowStep("health_check", "completed", "System health verified"))
            
            // Step 2: Get User Balance
            emit(WorkflowStep("balance_check", "started", "Checking user balance..."))
            val balanceResult = executeBalanceCheck(request.userId)
            if (!balanceResult) {
                throw Exception("Balance check failed")
            }
            emit(WorkflowStep("balance_check", "completed", "User balance verified"))
            
            // Step 3: Get Metadata (Optional)
            emit(WorkflowStep("metadata_extraction", "started", "Extracting video metadata..."))
            val metadataResult = executeMetadataExtraction(request.url)
            emit(WorkflowStep("metadata_extraction", "completed", "Metadata extracted successfully"))
            
            // Step 4: Token Vending
            emit(WorkflowStep("token_vending", "started", "Exchanging credits for transcription token..."))
            val token = executeTokenVending(request.userId, request.clientRequestId)
            if (token == null) {
                throw Exception("Token vending failed")
            }
            emit(WorkflowStep("token_vending", "completed", "Transcription token obtained"))
            
            // Step 5: Start Transcription
            emit(WorkflowStep("transcription_start", "started", "Starting transcription job..."))
            val jobId = executeTranscriptionStart(request.url, token)
            if (jobId == null) {
                throw Exception("Transcription start failed")
            }
            emit(WorkflowStep("transcription_start", "completed", "Transcription job started: $jobId"))
            
            // Step 6: Monitor Progress
            emit(WorkflowStep("progress_monitoring", "started", "Monitoring transcription progress..."))
            val finalResult = executeProgressMonitoring(jobId, token)
            
            val processingTime = System.currentTimeMillis() - startTime
            emit(WorkflowStep("workflow_complete", "completed", "Transcription workflow completed", System.currentTimeMillis(), mapOf(
                "processingTimeMs" to processingTime,
                "success" to finalResult.success,
                "transcript" to (finalResult.transcript ?: "")
            )))
            
            Log.d(TAG, "✅ Transcription workflow completed in ${processingTime}ms")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Transcription workflow failed: ${e.message}", e)
            
            val processingTime = System.currentTimeMillis() - startTime
            emit(WorkflowStep("workflow_error", "failed", "Transcription workflow failed: ${e.message}", System.currentTimeMillis(), mapOf(
                "processingTimeMs" to processingTime,
                "error" to (e.message ?: "")
            )))
            
            throw e
        } finally {
            // Clean up active workflow
            activeWorkflows.remove(workflowId)
        }
    }
    
    /**
     * Execute health check
     */
    private suspend fun executeHealthCheck(): Boolean {
        return try {
            val result = apiCommunication.performHealthCheck()
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Execute balance check
     */
    private suspend fun executeBalanceCheck(userId: String): Boolean {
        return try {
            val result = apiCommunication.checkUserBalance(userId)
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Execute metadata extraction
     */
    private suspend fun executeMetadataExtraction(url: String): Boolean {
        return try {
            val metadata = metadataExtractor.extractMetadata(url)
            true
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Metadata extraction failed (non-critical): ${e.message}")
            false
        }
    }
    
    /**
     * Execute token vending
     */
    private suspend fun executeTokenVending(userId: String, clientRequestId: String?): String? {
        return try {
            val result = apiCommunication.vendToken(userId, clientRequestId)
            if (result.isSuccess) {
                result.getOrNull()?.token
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Execute transcription start
     */
    private suspend fun executeTranscriptionStart(url: String, shortLivedToken: String): String? {
        return try {
            val result = apiCommunication.startTranscription(shortLivedToken, url)
            if (result.isSuccess) {
                result.getOrNull()?.jobId
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Execute progress monitoring
     */
    private suspend fun executeProgressMonitoring(jobId: String, shortLivedToken: String): TranscriptionResult {
        Log.d(TAG, "🔄 Starting progress monitoring for job: $jobId")
        
        return try {
            // Start monitoring
            val statusFlow = statusMonitoring.startMonitoring(jobId, shortLivedToken)
            
            // Collect status updates
            var finalResult: TranscriptionResult? = null
            
            statusFlow.collect { statusUpdate ->
                Log.d(TAG, "📊 Status update: ${statusUpdate.status} (${statusUpdate.progress}%)")
                
                // Log progress update (can't emit from here as we're not in a flow context)
                Log.d(TAG, "📊 Progress update: ${statusUpdate.message}")
                
                // Check if complete
                if (statusUpdate.status == "completed") {
                    finalResult = TranscriptionResult(
                        success = true,
                        jobId = jobId,
                        transcript = statusUpdate.transcript,
                        confidence = statusUpdate.confidence,
                        language = statusUpdate.language,
                        duration = statusUpdate.duration
                    )
                    return@collect
                }
                
                // Check for errors
                if (statusUpdate.status == "failed" || statusUpdate.error != null) {
                    finalResult = TranscriptionResult(
                        success = false,
                        jobId = jobId,
                        error = statusUpdate.error ?: "Transcription failed"
                    )
                    return@collect
                }
            }
            
            // Stop monitoring
            statusMonitoring.stopMonitoring(jobId)
            
            finalResult ?: TranscriptionResult(
                success = false,
                jobId = jobId,
                error = "Monitoring timeout"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Progress monitoring failed: ${e.message}", e)
            
            // Stop monitoring
            statusMonitoring.stopMonitoring(jobId)
            
            TranscriptionResult(
                success = false,
                jobId = jobId,
                error = e.message ?: "Progress monitoring failed"
            )
        }
    }
    
    
    /**
     * Cancel active workflow
     */
    suspend fun cancelWorkflow(workflowId: String) = withContext(Dispatchers.IO) {
        Log.d(TAG, "🛑 Cancelling workflow: $workflowId")
        
        activeWorkflows[workflowId]?.cancel()
        activeWorkflows.remove(workflowId)
    }
    
    /**
     * Get active workflow count
     */
    fun getActiveWorkflowCount(): Int {
        return activeWorkflows.size
    }
    
    /**
     * Get workflow statistics
     */
    fun getWorkflowStats(): Map<String, Any> {
        return mapOf(
            "activeWorkflows" to activeWorkflows.size,
            "workflowIds" to activeWorkflows.keys.toList()
        )
    }
}
