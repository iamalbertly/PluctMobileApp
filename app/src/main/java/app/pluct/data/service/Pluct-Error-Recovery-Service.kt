package app.pluct.data.service

import android.util.Log
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced error recovery and retry mechanisms
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Singleton
class PluctErrorRecoveryService @Inject constructor() {
    
    data class RetryConfig(
        val maxRetries: Int = 3,
        val baseDelayMs: Long = 1000,
        val maxDelayMs: Long = 30000,
        val backoffMultiplier: Double = 2.0
    )
    
    data class RetryResult(
        val success: Boolean,
        val attempts: Int,
        val totalTimeMs: Long,
        val error: String? = null
    )
    
    /**
     * Execute operation with exponential backoff retry
     */
    suspend fun executeWithRetry(
        operation: suspend () -> Boolean,
        config: RetryConfig = RetryConfig(),
        operationName: String = "Operation"
    ): RetryResult {
        val startTime = System.currentTimeMillis()
        var lastError: String? = null
        
        Log.i("ErrorRecovery", "🔄 Starting retry operation: $operationName")
        
        for (attempt in 1..config.maxRetries) {
            try {
                Log.i("ErrorRecovery", "🎯 Attempt $attempt/$config.maxRetries for $operationName")
                
                val result = operation()
                if (result) {
                    val totalTime = System.currentTimeMillis() - startTime
                    Log.i("ErrorRecovery", "✅ Operation $operationName succeeded on attempt $attempt (${totalTime}ms)")
                    
                    return RetryResult(
                        success = true,
                        attempts = attempt,
                        totalTimeMs = totalTime
                    )
                } else {
                    lastError = "Operation returned false"
                    Log.w("ErrorRecovery", "⚠️ Operation $operationName failed on attempt $attempt: $lastError")
                }
            } catch (e: Exception) {
                lastError = e.message ?: "Unknown error"
                Log.w("ErrorRecovery", "⚠️ Operation $operationName failed on attempt $attempt: $lastError")
            }
            
            // Don't delay after the last attempt
            if (attempt < config.maxRetries) {
                val delayMs = calculateDelay(attempt, config)
                Log.i("ErrorRecovery", "⏳ Waiting ${delayMs}ms before retry $attempt")
                delay(delayMs)
            }
        }
        
        val totalTime = System.currentTimeMillis() - startTime
        Log.e("ErrorRecovery", "❌ Operation $operationName failed after $config.maxRetries attempts (${totalTime}ms)")
        
        return RetryResult(
            success = false,
            attempts = config.maxRetries,
            totalTimeMs = totalTime,
            error = lastError
        )
    }
    
    /**
     * Calculate delay with exponential backoff
     */
    private fun calculateDelay(attempt: Int, config: RetryConfig): Long {
        val delayMs = (config.baseDelayMs * Math.pow(config.backoffMultiplier, (attempt - 1).toDouble())).toLong()
        return delayMs.coerceAtMost(config.maxDelayMs)
    }
    
    /**
     * Recover from network errors
     */
    suspend fun recoverFromNetworkError(
        operation: suspend () -> Boolean,
        operationName: String = "Network Operation"
    ): RetryResult {
        Log.i("ErrorRecovery", "🌐 Recovering from network error: $operationName")
        
        return executeWithRetry(
            operation = operation,
            config = RetryConfig(
                maxRetries = 5,
                baseDelayMs = 2000,
                maxDelayMs = 60000,
                backoffMultiplier = 1.5
            ),
            operationName = operationName
        )
    }
    
    /**
     * Recover from API errors
     */
    suspend fun recoverFromApiError(
        operation: suspend () -> Boolean,
        operationName: String = "API Operation"
    ): RetryResult {
        Log.i("ErrorRecovery", "🔌 Recovering from API error: $operationName")
        
        return executeWithRetry(
            operation = operation,
            config = RetryConfig(
                maxRetries = 3,
                baseDelayMs = 1000,
                maxDelayMs = 10000,
                backoffMultiplier = 2.0
            ),
            operationName = operationName
        )
    }
    
    /**
     * Recover from transcription errors
     */
    suspend fun recoverFromTranscriptionError(
        operation: suspend () -> Boolean,
        operationName: String = "Transcription Operation"
    ): RetryResult {
        Log.i("ErrorRecovery", "🎵 Recovering from transcription error: $operationName")
        
        return executeWithRetry(
            operation = operation,
            config = RetryConfig(
                maxRetries = 2,
                baseDelayMs = 5000,
                maxDelayMs = 30000,
                backoffMultiplier = 3.0
            ),
            operationName = operationName
        )
    }
    
    /**
     * Classify error type for appropriate recovery strategy
     */
    fun classifyError(error: Throwable): String {
        return when {
            error.message?.contains("network", ignoreCase = true) == true -> "NETWORK"
            error.message?.contains("timeout", ignoreCase = true) == true -> "TIMEOUT"
            error.message?.contains("api", ignoreCase = true) == true -> "API"
            error.message?.contains("transcription", ignoreCase = true) == true -> "TRANSCRIPTION"
            error.message?.contains("auth", ignoreCase = true) == true -> "AUTH"
            else -> "UNKNOWN"
        }
    }
    
    /**
     * Get appropriate retry config based on error type
     */
    fun getRetryConfigForError(errorType: String): RetryConfig {
        return when (errorType) {
            "NETWORK" -> RetryConfig(
                maxRetries = 5,
                baseDelayMs = 2000,
                maxDelayMs = 60000,
                backoffMultiplier = 1.5
            )
            "TIMEOUT" -> RetryConfig(
                maxRetries = 3,
                baseDelayMs = 3000,
                maxDelayMs = 30000,
                backoffMultiplier = 2.0
            )
            "API" -> RetryConfig(
                maxRetries = 3,
                baseDelayMs = 1000,
                maxDelayMs = 10000,
                backoffMultiplier = 2.0
            )
            "TRANSCRIPTION" -> RetryConfig(
                maxRetries = 2,
                baseDelayMs = 5000,
                maxDelayMs = 30000,
                backoffMultiplier = 3.0
            )
            "AUTH" -> RetryConfig(
                maxRetries = 1,
                baseDelayMs = 1000,
                maxDelayMs = 5000,
                backoffMultiplier = 1.0
            )
            else -> RetryConfig()
        }
    }
}
