package app.pluct.ui.utils

import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Ring buffer for storing WebView log entries with run ID correlation
 */
object RunRingBuffer {
    private const val TAG = "RunRingBuffer"
    private const val MAX_ENTRIES = 150
    
    private val logBuffer = ConcurrentLinkedQueue<LogEntry>()
    
    data class LogEntry(
        val timestamp: Long,
        val runId: String,
        val level: String,
        val message: String,
        val tag: String
    )
    
    /**
     * Add a log entry to the ring buffer
     */
    fun addLog(runId: String, level: String, message: String, tag: String = "WV") {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            runId = runId,
            level = level,
            message = message,
            tag = tag
        )
        
        logBuffer.offer(entry)
        
        // Maintain buffer size
        while (logBuffer.size > MAX_ENTRIES) {
            logBuffer.poll()
        }
    }
    
    /**
     * Get all log entries for a specific run ID
     */
    fun getLogsForRun(runId: String): List<LogEntry> {
        return logBuffer.filter { it.runId == runId }.sortedBy { it.timestamp }
    }
    
    /**
     * Get all recent log entries (last N entries)
     */
    fun getRecentLogs(count: Int = 50): List<LogEntry> {
        return logBuffer.toList().takeLast(count)
    }
    
    /**
     * Get all log entries
     */
    fun getAllLogs(): List<LogEntry> {
        return logBuffer.toList().sortedBy { it.timestamp }
    }
    
    /**
     * Clear the buffer
     */
    fun clear() {
        logBuffer.clear()
    }
    
    /**
     * Get buffer size
     */
    fun size(): Int = logBuffer.size
    
    /**
     * Check if buffer contains entries for a specific run ID
     */
    fun hasRunId(runId: String): Boolean {
        return logBuffer.any { it.runId == runId }
    }
    
    /**
     * Get the last log entry for a specific run ID
     */
    fun getLastLogForRun(runId: String): LogEntry? {
        return logBuffer.filter { it.runId == runId }.maxByOrNull { it.timestamp }
    }
    
    /**
     * Get log entries containing a specific pattern
     */
    fun getLogsContaining(pattern: String): List<LogEntry> {
        return logBuffer.filter { it.message.contains(pattern, ignoreCase = true) }
    }
    
    /**
     * Get log entries for a specific run ID containing a pattern
     */
    fun getLogsForRunContaining(runId: String, pattern: String): List<LogEntry> {
        return logBuffer.filter { 
            it.runId == runId && it.message.contains(pattern, ignoreCase = true) 
        }.sortedBy { it.timestamp }
    }
}
