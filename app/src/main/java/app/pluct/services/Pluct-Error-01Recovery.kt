package app.pluct.services

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

/**
 * Pluct-Error-01Recovery - Advanced error handling and recovery mechanisms
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@Singleton
class PluctErrorRecovery @Inject constructor() {
    
    private val _errorHistory = MutableStateFlow<List<ErrorRecord>>(emptyList())
    val errorHistory: StateFlow<List<ErrorRecord>> = _errorHistory.asStateFlow()
    
    private val _recoveryActions = MutableStateFlow<Map<String, RecoveryAction>>(emptyMap())
    val recoveryActions: StateFlow<Map<String, RecoveryAction>> = _recoveryActions.asStateFlow()
    
    private val errorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    data class ErrorRecord(
        val id: String,
        val type: ErrorType,
        val message: String,
        val stackTrace: String?,
        val context: Map<String, Any>,
        val timestamp: Long = System.currentTimeMillis(),
        val recovered: Boolean = false,
        val recoveryAction: String? = null
    )
    
    data class RecoveryAction(
        val id: String,
        val name: String,
        val description: String,
        val action: suspend () -> Boolean,
        val retryCount: Int = 0,
        val maxRetries: Int = 3,
        val backoffMultiplier: Double = 2.0,
        val initialDelay: Long = 1000L
    )
    
    enum class ErrorType {
        NETWORK_ERROR,
        API_ERROR,
        AUTHENTICATION_ERROR,
        VALIDATION_ERROR,
        PROCESSING_ERROR,
        STORAGE_ERROR,
        UNKNOWN_ERROR
    }
    
    /**
     * Record an error
     */
    fun recordError(
        type: ErrorType,
        message: String,
        stackTrace: String? = null,
        context: Map<String, Any> = emptyMap()
    ): String {
        val errorId = generateErrorId()
        val errorRecord = ErrorRecord(
            id = errorId,
            type = type,
            message = message,
            stackTrace = stackTrace,
            context = context
        )
        
        val currentHistory = _errorHistory.value.toMutableList()
        currentHistory.add(errorRecord)
        _errorHistory.value = currentHistory
        
        Log.e("PluctErrorRecovery", "Error recorded: $type - $message")
        
        // Attempt automatic recovery
        errorScope.launch {
            attemptRecovery(errorRecord)
        }
        
        return errorId
    }
    
    /**
     * Register a recovery action
     */
    fun registerRecoveryAction(recoveryAction: RecoveryAction) {
        val currentActions = _recoveryActions.value.toMutableMap()
        currentActions[recoveryAction.id] = recoveryAction
        _recoveryActions.value = currentActions
        
        Log.d("PluctErrorRecovery", "Recovery action registered: ${recoveryAction.name}")
    }
    
    /**
     * Attempt recovery for an error
     */
    private suspend fun attemptRecovery(errorRecord: ErrorRecord) {
        val recoveryAction = findRecoveryAction(errorRecord.type)
        if (recoveryAction != null) {
            try {
                val success = executeRecoveryWithBackoff(recoveryAction)
                if (success) {
                    markErrorAsRecovered(errorRecord.id, recoveryAction.name)
                    Log.i("PluctErrorRecovery", "Error recovered: ${errorRecord.id}")
                } else {
                    Log.w("PluctErrorRecovery", "Recovery failed for error: ${errorRecord.id}")
                }
            } catch (e: Exception) {
                Log.e("PluctErrorRecovery", "Recovery action failed", e)
            }
        } else {
            Log.w("PluctErrorRecovery", "No recovery action found for error type: ${errorRecord.type}")
        }
    }
    
    /**
     * Execute recovery with exponential backoff
     */
    private suspend fun executeRecoveryWithBackoff(recoveryAction: RecoveryAction): Boolean {
        var currentDelay = recoveryAction.initialDelay
        var attempt = 0
        
        while (attempt < recoveryAction.maxRetries) {
            try {
                val success = recoveryAction.action()
                if (success) {
                    return true
                }
            } catch (e: Exception) {
                Log.w("PluctErrorRecovery", "Recovery attempt $attempt failed", e)
            }
            
            attempt++
            if (attempt < recoveryAction.maxRetries) {
                delay(currentDelay)
                currentDelay = (currentDelay * recoveryAction.backoffMultiplier).toLong()
            }
        }
        
        return false
    }
    
    /**
     * Find appropriate recovery action for error type
     */
    private fun findRecoveryAction(errorType: ErrorType): RecoveryAction? {
        return _recoveryActions.value.values.find { action ->
            when (errorType) {
                ErrorType.NETWORK_ERROR -> action.name.contains("network", ignoreCase = true)
                ErrorType.API_ERROR -> action.name.contains("api", ignoreCase = true)
                ErrorType.AUTHENTICATION_ERROR -> action.name.contains("auth", ignoreCase = true)
                ErrorType.VALIDATION_ERROR -> action.name.contains("validation", ignoreCase = true)
                ErrorType.PROCESSING_ERROR -> action.name.contains("processing", ignoreCase = true)
                ErrorType.STORAGE_ERROR -> action.name.contains("storage", ignoreCase = true)
                else -> false
            }
        }
    }
    
    /**
     * Mark error as recovered
     */
    private fun markErrorAsRecovered(errorId: String, recoveryActionName: String) {
        val currentHistory = _errorHistory.value.toMutableList()
        val errorIndex = currentHistory.indexOfFirst { it.id == errorId }
        if (errorIndex != -1) {
            val updatedError = currentHistory[errorIndex].copy(
                recovered = true,
                recoveryAction = recoveryActionName
            )
            currentHistory[errorIndex] = updatedError
            _errorHistory.value = currentHistory
        }
    }
    
    /**
     * Get error statistics
     */
    fun getErrorStatistics(): ErrorStatistics {
        val errors = _errorHistory.value
        val totalErrors = errors.size
        val recoveredErrors = errors.count { it.recovered }
        val errorTypes = errors.groupBy { it.type }.mapValues { it.value.size }
        
        return ErrorStatistics(
            totalErrors = totalErrors,
            recoveredErrors = recoveredErrors,
            recoveryRate = if (totalErrors > 0) recoveredErrors.toDouble() / totalErrors else 0.0,
            errorTypes = errorTypes
        )
    }
    
    /**
     * Clear old error records (older than 24 hours)
     */
    fun cleanupOldErrors() {
        val twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        val filteredErrors = _errorHistory.value.filter { it.timestamp > twentyFourHoursAgo }
        _errorHistory.value = filteredErrors
        
        Log.d("PluctErrorRecovery", "Cleaned up old error records")
    }
    
    private fun generateErrorId(): String {
        return "error_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
    
    data class ErrorStatistics(
        val totalErrors: Int,
        val recoveredErrors: Int,
        val recoveryRate: Double,
        val errorTypes: Map<ErrorType, Int>
    )
}
