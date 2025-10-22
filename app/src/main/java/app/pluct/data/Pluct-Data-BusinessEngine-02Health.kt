package app.pluct.data

import android.util.Log
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import app.pluct.core.log.PluctLogger

/**
 * Pluct-Data-BusinessEngine-02Health - Health check functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation][CoreResponsibility]
 */
class PluctBusinessEngineHealth(
    private val baseUrl: String,
    private val httpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "PluctBusinessEngineHealth"
    }

    /**
     * Check Business Engine health status
     */
    suspend fun health(): BusinessEngineHealthResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔍 Checking Business Engine health...")
                val request = Request.Builder()
                    .url("$baseUrl/health")
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val status = json.optString("status", "unknown")
                    val uptime = json.optLong("uptimeSeconds", 0)
                    val version = json.optString("version", "unknown")
                    
                    PluctLogger.logBusinessEngineCall("health", true, 0, mapOf(
                        "status" to status,
                        "uptime" to uptime,
                        "version" to version
                    ))

                    BusinessEngineHealthResult(
                        isHealthy = status == "ok",
                        status = status,
                        uptimeSeconds = uptime,
                        version = version,
                        responseTime = 0
                    )
                } else {
                    PluctLogger.logBusinessEngineCall("health", false, 0, mapOf(
                        "error" to "HTTP ${response.code}",
                        "body" to responseBody
                    ))
                    
                    BusinessEngineHealthResult(
                        isHealthy = false,
                        status = "error",
                        uptimeSeconds = 0,
                        version = "unknown",
                        responseTime = 0,
                        error = "HTTP ${response.code}: $responseBody"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Health check failed", e)
                PluctLogger.logError("Health check failed: ${e.message}")
                
                BusinessEngineHealthResult(
                    isHealthy = false,
                    status = "error",
                    uptimeSeconds = 0,
                    version = "unknown",
                    responseTime = 0,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
}

data class BusinessEngineHealthResult(
    val isHealthy: Boolean,
    val status: String,
    val uptimeSeconds: Long,
    val version: String,
    val responseTime: Long,
    val error: String? = null
)
