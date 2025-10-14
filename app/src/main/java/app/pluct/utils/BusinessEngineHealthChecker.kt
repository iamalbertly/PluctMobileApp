package app.pluct.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

/**
 * Business Engine Health Checker - Verifies connectivity and service health
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
object BusinessEngineHealthChecker {
    private const val TAG = "BusinessEngineHealthChecker"
    private const val BUSINESS_ENGINE_BASE_URL = "https://pluct-business-engine.romeo-lya2.workers.dev"
    
    // Configure HTTP client with proper timeouts
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Check if Business Engine is healthy and accessible
     */
    suspend fun checkBusinessEngineHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking Business Engine health...")
            
            val request = Request.Builder()
                .url("$BUSINESS_ENGINE_BASE_URL/health")
                .get()
                .build()
            
            val response = httpClient.newCall(request).execute()
            val isHealthy = response.isSuccessful
            
            if (isHealthy) {
                Log.i(TAG, "Business Engine is healthy")
            } else {
                Log.e(TAG, "Business Engine health check failed: ${response.code}")
            }
            
            response.close()
            isHealthy
        } catch (e: Exception) {
            Log.e(TAG, "Business Engine health check error: ${e.message}")
            false
        }
    }

    /**
     * Test token vending endpoint
     */
    suspend fun testTokenVending(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Testing token vending endpoint...")
            
            val requestBody = org.json.JSONObject().apply {
                put("userId", "mobile")
            }
            
            val request = Request.Builder()
                .url("$BUSINESS_ENGINE_BASE_URL/vend-token")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = httpClient.newCall(request).execute()
            val isSuccessful = response.isSuccessful
            
            if (isSuccessful) {
                Log.i(TAG, "Token vending endpoint is working")
            } else {
                Log.e(TAG, "Token vending test failed: ${response.code}")
            }
            
            response.close()
            isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Token vending test error: ${e.message}")
            false
        }
    }

    /**
     * Test TTTranscribe proxy endpoint
     */
    suspend fun testTTTranscribeProxy(token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Testing TTTranscribe proxy endpoint...")
            
            val requestBody = org.json.JSONObject().apply {
                put("url", "https://vm.tiktok.com/ZMAPTWV7o/") // Test URL
            }
            
            val request = Request.Builder()
                .url("$BUSINESS_ENGINE_BASE_URL/ttt/transcribe")
                .addHeader("Authorization", "Bearer $token")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = httpClient.newCall(request).execute()
            val isSuccessful = response.isSuccessful
            
            if (isSuccessful) {
                Log.i(TAG, "TTTranscribe proxy endpoint is working")
            } else {
                Log.e(TAG, "TTTranscribe proxy test failed: ${response.code}")
            }
            
            response.close()
            isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "TTTranscribe proxy test error: ${e.message}")
            false
        }
    }

    /**
     * Comprehensive health check for all Business Engine endpoints
     */
    suspend fun performFullHealthCheck(): HealthCheckResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Performing full Business Engine health check...")
        
        val healthCheck = checkBusinessEngineHealth()
        val tokenVending = if (healthCheck) testTokenVending() else false
        val tttProxy = if (tokenVending) {
            // Get a token first, then test the proxy
            try {
                val requestBody = org.json.JSONObject().apply {
                    put("userId", "mobile")
                }
                
                val request = Request.Builder()
                    .url("$BUSINESS_ENGINE_BASE_URL/vend-token")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = org.json.JSONObject(responseBody ?: "")
                    val token = jsonResponse.getString("token")
                    testTTTranscribeProxy(token)
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get token for proxy test: ${e.message}")
                false
            }
        } else false
        
        val result = HealthCheckResult(
            isHealthy = healthCheck,
            tokenVendingWorking = tokenVending,
            tttProxyWorking = tttProxy,
            overallStatus = if (healthCheck && tokenVending && tttProxy) "HEALTHY" else "UNHEALTHY"
        )
        
        Log.i(TAG, "Health check completed: $result")
        result
    }

    /**
     * Handle TTT errors with proper logging and categorization
     */
    fun handleTTTError(stage: String, error: String, videoUrl: String) {
        Log.e(TAG, "TTT Error - Stage: $stage, URL: $videoUrl, Error: $error")
        
        // Categorize errors for better debugging
        when {
            error.contains("403") -> Log.e(TAG, "Credit/Authorization error in $stage")
            error.contains("timeout") -> Log.e(TAG, "Network timeout in $stage")
            error.contains("connection") -> Log.e(TAG, "Connection error in $stage")
            error.contains("500") -> Log.e(TAG, "Server error in $stage")
            else -> Log.e(TAG, "Unknown error in $stage")
        }
    }
}

/**
 * Health check result data class
 */
data class HealthCheckResult(
    val isHealthy: Boolean,
    val tokenVendingWorking: Boolean,
    val tttProxyWorking: Boolean,
    val overallStatus: String
)
