package app.pluct.data

import android.util.Log
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.UUID
import app.pluct.net.PluctNetworkHttp01Logger

/**
 * Pluct-Business-Engine-Health - Health check functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
class PluctBusinessEngineHealth(
    private val baseUrl: String
) {
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(PluctNetworkHttp01Logger())
        .retryOnConnectionFailure(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val correlationId = UUID.randomUUID().toString()
    
    companion object {
        private const val TAG = "PluctBusinessEngineHealth"
    }

    /**
     * Check Business Engine health with enhanced logging
     */
    suspend fun health(): Health {
        return try {
            val requestId = "health-${System.currentTimeMillis()}"
            Log.i("PLUCT_HTTP", """{"event":"request","reqId":"$requestId","url":"$baseUrl/health","method":"GET","headers":{"User-Agent":"Pluct-Mobile-App/1.0","Accept":"application/json","X-Correlation-ID":"$correlationId"},"body":""}""")
            Log.i("BUSINESS_ENGINE", "🎯 HEALTH CHECK REQUEST: $requestId")
            Log.i("BUSINESS_ENGINE", "🎯 REQUEST URL: $baseUrl/health")
            
            val request = Request.Builder()
                .url("$baseUrl/health")
                .addHeader("User-Agent", "Pluct-Mobile-App/1.0")
                .addHeader("Accept", "application/json")
                .addHeader("X-Correlation-ID", correlationId)
                .get()
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }
            
            val responseBody = response.body?.string() ?: "{}"
            val isHealthy = response.isSuccessful
            
            Log.d(TAG, "Health check: $isHealthy (${response.code})")
            Log.i("PLUCT_HTTP", """{"event":"response","reqId":"$requestId","url":"$baseUrl/health","code":${response.code},"body":"$responseBody","headers":{}}""")
            Log.i("BUSINESS_ENGINE", "🎯 HEALTH CHECK RESPONSE: $requestId - Status: ${response.code}")
            Log.i("BUSINESS_ENGINE", "🎯 RESPONSE BODY: $responseBody")
            
            response.close()
            Health(isHealthy)
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed: ${e.message}")
            Log.e("BUSINESS_ENGINE", "🎯 HEALTH CHECK ERROR: ${e.message}")
            throw EngineError.Network
        }
    }
}
