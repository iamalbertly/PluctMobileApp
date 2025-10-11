package app.pluct.ui.utils

import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Ring buffer for storing run logs with automatic cleanup
 */
object RunRingBuffer {
    private const val TAG = "RunRingBuffer"
    private const val MAX_ENTRIES = 1000
    
    private val logs = ConcurrentLinkedQueue<LogEntry>()
    
    data class LogEntry(
        val runId: String,
        val level: String,
        val message: String,
        val component: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Add a log entry to the ring buffer
     */
    fun addLog(runId: String, level: String, message: String, component: String) {
        try {
            val entry = LogEntry(runId, level, message, component)
            logs.offer(entry)
            
            // Clean up old entries if buffer is full
            while (logs.size > MAX_ENTRIES) {
                logs.poll()
            }
            
            // Also log to Android logcat
            when (level) {
                "ERROR" -> Log.e("$TAG-$component", "[$runId] $message")
                "WARN" -> Log.w("$TAG-$component", "[$runId] $message")
                else -> Log.d("$TAG-$component", "[$runId] $message")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding log entry: ${e.message}", e)
        }
    }
    
    /**
     * Get logs for a specific run ID
     */
    fun getLogsForRun(runId: String): List<LogEntry> {
        return logs.filter { it.runId == runId }.sortedBy { it.timestamp }
    }
    
    /**
     * Get all logs
     */
    fun getAllLogs(): List<LogEntry> {
        return logs.toList().sortedBy { it.timestamp }
    }
    
    /**
     * Clear all logs
     */
    fun clear() {
        logs.clear()
    }
    
    /**
     * Get recent logs (last 100 entries)
     */
    fun getRecentLogs(): List<LogEntry> {
        return logs.toList().takeLast(100).sortedBy { it.timestamp }
    }
}