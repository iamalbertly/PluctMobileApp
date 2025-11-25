package app.pluct.services

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import app.pluct.architecture.PluctComponent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.pow
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Pluct-Core-API-01UnifiedService - Main API service orchestrator
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Single source of truth for API service functionality
 */
@Singleton
class PluctCoreAPIUnifiedService @Inject constructor(
    private val logger: PluctCoreLoggingStructuredLogger,
    private val validator: PluctCoreValidationInputSanitizer,
    private val userIdentification: PluctCoreUserIdentification
) : PluctComponent {
    
    private val jwtGenerator = PluctCoreAPIJWTGenerator()
    
    companion object {
        private const val TAG = "PluctCoreAPIUnified"
        private const val BASE_URL = "https://pluct-business-engine.romeo-lya2.workers.dev"
        private const val TIMEOUT_MS = 30000L
        private const val MAX_RETRIES = 3
        private const val BASE_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 10000L
        private const val RETRY_MULTIPLIER = 2.0
        private const val CIRCUIT_BREAKER_THRESHOLD = 5
    }
    
    private val _apiMetrics = MutableStateFlow(APIMetrics())
    val apiMetrics: StateFlow<APIMetrics> = _apiMetrics.asStateFlow()
    
    private val _healthStatus = MutableStateFlow<Map<String, HealthStatus>>(emptyMap())
    val healthStatus: StateFlow<Map<String, HealthStatus>> = _healthStatus.asStateFlow()
    
    private val json = Json { ignoreUnknownKeys = true }
    private val consecutiveFailures = AtomicInteger(0)
    private val circuitBreakerOpen = AtomicLong(0)
    
    override val componentId: String = "pluct-core-api-unified-service"
    override val dependencies: List<String> = listOf(
        "pluct-core-logging-structured-logger",
        "pluct-core-validation-input-sanitizer",
        "pluct-core-user-identification"
    )
    
    override fun initialize() {
        Log.d(TAG, "Initializing PluctCoreAPIUnifiedService")
        startHealthMonitoring()
    }
    
    override fun cleanup() {
        Log.d(TAG, "Cleaning up PluctCoreAPIUnifiedService")
    }
    
    /**
     * Core API Operations - Complete Business Engine Integration
     */
    suspend fun checkUserBalance(): Result<CreditBalanceResponse> {
        Log.d(TAG, "🔍 Checking user balance...")
        val userId = userIdentification.userId
        val userToken = jwtGenerator.generateUserJWT(userId)
        Log.d(TAG, "👤 User ID: $userId")
        
        val result = executeWithRetry("GET", "/v1/credits/balance", null, userToken)
        return if (result.isSuccess) {
            val balance = result.getOrNull() as CreditBalanceResponse
            Log.d(TAG, "💰 Balance: ${balance.balance} credits")
            Result.success(balance)
        } else {
            Log.e(TAG, "❌ Failed to get balance: ${result.exceptionOrNull()?.message}")
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }
    
    suspend fun vendToken(clientRequestId: String? = null): Result<VendTokenResponse> {
        Log.d(TAG, "🎫 Vending service token...")
        val userId = userIdentification.userId
        val userToken = jwtGenerator.generateUserJWT(userId)
        
        // Business Engine requires both userId and clientRequestId in the request body
        val payload = mapOf(
            "userId" to userId,
            "clientRequestId" to (clientRequestId ?: "req_${System.currentTimeMillis()}")
        )
        
        Log.d(TAG, "📦 Token vending payload: $payload")
        val result = executeWithRetry("POST", "/v1/vend-token", payload, userToken)
        return if (result.isSuccess) {
            val vendResponse = result.getOrNull() as VendTokenResponse
            Log.d(TAG, "✅ Token vended successfully. Balance after: ${vendResponse.balanceAfter}")
            Result.success(vendResponse)
        } else {
            Log.e(TAG, "❌ Failed to vend token: ${result.exceptionOrNull()?.message}")
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }
    
    suspend fun getMetadata(url: String): Result<MetadataResponse> {
        Log.d(TAG, "📊 Getting metadata for URL: $url")
        val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
        val result = executeWithRetry("GET", "/meta?url=$encodedUrl", null)
        return if (result.isSuccess) {
            val metadata = result.getOrNull() as MetadataResponse
            Log.d(TAG, "📋 Metadata: ${metadata.title} by ${metadata.author}")
            Result.success(metadata)
        } else {
            Log.e(TAG, "❌ Failed to get metadata: ${result.exceptionOrNull()?.message}")
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }
    
    suspend fun submitTranscriptionJob(url: String, serviceToken: String): Result<TranscriptionResponse> {
        Log.d(TAG, "🎬 Submitting transcription job for URL: $url")
        val payload = mapOf("url" to url)
        val result = executeWithRetry("POST", "/ttt/transcribe", payload, serviceToken)
        return if (result.isSuccess) {
            val transcription = result.getOrNull() as TranscriptionResponse
            Log.d(TAG, "✅ Transcription job submitted: ${transcription.jobId}")
            Result.success(transcription)
        } else {
            Log.e(TAG, "❌ Failed to submit transcription: ${result.exceptionOrNull()?.message}")
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }
    
    suspend fun checkTranscriptionStatus(jobId: String, serviceToken: String): Result<TranscriptionStatusResponse> {
        Log.d(TAG, "⏳ Checking transcription status for job: $jobId")
        val result = executeWithRetry("GET", "/ttt/status/$jobId", null, serviceToken)
        return if (result.isSuccess) {
            val status = result.getOrNull() as TranscriptionStatusResponse
            Log.d(TAG, "📊 Job status: ${status.status} (${status.progress}%)")
            Result.success(status)
        } else {
            Log.e(TAG, "❌ Failed to check status: ${result.exceptionOrNull()?.message}")
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }
    
    /**
     * Complete transcription flow - Business Engine integration
     */
    suspend fun processTikTokVideo(url: String): Result<TranscriptionStatusResponse> {
        Log.d(TAG, "🚀 Starting complete TikTok video processing flow...")
        
        return try {
            // Step 1: Get metadata
            Log.d(TAG, "Step 1: Getting video metadata...")
            val metadataResult = getMetadata(url)
            if (metadataResult.isFailure) {
                Log.e(TAG, "❌ Failed to get metadata")
                return Result.failure(metadataResult.exceptionOrNull() ?: Exception("Metadata failed"))
            }
            
            // Step 2: Vend service token
            Log.d(TAG, "Step 2: Vending service token...")
            val vendResult = vendToken()
            if (vendResult.isFailure) {
                Log.e(TAG, "❌ Failed to vend token")
                return Result.failure(vendResult.exceptionOrNull() ?: Exception("Token vending failed"))
            }
            val serviceToken = vendResult.getOrNull()!!.token
            
            // Step 3: Submit transcription job
            Log.d(TAG, "Step 3: Submitting transcription job...")
            val submitResult = submitTranscriptionJob(url, serviceToken)
            if (submitResult.isFailure) {
                Log.e(TAG, "❌ Failed to submit transcription")
                return Result.failure(submitResult.exceptionOrNull() ?: Exception("Submission failed"))
            }
            val jobId = submitResult.getOrNull()!!.jobId
            
            // Step 4: Poll for completion
            Log.d(TAG, "Step 4: Polling for completion...")
            var attempts = 0
            val maxAttempts = 20 // 5 minutes with 15-second intervals
            
            while (attempts < maxAttempts) {
                delay(15000) // Wait 15 seconds
                attempts++
                
                val statusResult = checkTranscriptionStatus(jobId, serviceToken)
                if (statusResult.isSuccess) {
                    val status = statusResult.getOrNull()!!
                    Log.d(TAG, "📊 Status check $attempts: ${status.status} (${status.progress}%)")
                    
                    if (status.status == "completed") {
                        Log.d(TAG, "✅ Transcription completed successfully!")
                        return Result.success(status)
                    } else if (status.status == "failed") {
                        Log.e(TAG, "❌ Transcription failed")
                        return Result.failure(Exception("Transcription failed"))
                    }
                } else {
                    Log.w(TAG, "⚠️ Status check failed: ${statusResult.exceptionOrNull()?.message}")
                }
            }
            
            Log.e(TAG, "⏰ Transcription timed out after $maxAttempts attempts")
            Result.failure(Exception("Transcription timed out"))
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Complete flow failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * HTTP Operations - Delegated to HTTP client
     */
    private suspend fun executeWithRetry(
        method: String,
        endpoint: String,
        payload: Map<String, Any>? = null,
        authToken: String? = null
    ): Result<Any> {
        if (isCircuitBreakerOpen()) {
            return Result.failure(Exception("Circuit breaker is open"))
        }
        
        var lastException: Exception? = null
        
        repeat(MAX_RETRIES) { attempt ->
            try {
                val result = executeHttpRequest(method, endpoint, payload, authToken)
                consecutiveFailures.set(0)
                updateMetrics(true, attempt)
                return result
            } catch (e: Exception) {
                lastException = e
                consecutiveFailures.incrementAndGet()
                updateMetrics(false, attempt)
                
                if (attempt < MAX_RETRIES - 1) {
                    val delay = calculateRetryDelay(attempt)
                    Log.w(TAG, "Request failed, retrying in ${delay}ms: ${e.message}")
                    delay(delay)
                }
            }
        }
        
        handleConsecutiveFailures()
        return Result.failure(lastException ?: Exception("Request failed after $MAX_RETRIES attempts"))
    }
    
    private suspend fun executeHttpRequest(
        method: String,
        endpoint: String,
        payload: Map<String, Any>? = null,
        authToken: String? = null
    ): Result<Any> {
        // Delegate to HTTP client for actual implementation
        val httpClient = PluctCoreAPIHTTPClientImpl(logger, validator, userIdentification)
        return httpClient.executeRequest(method, endpoint, payload, authToken)
    }
    
    /**
     * Circuit Breaker Logic
     */
    private fun isCircuitBreakerOpen(): Boolean {
        val openTime = circuitBreakerOpen.get()
        if (openTime == 0L) return false
        
        val now = System.currentTimeMillis()
        if (now - openTime > 60000) { // 1 minute
            circuitBreakerOpen.set(0)
            consecutiveFailures.set(0)
            return false
        }
        
        return true
    }
    
    private fun handleConsecutiveFailures() {
        if (consecutiveFailures.get() >= CIRCUIT_BREAKER_THRESHOLD) {
            circuitBreakerOpen.set(System.currentTimeMillis())
            Log.w(TAG, "Circuit breaker opened due to consecutive failures")
        }
    }
    
    private fun calculateRetryDelay(attempt: Int): Long {
        val delay = BASE_RETRY_DELAY_MS * RETRY_MULTIPLIER.pow(attempt)
        return kotlin.math.min(delay.toLong(), MAX_RETRY_DELAY_MS)
    }
    
    /**
     * Metrics and Health Monitoring
     */
    private fun updateMetrics(success: Boolean, attempt: Int) {
        val currentMetrics = _apiMetrics.value
        val newMetrics = currentMetrics.copy(
            totalRequests = currentMetrics.totalRequests + 1,
            successfulRequests = if (success) currentMetrics.successfulRequests + 1 else currentMetrics.successfulRequests,
            failedRequests = if (!success) currentMetrics.failedRequests + 1 else currentMetrics.failedRequests,
            averageRetries = (currentMetrics.averageRetries * (currentMetrics.totalRequests - 1) + attempt) / currentMetrics.totalRequests
        )
        _apiMetrics.value = newMetrics
    }
    
    private fun startHealthMonitoring() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val healthResult = executeHttpRequest("GET", "/health", null)
                    val health = if (healthResult.isSuccess) HealthStatus.HEALTHY else HealthStatus.UNHEALTHY
                    _healthStatus.value = mapOf("api" to health)
                } catch (e: Exception) {
                    _healthStatus.value = mapOf("api" to HealthStatus.UNHEALTHY)
                }
                delay(30000) // Check every 30 seconds
            }
        }
    }
}

/**
 * Data Classes - Single source of truth
 */
@Serializable
data class APIMetrics(
    val totalRequests: Long = 0,
    val successfulRequests: Long = 0,
    val failedRequests: Long = 0,
    val averageRetries: Double = 0.0
)

@Serializable
data class CreditBalanceResponse(
    val userId: String,
    val balance: Int,
    val updatedAt: String
)

@Serializable
data class VendTokenResponse(
    val token: String,
    val expiresIn: Int,  // Changed from expiresAt (String) to expiresIn (Int) to match actual API
    val balanceAfter: Int,
    val requestId: String
)

@Serializable
data class MetadataResponse(
    val url: String,
    val title: String,
    val description: String,
    val author: String,
    val duration: Int,
    val thumbnail: String? = null,
    val cached: Boolean? = null,
    val timestamp: String? = null,
    val handle: String? = null
)

@Serializable
data class TranscriptionResponse(
    val jobId: String,
    val status: String,
    val estimatedTime: Int? = null,  // Optional - not always returned by TTTranscribe service
    val url: String
)

@Serializable
data class TranscriptionStatusResponse(
    val jobId: String,
    val status: String,
    val progress: Int,
    val transcript: String?,
    val confidence: Double?,
    val language: String?,
    val duration: Int?
)

enum class HealthStatus {
    HEALTHY, UNHEALTHY, DEGRADED
}
