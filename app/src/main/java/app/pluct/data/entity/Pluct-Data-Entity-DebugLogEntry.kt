package app.pluct.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Pluct-Data-Entity-DebugLogEntry - Database entity for debug logs
 * Stores detailed error and warning information for debugging
 */
@Entity(tableName = "debug_logs")
data class DebugLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val category: String, // e.g., "TRANSCRIPTION", "CREDIT_CHECK", "API_CALL"
    val operation: String, // e.g., "processTikTokVideo", "checkUserBalance"
    val message: String,
    
    // Request details
    val requestMethod: String = "",
    val requestUrl: String = "",
    val requestPayload: String = "",
    val requestHeaders: String = "",
    
    // Response details
    val responseStatusCode: Int = 0,
    val responseBody: String = "",
    val responseHeaders: String = "",
    
    // Error details
    val errorCode: String = "",
    val errorType: String = "",
    val stackTrace: String = ""
)

enum class LogLevel {
    ERROR,
    WARNING,
    INFO
}
