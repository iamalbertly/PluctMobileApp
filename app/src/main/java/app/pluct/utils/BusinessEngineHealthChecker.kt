package app.pluct.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.TimeUnit

/**
 * Business Engine Health Checker - Verifies connectivity and service health
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
object BusinessEngineHealthChecker {
    private const val TAG = "BusinessEngineHealthChecker"
    private const val BUSINESS_ENGINE_BASE_URL = "https://pluct-business-engine.romeo-lya2.workers.dev"
    
    // Configure HTTP client with proper timeouts and debugging
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val request = chain.request()
            Log.d(TAG, "HTTP Request: ${request.method} ${request.url}")
            Log.d(TAG, "Request headers: ${request.headers}")
            
            // Log request body if present
            val requestBody = request.body
            if (requestBody != null) {
                Log.d(TAG, "Request body type: ${requestBody.contentType()}")
                Log.d(TAG, "Request body length: ${requestBody.contentLength()}")
            }
            
            val response = chain.proceed(request)
            Log.d(TAG, "HTTP Response: ${response.code} ${response.message}")
            Log.d(TAG, "Response headers: ${response.headers}")
            
            // Log response body
            val responseBody = response.body
            if (responseBody != null) {
                Log.d(TAG, "Response body type: ${responseBody.contentType()}")
                Log.d(TAG, "Response body length: ${responseBody.contentLength()}")
                
                // Read and log response body content
                val responseBodyString = responseBody.string()
                Log.d(TAG, "Response body content: $responseBodyString")
                
                // Create a new response body since we consumed the original
                val newResponseBody = responseBodyString.toResponseBody(responseBody.contentType())
                response.newBuilder().body(newResponseBody).build()
            } else {
                response
            }
        }
        .build()

    /**
     * Check if Business Engine is healthy and accessible
     */
    suspend fun checkBusinessEngineHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking Business Engine health...")
            Log.i("TTT", "stage=HEALTH_CHECK url=- reqId=- msg=checking")
            Log.d(TAG, "Request URL: $BUSINESS_ENGINE_BASE_URL/health")
            
            val request = Request.Builder()
                .url("$BUSINESS_ENGINE_BASE_URL/health")
                .addHeader("User-Agent", "Pluct-Mobile-App/1.0")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .get()
                .build()
            
            Log.d(TAG, "Making HTTP request to: ${request.url}")
            val response = httpClient.newCall(request).execute()
            val isHealthy = response.isSuccessful
            
            Log.d(TAG, "Response code: ${response.code}")
            Log.d(TAG, "Response headers: ${response.headers}")
            
            if (isHealthy) {
                Log.i(TAG, "Business Engine is healthy")
                Log.i("TTT", "stage=HEALTH_CHECK url=- reqId=- msg=success")
            } else {
                Log.e(TAG, "Business Engine health check failed: ${response.code}")
                Log.e(TAG, "Response body: ${response.body?.string()}")
                Log.e("TTT", "stage=HEALTH_CHECK url=- reqId=- msg=failed code=${response.code}")
            }
            
            response.close()
            isHealthy
        } catch (e: Exception) {
            Log.e(TAG, "Business Engine health check error: ${e.message}")
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Stack trace: ${e.stackTrace.joinToString("\n")}")
            Log.e("TTT", "stage=HEALTH_CHECK url=- reqId=- msg=exception ${e.message}")
            false
        }
    }

    /**
     * Test token vending endpoint
     */
    suspend fun testTokenVending(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Testing token vending endpoint...")
            Log.i("TTT", "stage=VENDING_TOKEN url=- reqId=- msg=requesting")
            
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
                Log.i("TTT", "stage=VENDING_TOKEN url=- reqId=- msg=success")
            } else {
                Log.e(TAG, "Token vending test failed: ${response.code}")
                Log.e("TTT", "stage=VENDING_TOKEN url=- reqId=- msg=failed code=${response.code}")
            }
            
            response.close()
            isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Token vending test error: ${e.message}")
            Log.e("TTT", "stage=VENDING_TOKEN url=- reqId=- msg=exception ${e.message}")
            false
        }
    }

    /**
     * Test TTTranscribe proxy endpoint
     */
    suspend fun testTTTranscribeProxy(token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Testing TTTranscribe proxy endpoint...")
            Log.i("TTT", "stage=TTTRANSCRIBE_CALL url=https://vm.tiktok.com/ZMAPTWV7o/ reqId=- msg=requesting")
            
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
                Log.i("TTT", "stage=TTTRANSCRIBE_CALL url=https://vm.tiktok.com/ZMAPTWV7o/ reqId=- msg=success")
            } else {
                Log.e(TAG, "TTTranscribe proxy test failed: ${response.code}")
                Log.e("TTT", "stage=TTTRANSCRIBE_CALL url=https://vm.tiktok.com/ZMAPTWV7o/ reqId=- msg=failed code=${response.code}")
            }
            
            response.close()
            isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "TTTranscribe proxy test error: ${e.message}")
            Log.e("TTT", "stage=TTTRANSCRIBE_CALL url=https://vm.tiktok.com/ZMAPTWV7o/ reqId=- msg=exception ${e.message}")
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
