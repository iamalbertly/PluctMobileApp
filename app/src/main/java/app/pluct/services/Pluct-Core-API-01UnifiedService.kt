package app.pluct.services

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import app.pluct.architecture.PluctComponent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.pow
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Pluct-Core-API-01UnifiedService - Single source of truth for all API communications
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Consolidated service with built-in HTTP client, retry logic, and circuit breaker
 */
@Singleton
class PluctCoreAPIUnifiedService @Inject constructor(
    private val logger: PluctCoreLoggingStructuredLogger,
    private val validator: PluctCoreValidationInputSanitizer,
    private val userIdentification: PluctCoreUserIdentification
) : PluctComponent {
    
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
    private val lastFailureTime = AtomicLong(0)
    
    override val componentId: String = "pluct-core-api-unified-service"
    override val dependencies: List<String> = emptyList()
    
    override fun initialize() {
        Log.d(TAG, "Initializing PluctCoreAPIUnifiedService")
    }
    
    override fun cleanup() {
        Log.d(TAG, "Cleaning up PluctCoreAPIUnifiedService")
    }
    
    /**
     * Generate JWT token for user authentication
     */
    private fun generateUserJWT(userId: String): String {
        val now = System.currentTimeMillis() / 1000
        val payload = mapOf(
            "sub" to userId,
            "scope" to "ttt:transcribe",
            "iat" to now,
            "exp" to (now + 900) // 15 minutes
        )
        
        // Simplified JWT generation (in production, use proper JWT library)
        val header = """{"alg":"HS256","typ":"JWT"}"""
        val payloadJson = """{"sub":"$userId","scope":"ttt:transcribe","iat":$now,"exp":${now + 900}}"""
        
        val headerEncoded = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(header.toByteArray())
        val payloadEncoded = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.toByteArray())
        
        val signature = "$headerEncoded.$payloadEncoded"
        return signature // Simplified - in production, add proper HMAC signature
    }
    private suspend fun executeRequest(
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        // Check circuit breaker
        val currentMetrics = _apiMetrics.value
        if (currentMetrics.circuitBreakerOpen) {
            logger.logError(TAG, "Circuit breaker is open, rejecting request", 
                context = mapOf("url" to url, "method" to method))
            return@withContext Result.failure(Exception("Circuit breaker is open"))
        }
        
        var lastException: Exception? = null
        
        repeat(MAX_RETRIES) { attempt ->
            var connection: HttpURLConnection? = null
            try {
                val startTime = System.currentTimeMillis()
                connection = URL(url).openConnection() as HttpURLConnection
                
                connection.requestMethod = method
                connection.connectTimeout = TIMEOUT_MS.toInt()
                connection.readTimeout = TIMEOUT_MS.toInt()
                
                // Add default headers
                connection.setRequestProperty("User-Agent", "PluctMobileApp/1.0")
                connection.setRequestProperty("Accept", "application/json")
                
                headers.forEach { (key, value) ->
                    connection.setRequestProperty(key, value)
                }
                
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    OutputStreamWriter(connection.outputStream).use { writer ->
                        writer.write(body)
                    }
                }
                
                val responseCode = connection.responseCode
                val responseBody = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream)).use { reader ->
                        reader.readText()
                    }
                }
                
                val endTime = System.currentTimeMillis()
                val responseTime = endTime - startTime
                
                updateMetrics(true, responseTime)
                
                if (responseCode in 200..299) {
                    Log.d(TAG, "Request successful: $method $url (${responseTime}ms)")
                    return@withContext Result.success(responseBody)
                } else {
                    val error = ApiError(responseCode, responseBody, url, method)
                    logger.logError(TAG, "Request failed: $error", context = mapOf(
                        "url" to url, "method" to method, "responseCode" to responseCode.toString()
                    ))
                    
                    // Don't retry on client errors (4xx) except 429 (rate limit)
                    if (responseCode in 400..499 && responseCode != 429) {
                        return@withContext Result.failure(error)
                    }
                    
                    lastException = error
                }
                
            } catch (e: Exception) {
                lastException = e
                logger.logError(TAG, "Request exception: ${e.message}", e)
            } finally {
                connection?.disconnect()
            }
            
            // Exponential backoff
            if (attempt < MAX_RETRIES - 1) {
                val calculatedDelay = BASE_RETRY_DELAY_MS * (RETRY_MULTIPLIER.pow(attempt))
                val delayMs = if (calculatedDelay < MAX_RETRY_DELAY_MS) calculatedDelay else MAX_RETRY_DELAY_MS
                kotlinx.coroutines.delay(delayMs.toLong())
            }
        }
        
        updateMetrics(false, 0)
        Result.failure(lastException ?: Exception("Request failed after $MAX_RETRIES attempts"))
    }
    
    /**
     * Update API metrics and circuit breaker state
     */
    private fun updateMetrics(success: Boolean, responseTime: Long) {
        val currentMetrics = _apiMetrics.value
        val newMetrics = currentMetrics.copy(
            totalRequests = currentMetrics.totalRequests + 1,
            successfulRequests = if (success) currentMetrics.successfulRequests + 1 else currentMetrics.successfulRequests,
            averageResponseTime = if (currentMetrics.totalRequests > 0) {
                (currentMetrics.averageResponseTime * currentMetrics.totalRequests + responseTime) / (currentMetrics.totalRequests + 1)
            } else responseTime,
            consecutiveFailures = if (success) 0 else currentMetrics.consecutiveFailures + 1,
            circuitBreakerOpen = if (!success && currentMetrics.consecutiveFailures >= CIRCUIT_BREAKER_THRESHOLD) {
                lastFailureTime.set(System.currentTimeMillis())
                true
            } else if (success && currentMetrics.circuitBreakerOpen) {
                // Reset circuit breaker after 30 seconds
                if (System.currentTimeMillis() - lastFailureTime.get() > 30000) {
                    false
                } else currentMetrics.circuitBreakerOpen
            } else currentMetrics.circuitBreakerOpen
        )
        _apiMetrics.value = newMetrics
    }
    
    /**
     * Check system health
     */
    suspend fun checkHealth(): Result<HealthStatus> = withContext(Dispatchers.IO) {
        try {
            val result = executeRequest("$BASE_URL/health")
            if (result.isSuccess) {
                val healthData = json.decodeFromString<HealthStatus>(result.getOrThrow())
                _healthStatus.value = mapOf("business-engine" to healthData)
                Result.success(healthData)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Health check failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get user credit balance with proper user identification
     */
    suspend fun getCreditBalance(): Result<CreditBalanceResponse> = withContext(Dispatchers.IO) {
        try {
            val userId = userIdentification.userId
            logger.log(PluctCoreLoggingStructuredLogger.LogLevel.INFO, TAG, "Getting REAL credit balance for user: $userId")
            
            val jwt = PluctCoreAPIModels.generateUserJWT(userId)
            val result = executeRequest(
                url = "$BASE_URL/v1/credits/balance",
                headers = mapOf(
                    "Authorization" to "Bearer $jwt",
                    "Content-Type" to "application/json",
                    "X-User-ID" to userId
                )
            )
            
            if (result.isSuccess) {
                val responseBody = result.getOrThrow()
                logger.log(PluctCoreLoggingStructuredLogger.LogLevel.INFO, TAG, "Raw API response for credit balance: $responseBody")
                
                val balanceData = json.decodeFromString<CreditBalanceResponse>(responseBody)
                logger.log(PluctCoreLoggingStructuredLogger.LogLevel.INFO, TAG, "REAL credit balance retrieved for $userId: ${balanceData.balance}")
                Result.success(balanceData)
            } else {
                val error = result.exceptionOrNull() ?: Exception("Credit balance check failed")
                logger.logError(TAG, "Failed to get credit balance for $userId: ${error.message}", error as? Exception)
                
                // If user doesn't exist (404), create them with default credits
                if (error.message?.contains("404") == true || error.message?.contains("not found") == true) {
                    logger.log(PluctCoreLoggingStructuredLogger.LogLevel.INFO, TAG, "User $userId not found, creating with default credits")
                    return@withContext createUserWithDefaultCredits()
                } else {
                    Result.failure(error)
                }
            }
        } catch (e: Exception) {
            logger.logError(TAG, "Error getting credit balance: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create user with default credits if they don't exist
     */
    private suspend fun createUserWithDefaultCredits(): Result<CreditBalanceResponse> = withContext(Dispatchers.IO) {
        try {
            val userId = userIdentification.userId
            val defaultCredits = userIdentification.getDefaultCredits()
            
            logger.log(PluctCoreLoggingStructuredLogger.LogLevel.INFO, TAG, "Creating user $userId with $defaultCredits default credits")
            
            // Try to vend a token to create the user account
            val vendResult = vendToken()
            vendResult.fold(
                onSuccess = { vendResponse ->
                    logger.log(PluctCoreLoggingStructuredLogger.LogLevel.INFO, TAG, "User $userId created successfully with ${vendResponse.balanceAfter} credits")
                    Result.success(CreditBalanceResponse(
                        userId = userId,
                        balance = vendResponse.balanceAfter,
                        updatedAt = System.currentTimeMillis().toString()
                    ))
                },
                onFailure = { error ->
                    logger.logError(TAG, "Failed to create user $userId: ${error.message}", error as? Exception)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.logError(TAG, "Error creating user: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Vend token for transcription with proper user identification
     */
    suspend fun vendToken(clientRequestId: String? = null): Result<VendTokenResponse> = withContext(Dispatchers.IO) {
        try {
            val userId = userIdentification.userId
            logger.log(PluctCoreLoggingStructuredLogger.LogLevel.INFO, TAG, "Vending token for user: $userId")
            
            val jwt = PluctCoreAPIModels.generateUserJWT(userId)
            val requestBody = """{"clientRequestId":"${clientRequestId ?: "req_${System.currentTimeMillis()}"}"}"""
            
            val result = executeRequest(
                url = "$BASE_URL/v1/vend-token",
                method = "POST",
                headers = mapOf(
                    "Authorization" to "Bearer $jwt",
                    "Content-Type" to "application/json",
                    "X-User-ID" to userId
                ),
                body = requestBody
            )
            
            if (result.isSuccess) {
                val vendData = json.decodeFromString<VendTokenResponse>(result.getOrThrow())
                logger.log(PluctCoreLoggingStructuredLogger.LogLevel.INFO, TAG, "Token vended successfully for $userId")
                Result.success(vendData)
            } else {
                val error = result.exceptionOrNull() ?: Exception("Token vending failed")
                logger.logError(TAG, "Failed to vend token for $userId: ${error.message}", error as? Exception)
                Result.failure(error)
            }
        } catch (e: Exception) {
            logger.logError(TAG, "Error vending token: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Start transcription job
     */
    suspend fun startTranscription(url: String): Result<TranscriptionResponse> = withContext(Dispatchers.IO) {
        try {
            // Validate URL
            val urlValidation = validator.validateUrl(url)
            if (!urlValidation.isValid) {
                return@withContext Result.failure(Exception("Invalid URL: ${urlValidation.errorMessage}"))
            }
            
            val userId = userIdentification.userId
            val jwt = PluctCoreAPIModels.generateUserJWT(userId)
            val requestBody = """{"url":"$url"}"""
            
            val result = executeRequest(
                url = "$BASE_URL/ttt/transcribe",
                method = "POST",
                headers = mapOf(
                    "Authorization" to "Bearer $jwt",
                    "Content-Type" to "application/json",
                    "X-User-ID" to userId
                ),
                body = requestBody
            )
            
            if (result.isSuccess) {
                val transcriptionData = json.decodeFromString<TranscriptionResponse>(result.getOrThrow())
                Result.success(transcriptionData)
            } else {
                val error = result.exceptionOrNull() ?: Exception("Transcription start failed")
                Result.failure(error)
            }
        } catch (e: Exception) {
            logger.logError(TAG, "Error starting transcription: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check transcription status
     */
    suspend fun checkTranscriptionStatus(jobId: String): Result<TranscriptionStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val userId = userIdentification.userId
            val jwt = PluctCoreAPIModels.generateUserJWT(userId)
            
            val result = executeRequest(
                url = "$BASE_URL/ttt/status/$jobId",
                headers = mapOf(
                    "Authorization" to "Bearer $jwt",
                    "X-User-ID" to userId
                )
            )
            
            if (result.isSuccess) {
                val statusData = json.decodeFromString<TranscriptionStatusResponse>(result.getOrThrow())
                Result.success(statusData)
            } else {
                val error = result.exceptionOrNull() ?: Exception("Status check failed")
                Result.failure(error)
            }
        } catch (e: Exception) {
            logger.logError(TAG, "Error checking transcription status: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Extract metadata locally (simplified)
     */
    suspend fun extractMetadataLocally(url: String): Result<MetadataResponse> = withContext(Dispatchers.IO) {
        try {
            // Validate URL
            val urlValidation = validator.validateUrl(url)
            if (!urlValidation.isValid) {
                return@withContext Result.failure(Exception("Invalid URL: ${urlValidation.errorMessage}"))
            }
            
            // Simplified metadata extraction
            val metadata = MetadataResponse(
                url = url,
                title = "Video Title",
                description = "Video Description",
                author = "Creator",
                duration = 30,
                thumbnail = "",
                cached = false,
                timestamp = System.currentTimeMillis().toString()
            )
            
            Result.success(metadata)
        } catch (e: Exception) {
            logger.logError(TAG, "Error extracting metadata: ${e.message}", e)
            Result.failure(e)
        }
    }
}

// Data classes for API responses
@Serializable
data class APIMetrics(
    val totalRequests: Long = 0,
    val successfulRequests: Long = 0,
    val averageResponseTime: Long = 0,
    val consecutiveFailures: Int = 0,
    val circuitBreakerOpen: Boolean = false,
    val lastError: String? = null
)

@Serializable
data class ApiError(
    val responseCode: Int,
    val responseBody: String,
    val url: String,
    val method: String
) : Exception("API Error: $responseCode - $responseBody")

@Serializable
data class HealthStatus(
    val status: String,
    val uptimeSeconds: Long,
    val version: String,
    val build: BuildInfo,
    val configuration: Map<String, Boolean>,
    val connectivity: Map<String, String>,
    val routes: Map<String, List<String>>
)

@Serializable
data class BuildInfo(
    val ref: String,
    val deployedAt: String
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
    val scope: String,
    val expiresAt: String,
    val balanceAfter: Int,
    val requestId: String
)

@Serializable
data class TranscriptionResponse(
    val jobId: String,
    val status: String,
    val estimatedTime: Int,
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

@Serializable
data class MetadataResponse(
    val url: String,
    val title: String,
    val description: String,
    val author: String,
    val duration: Int,
    val thumbnail: String,
    val cached: Boolean,
    val timestamp: String
)