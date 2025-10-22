package app.pluct.core.log

import android.util.Log
import org.json.JSONObject
import app.pluct.core.error.ErrorEnvelope
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pluct-Core-Log-01StructuredLogger - Deterministic structured logging system
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Provides consistent, parseable logging for automated testing and debugging
 */
object PluctLogger {
    private const val TAG = "PLUCT_ERR"
    private const val INFO_TAG = "PLUCT_INFO"
    private const val API_TAG = "PLUCT_API"
    private const val PERFORMANCE_TAG = "PLUCT_PERF"
    
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    fun logError(e: ErrorEnvelope) {
        val json = """{"type":"ui_error","code":"${e.code}","message":${escape(e.message)},"source":"${e.source}","timestamp":"${getCurrentTimestamp()}","severity":"${e.severity}","context":"${e.context ?: "unknown"}"}"""
        Log.e(TAG, json)
    }
    
    fun logInfo(message: String, context: String = "general") {
        val json = """{"type":"info","message":${escape(message)},"context":"$context","timestamp":"${getCurrentTimestamp()}"}"""
        Log.i(INFO_TAG, json)
    }
    
    fun logWarning(message: String, context: String = "general") {
        val json = """{"type":"warning","message":${escape(message)},"context":"$context","timestamp":"${getCurrentTimestamp()}"}"""
        Log.w(INFO_TAG, json)
    }
    
    fun logApiCall(endpoint: String, method: String, statusCode: Int, durationMs: Long) {
        val json = """{"type":"api_call","endpoint":"$endpoint","method":"$method","statusCode":$statusCode,"durationMs":$durationMs,"timestamp":"${getCurrentTimestamp()}"}"""
        Log.i(API_TAG, json)
    }
    
    fun logPerformance(operation: String, durationMs: Long, details: Map<String, Any> = emptyMap()) {
        val detailsJson = if (details.isNotEmpty()) {
            details.entries.joinToString(",") { """"${it.key}":"${it.value}"""" }
        } else ""
        
        val json = """{"type":"performance","operation":"$operation","durationMs":$durationMs,"timestamp":"${getCurrentTimestamp()}${if (detailsJson.isNotEmpty()) ",$detailsJson" else ""}}"""
        Log.i(PERFORMANCE_TAG, json)
    }
    
    fun logUserAction(action: String, details: Map<String, Any> = emptyMap()) {
        val detailsJson = if (details.isNotEmpty()) {
            details.entries.joinToString(",") { """"${it.key}":"${it.value}"""" }
        } else ""
        
        val json = """{"type":"user_action","action":"$action","timestamp":"${getCurrentTimestamp()}${if (detailsJson.isNotEmpty()) ",$detailsJson" else ""}}"""
        Log.i(INFO_TAG, json)
    }
    
    fun logBusinessEngineCall(operation: String, success: Boolean, responseTime: Long, details: Map<String, Any> = emptyMap()) {
        val detailsJson = if (details.isNotEmpty()) {
            details.entries.joinToString(",") { """"${it.key}":"${it.value}"""" }
        } else ""
        
        val json = """{"type":"business_engine","operation":"$operation","success":$success,"responseTime":$responseTime,"timestamp":"${getCurrentTimestamp()}${if (detailsJson.isNotEmpty()) ",$detailsJson" else ""}}"""
        Log.i(API_TAG, json)
    }
    
    fun logTTTranscribeCall(operation: String, success: Boolean, responseTime: Long, details: Map<String, Any> = emptyMap()) {
        val detailsJson = if (details.isNotEmpty()) {
            details.entries.joinToString(",") { """"${it.key}":"${it.value}"""" }
        } else ""
        
        val json = """{"type":"tttranscribe","operation":"$operation","success":$success,"responseTime":$responseTime,"timestamp":"${getCurrentTimestamp()}${if (detailsJson.isNotEmpty()) ",$detailsJson" else ""}}"""
        Log.i(API_TAG, json)
    }
    
    private fun escape(s: String): String {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }
    
    private fun getCurrentTimestamp(): String {
        return dateFormatter.format(Date())
    }
}
