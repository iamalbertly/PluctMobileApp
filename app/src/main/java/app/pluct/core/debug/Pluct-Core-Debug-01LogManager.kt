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
    
    // Deduplication: prevent logging the same error within 5 minutes
    private val DEDUP_WINDOW_MS = 5 * 60 * 1000L // 5 minutes
    private val MAX_LOGS = 10000 // Maximum logs to keep
    private val AUTO_CLEANUP_AGE_DAYS = 3 // Auto-delete logs older than 3 days
    private val DUPLICATE_CLEANUP_INTERVAL_MS = 6 * 60 * 60 * 1000L // 6 hours
    @Volatile private var lastDuplicateCleanupMs = 0L

    companion object {
        private const val PAIN_TAG = "PluctUserPain"
        private val REPEAT_SUFFIX = Regex(" · repeats=(\\d+)$")

        internal fun appendRepeatMarker(previousMessage: String): String {
            val m = REPEAT_SUFFIX.find(previousMessage)
            return if (m != null) {
                val n = m.groupValues[1].toIntOrNull() ?: 1
                previousMessage.replaceRange(m.range, " · repeats=${n + 1}")
            } else {
                "$previousMessage · repeats=2"
            }
        }

        internal fun fingerprintForLog(url: String): String {
            if (url.isBlank()) return "0"
            return (url.hashCode() and 0x7fff_ffff).toString(16)
        }
    }

    private fun logPainPulse(category: String, operation: String, httpCode: Int, urlFingerprint: String) {
        Log.i(PAIN_TAG, "pulse category=$category op=$operation http=$httpCode fp=$urlFingerprint")
    }

    /**
     * Log an API error with full details (with deduplication)
     */
    fun logAPIError(error: PluctCoreAPIDetailedError, category: String = "API_CALL") {
        scope.launch {
            try {
                val operation = error.technicalDetails.operation
                val message = error.userMessage
                val sinceTimestamp = System.currentTimeMillis() - DEDUP_WINDOW_MS
                val now = System.currentTimeMillis()
                val fp = fingerprintForLog(error.technicalDetails.requestUrl)
                val http = error.technicalDetails.responseStatusCode

                // Check for similar recent logs
                val similarLogs = debugLogDAO.findSimilarLogs(category, operation, message, sinceTimestamp, 1)
                if (similarLogs.isNotEmpty()) {
                    val row = similarLogs.first()
                    val bumped = appendRepeatMarker(row.message)
                    debugLogDAO.updateLogTimestampAndMessage(row.id, now, bumped)
                    logPainPulse(category, operation, http, fp)
                    Log.d("DebugLogManager", "Bumped duplicate API error log: $category - $operation")
                    return@launch
                }
                
                val logEntry = DebugLogEntry(
                    level = LogLevel.ERROR,
                    category = category,
                    operation = operation,
                    message = message,
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
                Log.d("DebugLogManager", "Logged API error: $operation")
                
                // Auto-cleanup if needed
                performAutoCleanupIfNeeded()
            } catch (e: Exception) {
                Log.e("DebugLogManager", "Failed to log API error: ${e.message}", e)
            }
        }
    }
    
    /**
     * Log a general error (with deduplication)
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
                val sinceTimestamp = System.currentTimeMillis() - DEDUP_WINDOW_MS
                
                // Check for similar recent logs
                val similarLogs = debugLogDAO.findSimilarLogs(category, operation, message, sinceTimestamp, 1)
                if (similarLogs.isNotEmpty()) {
                    val row = similarLogs.first()
                    val bumped = appendRepeatMarker(row.message)
                    debugLogDAO.updateLogTimestampAndMessage(row.id, System.currentTimeMillis(), bumped)
                    logPainPulse(category, operation, 0, fingerprintForLog(requestUrl))
                    Log.d("DebugLogManager", "Bumped duplicate error log: $category - $operation")
                    return@launch
                }
                
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
                
                // Auto-cleanup if needed
                performAutoCleanupIfNeeded()
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
                val sinceTimestamp = System.currentTimeMillis() - DEDUP_WINDOW_MS
                val similarLogs = debugLogDAO.findSimilarLogs(category, operation, message, sinceTimestamp, 1)
                if (similarLogs.isNotEmpty()) {
                    val row = similarLogs.first()
                    val bumped = appendRepeatMarker(row.message)
                    debugLogDAO.updateLogTimestampAndMessage(row.id, System.currentTimeMillis(), bumped)
                    logPainPulse(category, operation, 0, "0")
                    Log.d("DebugLogManager", "Bumped duplicate warning log: $category - $operation")
                    return@launch
                }

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
                val sinceTimestamp = System.currentTimeMillis() - DEDUP_WINDOW_MS
                val similarLogs = debugLogDAO.findSimilarLogs(category, operation, message, sinceTimestamp, 1)
                if (similarLogs.isNotEmpty()) {
                    Log.d("DebugLogManager", "Skipping duplicate info log: $category - $operation")
                    return@launch
                }

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

    suspend fun formatErrorCategorySummary(limit: Int = 12): String {
        return try {
            val rows = debugLogDAO.countByCategoryForLevel(LogLevel.ERROR, limit)
            if (rows.isEmpty()) return ""
            buildString {
                appendLine("--- Error count by category ---")
                rows.forEach { appendLine("${it.category}: ${it.errorCount}") }
            }
        } catch (e: Exception) {
            Log.w("DebugLogManager", "formatErrorCategorySummary failed: ${e.message}")
            ""
        }
    }
    
    /**
     * Clear old logs (older than specified days, default 7)
     */
    suspend fun clearOldLogs(days: Int = 7) {
        try {
            val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
            val deleted = debugLogDAO.deleteOldLogs(cutoffTime)
            Log.d("DebugLogManager", "Cleared $deleted old log entries (older than $days days)")
        } catch (e: Exception) {
            Log.e("DebugLogManager", "Failed to clear old logs: ${e.message}", e)
        }
    }
    
    /**
     * Auto-cleanup: Remove old logs, duplicates, and limit total count
     */
    private suspend fun performAutoCleanupIfNeeded() {
        try {
            val logCount = debugLogDAO.getLogCount()
            
            // If we have too many logs, clean up old ones and duplicates
            if (logCount > MAX_LOGS) {
                val cutoffTime = System.currentTimeMillis() - (AUTO_CLEANUP_AGE_DAYS * 24 * 60 * 60 * 1000L)
                val deletedOld = debugLogDAO.deleteOldLogsBatch(cutoffTime, maxDelete = logCount - MAX_LOGS / 2)
                Log.d("DebugLogManager", "Auto-cleaned $deletedOld old log entries (total was $logCount)")
                
                // Also clean up duplicates from last 24 hours if still over limit
                val remainingCount = debugLogDAO.getLogCount()
                if (remainingCount > MAX_LOGS) {
                    // Clean duplicates for common operations
                    val duplicateWindow = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) // Last 24 hours
                    val commonOperations = listOf(
                        "CREDIT_CHECK" to "loadInitialData",
                        "CREDIT_CHECK" to "checkUserBalance",
                        "API_CALL" to "execute",
                        "TRANSCRIPTION" to "processTikTokVideo",
                        "PREWARM" to "metadata_prewarm"
                    )
                    var totalDuplicatesDeleted = 0
                    
                    for ((category, operation) in commonOperations) {
                        // Delete duplicates keeping only the most recent
                        val deleted = debugLogDAO.deleteDuplicateLogsByCategoryAndOperation(
                            category = category,
                            operation = operation,
                            beforeTimestamp = duplicateWindow
                        )
                        totalDuplicatesDeleted += deleted
                    }
                    
                    if (totalDuplicatesDeleted > 0) {
                        Log.d("DebugLogManager", "Auto-cleaned $totalDuplicatesDeleted duplicate log entries")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DebugLogManager", "Failed to perform auto-cleanup: ${e.message}", e)
        }
    }
    
    /**
     * Initialize: Clean up old logs on app startup
     */
    fun initialize() {
        scope.launch {
            try {
                clearOldLogs(AUTO_CLEANUP_AGE_DAYS)
                removeCommonDuplicatesIfNeeded()
                val logCount = debugLogDAO.getLogCount()
                Log.d("DebugLogManager", "Initialized. Current log count: $logCount")
            } catch (e: Exception) {
                Log.e("DebugLogManager", "Failed to initialize: ${e.message}", e)
            }
        }
    }

    /**
     * Periodically remove common duplicate logs (lightweight cleanup)
     */
    private suspend fun removeCommonDuplicatesIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastDuplicateCleanupMs < DUPLICATE_CLEANUP_INTERVAL_MS) return
        lastDuplicateCleanupMs = now

        val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        val commonOperations = listOf(
            "CREDIT_CHECK" to "loadInitialData",
            "CREDIT_CHECK" to "checkUserBalance",
            "API_CALL" to "execute",
            "API_REQUEST" to "/health",
            "TRANSCRIPTION" to "processTikTokVideo",
            "PREWARM" to "metadata_prewarm"
        )
        var totalDuplicatesDeleted = 0
        for ((category, operation) in commonOperations) {
            val deleted = debugLogDAO.deleteDuplicateLogsByCategoryAndOperation(
                category = category,
                operation = operation,
                beforeTimestamp = cutoffTime
            )
            totalDuplicatesDeleted += deleted
        }
        if (totalDuplicatesDeleted > 0) {
            Log.d("DebugLogManager", "Periodic duplicate cleanup removed $totalDuplicatesDeleted logs")
        }
    }
    
    /**
     * Check for duplicate logs and optionally remove them
     */
    suspend fun findAndRemoveDuplicates(category: String, operation: String, message: String, keepNewest: Boolean = true): Int {
        try {
            val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) // Last 24 hours
            val deleted = debugLogDAO.deleteDuplicateLogs(category, operation, message, cutoffTime)
            if (deleted > 0) {
                Log.d("DebugLogManager", "Removed $deleted duplicate logs: $category - $operation")
            }
            return deleted
        } catch (e: Exception) {
            Log.e("DebugLogManager", "Failed to remove duplicates: ${e.message}", e)
            return 0
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
     * Clear all debug logs
     */
    suspend fun clearAllLogs() {
        try {
            val deleted = debugLogDAO.clearAllLogs()
            Log.d("DebugLogManager", "Cleared all $deleted log entries")
        } catch (e: Exception) {
            Log.e("DebugLogManager", "Failed to clear all logs: ${e.message}", e)
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
