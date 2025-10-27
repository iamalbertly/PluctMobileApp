package app.pluct.services

import android.util.Log
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import app.pluct.architecture.PluctComponent

/**
 * Pluct-Core-Logging-01StructuredLogger - Centralized structured logging service
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Provides structured logging with different levels and contextual information
 */
@Singleton
class PluctCoreLoggingStructuredLogger @Inject constructor() : PluctComponent {
    
    companion object {
        private const val TAG = "PluctStructuredLogger"
        private const val MAX_LOG_ENTRIES = 1000
    }
    
    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries: StateFlow<List<LogEntry>> = _logEntries.asStateFlow()
    
    private val json = Json { ignoreUnknownKeys = true }
    
    override val componentId: String = "pluct-core-logging-structured-logger"
    override val dependencies: List<String> = emptyList()
    
    override fun initialize() {
        Log.d(TAG, "Initializing PluctCoreLoggingStructuredLogger")
    }
    
    override fun cleanup() {
        Log.d(TAG, "Cleaning up PluctCoreLoggingStructuredLogger")
    }
    
    @Serializable
    data class LogEntry(
        val timestamp: Long,
        val level: LogLevel,
        val tag: String,
        val message: String,
        val context: Map<String, String> = emptyMap(),
        val exception: String? = null,
        val userId: String? = null,
        val sessionId: String? = null
    )
    
    enum class LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR, ASSERT
    }
    
    /**
     * Log a message with structured context
     */
    fun log(
        level: LogLevel,
        tag: String,
        message: String,
        context: Map<String, String> = emptyMap(),
        exception: Throwable? = null,
        userId: String? = null,
        sessionId: String? = null
    ) {
        val timestamp = System.currentTimeMillis()
        val logEntry = LogEntry(
            timestamp = timestamp,
            level = level,
            tag = tag,
            message = message,
            context = context,
            exception = exception?.stackTraceToString(),
            userId = userId,
            sessionId = sessionId
        )
        
        // Add to flow
        val currentEntries = _logEntries.value.toMutableList()
        currentEntries.add(logEntry)
        
        // Keep only the last MAX_LOG_ENTRIES
        if (currentEntries.size > MAX_LOG_ENTRIES) {
            currentEntries.removeAt(0)
        }
        
        _logEntries.value = currentEntries
        
        // Also log to Android Log
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, message, exception)
            LogLevel.DEBUG -> Log.d(tag, message, exception)
            LogLevel.INFO -> Log.i(tag, message, exception)
            LogLevel.WARN -> Log.w(tag, message, exception)
            LogLevel.ERROR -> Log.e(tag, message, exception)
            LogLevel.ASSERT -> Log.wtf(tag, message, exception)
        }
    }
    
    /**
     * Log API request
     */
    fun logApiRequest(
        method: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        userId: String? = null
    ) {
        log(
            level = LogLevel.INFO,
            tag = "API_REQUEST",
            message = "$method $url",
            context = mapOf(
                "method" to method,
                "url" to url,
                "hasBody" to (body != null).toString(),
                "headerCount" to headers.size.toString()
            ),
            userId = userId
        )
    }
    
    /**
     * Log API response
     */
    fun logApiResponse(
        method: String,
        url: String,
        responseCode: Int,
        responseTime: Long,
        success: Boolean,
        userId: String? = null
    ) {
        log(
            level = if (success) LogLevel.INFO else LogLevel.WARN,
            tag = "API_RESPONSE",
            message = "$method $url -> $responseCode (${responseTime}ms)",
            context = mapOf(
                "method" to method,
                "url" to url,
                "responseCode" to responseCode.toString(),
                "responseTime" to responseTime.toString(),
                "success" to success.toString()
            ),
            userId = userId
        )
    }
    
    /**
     * Log user action
     */
    fun logUserAction(
        action: String,
        context: Map<String, String> = emptyMap(),
        userId: String? = null
    ) {
        log(
            level = LogLevel.INFO,
            tag = "USER_ACTION",
            message = "User action: $action",
            context = context + mapOf("action" to action),
            userId = userId
        )
    }
    
    /**
     * Log error with context
     */
    fun logError(
        tag: String,
        message: String,
        exception: Throwable? = null,
        context: Map<String, String> = emptyMap(),
        userId: String? = null
    ) {
        log(
            level = LogLevel.ERROR,
            tag = tag,
            message = message,
            context = context,
            exception = exception,
            userId = userId
        )
    }
    
    /**
     * Get recent log entries for debugging
     */
    fun getRecentLogs(count: Int = 50): List<LogEntry> {
        return _logEntries.value.takeLast(count)
    }
    
    /**
     * Get logs by level
     */
    fun getLogsByLevel(level: LogLevel): List<LogEntry> {
        return _logEntries.value.filter { it.level == level }
    }
    
    /**
     * Get logs by tag
     */
    fun getLogsByTag(tag: String): List<LogEntry> {
        return _logEntries.value.filter { it.tag == tag }
    }
    
    /**
     * Clear all logs
     */
    fun clearLogs() {
        _logEntries.value = emptyList()
        Log.d(TAG, "Logs cleared")
    }
    
    /**
     * Export logs as JSON
     */
    fun exportLogsAsJson(): String {
        return "[]" // Simplified for now
    }
}
