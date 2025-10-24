package app.pluct.services

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Pluct-Monitoring-01Logger - Comprehensive logging and monitoring system
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */

class PluctLogger private constructor() {
    
    companion object {
        private const val TAG = "PluctApp"
        private const val MAX_LOG_ENTRIES = 1000
        
        @Volatile
        private var INSTANCE: PluctLogger? = null
        
        fun getInstance(): PluctLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PluctLogger().also { INSTANCE = it }
            }
        }
    }
    
    private val _logEntries = MutableSharedFlow<LogEntry>()
    val logEntries: Flow<LogEntry> = _logEntries.asSharedFlow()
    
    private val logBuffer = mutableListOf<LogEntry>()
    
    /**
     * Logs an info message
     */
    fun info(message: String, tag: String = TAG, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.INFO, message, tag, metadata)
    }
    
    /**
     * Logs a debug message
     */
    fun debug(message: String, tag: String = TAG, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.DEBUG, message, tag, metadata)
    }
    
    /**
     * Logs a warning message
     */
    fun warning(message: String, tag: String = TAG, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.WARNING, message, tag, metadata)
    }
    
    /**
     * Logs an error message
     */
    fun error(message: String, tag: String = TAG, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.ERROR, message, tag, metadata, throwable)
    }
    
    /**
     * Logs a critical error message
     */
    fun critical(message: String, tag: String = TAG, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.CRITICAL, message, tag, metadata, throwable)
    }
    
    /**
     * Logs API request/response
     */
    fun apiRequest(
        method: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val apiMetadata = metadata + mapOf(
            "method" to method,
            "url" to url,
            "headers" to headers,
            "body" to (body ?: "")
        )
        log(LogLevel.INFO, "API Request: $method $url", "API", apiMetadata)
    }
    
    /**
     * Logs API response
     */
    fun apiResponse(
        statusCode: Int,
        responseTime: Long,
        responseBody: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val apiMetadata = metadata + mapOf(
            "statusCode" to statusCode,
            "responseTime" to responseTime,
            "responseBody" to (responseBody ?: "")
        )
        log(LogLevel.INFO, "API Response: $statusCode (${responseTime}ms)", "API", apiMetadata)
    }
    
    /**
     * Logs user interaction
     */
    fun userInteraction(
        action: String,
        screen: String,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val interactionMetadata = metadata + mapOf(
            "action" to action,
            "screen" to screen
        )
        log(LogLevel.INFO, "User Interaction: $action on $screen", "USER", interactionMetadata)
    }
    
    /**
     * Logs performance metrics
     */
    fun performance(
        operation: String,
        duration: Long,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val performanceMetadata = metadata + mapOf(
            "operation" to operation,
            "duration" to duration
        )
        log(LogLevel.INFO, "Performance: $operation took ${duration}ms", "PERFORMANCE", performanceMetadata)
    }
    
    /**
     * Logs transcription processing
     */
    fun transcription(
        videoId: String,
        status: String,
        progress: Int? = null,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val transcriptionMetadata = metadata + mapOf(
            "videoId" to videoId,
            "status" to status,
            "progress" to (progress ?: 0)
        )
        log(LogLevel.INFO, "Transcription: $status for $videoId", "TRANSCRIPTION", transcriptionMetadata)
    }
    
    /**
     * Core logging method
     */
    private fun log(
        level: LogLevel,
        message: String,
        tag: String,
        metadata: Map<String, Any>,
        throwable: Throwable? = null
    ) {
        val logEntry = LogEntry(
            timestamp = LocalDateTime.now(),
            level = level,
            message = message,
            tag = tag,
            metadata = metadata,
            throwable = throwable
        )
        
        // Add to buffer
        synchronized(logBuffer) {
            logBuffer.add(logEntry)
            if (logBuffer.size > MAX_LOG_ENTRIES) {
                logBuffer.removeAt(0)
            }
        }
        
        // Emit to flow
        _logEntries.tryEmit(logEntry)
        
        // Log to Android system
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARNING -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
            LogLevel.CRITICAL -> Log.e(tag, "CRITICAL: $message", throwable)
        }
    }
    
    /**
     * Gets recent log entries
     */
    fun getRecentLogs(count: Int = 100): List<LogEntry> {
        return synchronized(logBuffer) {
            logBuffer.takeLast(count)
        }
    }
    
    /**
     * Gets logs by level
     */
    fun getLogsByLevel(level: LogLevel): List<LogEntry> {
        return synchronized(logBuffer) {
            logBuffer.filter { it.level == level }
        }
    }
    
    /**
     * Gets logs by tag
     */
    fun getLogsByTag(tag: String): List<LogEntry> {
        return synchronized(logBuffer) {
            logBuffer.filter { it.tag == tag }
        }
    }
    
    /**
     * Clears all logs
     */
    fun clearLogs() {
        synchronized(logBuffer) {
            logBuffer.clear()
        }
    }
    
    /**
     * Exports logs for debugging
     */
    fun exportLogs(): String {
        return synchronized(logBuffer) {
            logBuffer.joinToString("\n") { entry ->
                "${entry.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)} [${entry.level}] [${entry.tag}] ${entry.message}"
            }
        }
    }
}

/**
 * Log entry data class
 */
data class LogEntry(
    val timestamp: LocalDateTime,
    val level: LogLevel,
    val message: String,
    val tag: String,
    val metadata: Map<String, Any>,
    val throwable: Throwable? = null
)

enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

/**
 * Performance monitoring utilities
 */
class PluctPerformanceMonitor {
    
    private val startTimes = mutableMapOf<String, Long>()
    private val logger = PluctLogger.getInstance()
    
    /**
     * Starts timing an operation
     */
    fun startTiming(operation: String) {
        startTimes[operation] = System.currentTimeMillis()
    }
    
    /**
     * Ends timing an operation and logs the duration
     */
    fun endTiming(operation: String, metadata: Map<String, Any> = emptyMap()) {
        val startTime = startTimes.remove(operation)
        if (startTime != null) {
            val duration = System.currentTimeMillis() - startTime
            logger.performance(operation, duration, metadata)
        }
    }
    
    /**
     * Measures the execution time of a block
     */
    suspend fun <T> measure(operation: String, block: suspend () -> T): T {
        startTiming(operation)
        return try {
            block()
        } finally {
            endTiming(operation)
        }
    }
}

/**
 * Error tracking and reporting
 */
class PluctErrorTracker {
    
    private val logger = PluctLogger.getInstance()
    private val errorCounts = mutableMapOf<String, Int>()
    
    /**
     * Tracks an error occurrence
     */
    fun trackError(error: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        errorCounts[error] = (errorCounts[error] ?: 0) + 1
        
        val errorMetadata = metadata + mapOf(
            "errorCount" to errorCounts[error]!!,
            "errorType" to error
        )
        
        logger.error("Error tracked: $error", "ERROR_TRACKER", throwable, errorMetadata)
    }
    
    /**
     * Gets error statistics
     */
    fun getErrorStats(): Map<String, Int> {
        return errorCounts.toMap()
    }
    
    /**
     * Clears error statistics
     */
    fun clearErrorStats() {
        errorCounts.clear()
    }
}
