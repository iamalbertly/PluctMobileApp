package app.pluct.services

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Logging-Service - Comprehensive logging and monitoring service
 * Single source of truth for logging operations
 * Adheres to 300-line limit with smart separation of concerns
 */

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
)

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR, FATAL
}

@Singleton
class PluctLoggingService @Inject constructor() {
    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries: StateFlow<List<LogEntry>> = _logEntries.asStateFlow()
    
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    fun startMonitoring() {
        _isMonitoring.value = true
        log(LogLevel.INFO, "PluctLoggingService", "🔍 Logging monitoring started")
    }
    
    fun stopMonitoring() {
        _isMonitoring.value = false
        log(LogLevel.INFO, "PluctLoggingService", "🛑 Logging monitoring stopped")
    }
    
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        val entry = LogEntry(
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        )
        
        _logEntries.value = _logEntries.value + entry
        
        // Also log to Android system
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
            LogLevel.FATAL -> Log.wtf(tag, message, throwable)
        }
    }
    
    fun debug(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.DEBUG, tag, message, throwable)
    }
    
    fun info(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.INFO, tag, message, throwable)
    }
    
    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.WARN, tag, message, throwable)
    }
    
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, tag, message, throwable)
    }
    
    fun fatal(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.FATAL, tag, message, throwable)
    }
    
    fun getLogEntries(): List<LogEntry> = _logEntries.value
    
    fun getLogEntriesByLevel(level: LogLevel): List<LogEntry> {
        return _logEntries.value.filter { it.level == level }
    }
    
    fun getLogEntriesByTag(tag: String): List<LogEntry> {
        return _logEntries.value.filter { it.tag == tag }
    }
    
    fun clearLogs() {
        _logEntries.value = emptyList()
    }
    
    fun getLogSummary(): Map<LogLevel, Int> {
        return _logEntries.value.groupBy { it.level }.mapValues { it.value.size }
    }
}

