package app.pluct.core.debug

import android.util.Log
import app.pluct.data.dao.PluctDebugLogDAO
import app.pluct.data.entity.DebugLogEntry
import app.pluct.data.entity.LogLevel
import app.pluct.services.PluctCoreAPIDetailedError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Core-Debug-01LogManager - Centralized debug log manager
 * Captures all errors, warnings, and important events for debugging
 */
@Singleton
class PluctCoreDebug01LogManager @Inject constructor(
    private val debugLogDAO: PluctDebugLogDAO
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Log an API error with full details
     */
    fun logAPIError(error: PluctCoreAPIDetailedError, category: String = "API_CALL") {
        scope.launch {
            try {
                val logEntry = DebugLogEntry(
                    level = LogLevel.ERROR,
                    category = category,
                    operation = error.technicalDetails.operation,
                    message = error.userMessage,
                    requestMethod = error.technicalDetails.requestMethod,
                    requestUrl = error.technicalDetails.requestUrl,
                    requestPayload = error.technicalDetails.requestPayload,
                    requestHeaders = error.technicalDetails.requestHeaders,
                    responseStatusCode = error.technicalDetails.responseStatusCode,
                    responseBody = error.technicalDetails.responseBody,
                    responseHeaders = error.technicalDetails.responseHeaders,
                    errorCode = error.technicalDetails.errorCode,
                    errorType = error.technicalDetails.errorType,
                    stackTrace = error.stackTraceToString()
                )
                debugLogDAO.insertLog(logEntry)
                Log.d("DebugLogManager", "Logged API error: ${error.technicalDetails.operation}")
            } catch (e: Exception) {
                Log.e("DebugLogManager", "Failed to log API error: ${e.message}", e)
            }
        }
    }
    
    /**
     * Log a general error
     */
    fun logError(
        category: String,
        operation: String,
        message: String,
        exception: Throwable? = null,
        requestUrl: String = "",
        requestPayload: String = "",
        responseBody: String = ""
    ) {
        scope.launch {
            try {
                val logEntry = DebugLogEntry(
                    level = LogLevel.ERROR,
                    category = category,
                    operation = operation,
                    message = message,
                    requestUrl = requestUrl,
                    requestPayload = requestPayload,
                    responseBody = responseBody,
                    stackTrace = exception?.stackTraceToString() ?: ""
                )
                debugLogDAO.insertLog(logEntry)
                Log.d("DebugLogManager", "Logged error: $category - $operation")
            } catch (e: Exception) {
                Log.e("DebugLogManager", "Failed to log error: ${e.message}", e)
            }
        }
    }
    
    /**
     * Log a warning
     */
    fun logWarning(
        category: String,
        operation: String,
        message: String,
        details: String = ""
    ) {
        scope.launch {
            try {
                val logEntry = DebugLogEntry(
                    level = LogLevel.WARNING,
                    category = category,
                    operation = operation,
                    message = message,
                    responseBody = details
                )
                debugLogDAO.insertLog(logEntry)
                Log.d("DebugLogManager", "Logged warning: $category - $operation")
            } catch (e: Exception) {
                Log.e("DebugLogManager", "Failed to log warning: ${e.message}", e)
            }
        }
    }
    
    /**
     * Log an info message
     */
    fun logInfo(
        category: String,
        operation: String,
        message: String,
        details: String = "",
        requestUrl: String = "",
        requestMethod: String = "",
        requestPayload: String = ""
    ) {
        scope.launch {
            try {
                val logEntry = DebugLogEntry(
                    level = LogLevel.INFO,
                    category = category,
                    operation = operation,
                    message = message,
                    requestUrl = requestUrl,
                    requestMethod = requestMethod,
                    requestPayload = requestPayload,
                    responseBody = details
                )
                debugLogDAO.insertLog(logEntry)
            } catch (e: Exception) {
                Log.e("DebugLogManager", "Failed to log info: ${e.message}", e)
            }
        }
    }
    
    /**
     * Get recent logs as a Flow
     */
    fun getRecentLogs(limit: Int = 100): kotlinx.coroutines.flow.Flow<List<DebugLogEntry>> {
        return debugLogDAO.getRecentLogs(limit)
    }
    
    /**
     * Clear old logs (older than 7 days)
     */
    suspend fun clearOldLogs() {
        try {
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            val deleted = debugLogDAO.deleteOldLogs(sevenDaysAgo)
            Log.d("DebugLogManager", "Cleared $deleted old log entries")
        } catch (e: Exception) {
            Log.e("DebugLogManager", "Failed to clear old logs: ${e.message}", e)
        }
    }
    
    /**
     * Delete a specific log entry
     */
    suspend fun deleteLog(log: DebugLogEntry) {
        try {
            debugLogDAO.deleteLog(log)
            Log.d("DebugLogManager", "Deleted log entry: ${log.category} - ${log.operation}")
        } catch (e: Exception) {
            Log.e("DebugLogManager", "Failed to delete log: ${e.message}", e)
        }
    }
    
    /**
     * Get formatted log for copying to clipboard
     */
    fun formatLogForClipboard(log: DebugLogEntry): String {
        return buildString {
            appendLine("===== DEBUG LOG ENTRY =====")
            appendLine("Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp))}")
            appendLine("Level: ${log.level}")
            appendLine("Category: ${log.category}")
            appendLine("Operation: ${log.operation}")
            appendLine()
            appendLine("MESSAGE:")
            appendLine(log.message)
            
            if (log.requestUrl.isNotEmpty()) {
                appendLine()
                appendLine("REQUEST:")
                appendLine("  Method: ${log.requestMethod}")
                appendLine("  URL: ${log.requestUrl}")
                if (log.requestPayload.isNotEmpty()) {
                    appendLine("  Payload: ${log.requestPayload}")
                }
                if (log.requestHeaders.isNotEmpty()) {
                    appendLine("  Headers: ${log.requestHeaders}")
                }
            }
            
            if (log.responseStatusCode > 0) {
                appendLine()
                appendLine("RESPONSE:")
                appendLine("  Status Code: ${log.responseStatusCode}")
                if (log.responseBody.isNotEmpty()) {
                    appendLine("  Body: ${log.responseBody}")
                }
                if (log.responseHeaders.isNotEmpty()) {
                    appendLine("  Headers: ${log.responseHeaders}")
                }
            }
            
            if (log.errorCode.isNotEmpty()) {
                appendLine()
                appendLine("ERROR:")
                appendLine("  Code: ${log.errorCode}")
                appendLine("  Type: ${log.errorType}")
            }
            
            if (log.stackTrace.isNotEmpty()) {
                appendLine()
                appendLine("STACK TRACE:")
                appendLine(log.stackTrace)
            }
            
            appendLine("===========================")
        }
    }
}
