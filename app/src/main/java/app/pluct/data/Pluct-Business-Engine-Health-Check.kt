package app.pluct.data

import android.util.Log
import app.pluct.net.PluctNetworkHttp01Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Pluct-Business-Engine-Health-Check - System health verification
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluctBusinessEngineHealthCheck @Inject constructor() {
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(PluctNetworkHttp01Logger())
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val baseUrl = "https://pluct-business-engine.romeo-lya2.workers.dev"
    
    data class HealthStatus(
        val isHealthy: Boolean,
        val status: String,
        val uptimeSeconds: Long?,
        val version: String?,
        val connectivity: Map<String, String>?,
        val error: String? = null
    )
    
    suspend fun checkHealth(): HealthStatus = withContext(Dispatchers.IO) {
        try {
            Log.i("PluctBusinessEngineHealthCheck", "🎯 Checking Business Engine health...")
            
            val request = Request.Builder()
                .url("$baseUrl/health")
                .get()
                .addHeader("Accept", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            Log.i("PluctBusinessEngineHealthCheck", "🎯 Health API Response: $responseBody")
            
            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val status = json.optString("status", "unknown")
                val uptimeSeconds = json.optLong("uptimeSeconds", 0)
                val version = json.optString("version", "unknown")
                
                val connectivity = json.optJSONObject("connectivity")?.let { conn ->
                    mapOf(
                        "d1" to conn.optString("d1", "unknown"),
                        "kv" to conn.optString("kv", "unknown"),
                        "ttt" to conn.optString("ttt", "unknown"),
                        "circuitBreaker" to conn.optString("circuitBreaker", "unknown")
                    )
                }
                
                val isHealthy = status == "ok" && 
                    connectivity?.get("ttt") == "healthy" &&
                    connectivity?.get("circuitBreaker") == "closed"
                
                Log.i("PluctBusinessEngineHealthCheck", "✅ Health check result: $status")
                Log.i("PluctBusinessEngineHealthCheck", "✅ TTT Status: ${connectivity?.get("ttt")}")
                Log.i("PluctBusinessEngineHealthCheck", "✅ Circuit Breaker: ${connectivity?.get("circuitBreaker")}")
                
                HealthStatus(
                    isHealthy = isHealthy,
                    status = status,
                    uptimeSeconds = uptimeSeconds,
                    version = version,
                    connectivity = connectivity
                )
            } else {
                Log.w("PluctBusinessEngineHealthCheck", "⚠️ Health check failed: ${response.code}")
                HealthStatus(
                    isHealthy = false,
                    status = "error",
                    uptimeSeconds = null,
                    version = null,
                    connectivity = null,
                    error = "HTTP ${response.code}: $responseBody"
                )
            }
        } catch (e: Exception) {
            Log.e("PluctBusinessEngineHealthCheck", "❌ Health check failed: ${e.message}", e)
            HealthStatus(
                isHealthy = false,
                status = "error",
                uptimeSeconds = null,
                version = null,
                connectivity = null,
                error = e.message ?: "Unknown error"
            )
        }
    }
}
