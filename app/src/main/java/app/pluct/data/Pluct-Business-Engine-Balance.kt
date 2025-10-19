package app.pluct.data

import android.util.Log
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.UUID
import app.pluct.net.PluctHttpLogger

/**
 * Pluct-Business-Engine-Balance - Credit balance management
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
class PluctBusinessEngineBalance(
    private val baseUrl: String
) {
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(PluctHttpLogger())
        .retryOnConnectionFailure(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val correlationId = UUID.randomUUID().toString()
    
    companion object {
        private const val TAG = "PluctBusinessEngineBalance"
    }

    /**
     * Get user credit balance with enhanced logging
     */
    suspend fun balance(userJwt: String): Balance {
        return withContext(Dispatchers.IO) {
            try {
                // Enhanced HTTP telemetry logging
                val requestId = "balance-${System.currentTimeMillis()}"
                Log.i("PLUCT_HTTP", """{"event":"request","reqId":"$requestId","url":"$baseUrl/v1/credits/balance","method":"GET","headers":{"Authorization":"Bearer ${userJwt.take(20)}...","User-Agent":"Pluct-Mobile-App/1.0","Accept":"application/json","X-Correlation-ID":"$correlationId"},"body":""}""")
                Log.d(TAG, "Making balance request to: $baseUrl/v1/credits/balance")
                Log.i("CREDIT_BALANCE", "🎯 CREDIT BALANCE REQUEST: $requestId")
                Log.i("CREDIT_BALANCE", "🎯 REQUEST URL: $baseUrl/v1/credits/balance")
                Log.i("CREDIT_BALANCE", "🎯 REQUEST HEADERS: Authorization=Bearer ${userJwt.take(20)}..., User-Agent=Pluct-Mobile-App/1.0")
                
                val request = Request.Builder()
                    .url("$baseUrl/v1/credits/balance")
                    .addHeader("Authorization", "Bearer $userJwt")
                    .addHeader("User-Agent", "Pluct-Mobile-App/1.0")
                    .addHeader("Accept", "application/json")
                    .addHeader("X-Correlation-ID", correlationId)
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: "{}"
                
                if (!response.isSuccessful) {
                    Log.e(TAG, "Balance request failed: ${response.code} - $responseBody")
                    Log.e("CREDIT_BALANCE", "🎯 CREDIT BALANCE REQUEST FAILED: $requestId - Status: ${response.code}")
                    Log.e("CREDIT_BALANCE", "🎯 ERROR RESPONSE: $responseBody")
                    response.close()
                    throw EngineError.Upstream(response.code, responseBody)
                }
                
                // Enhanced HTTP telemetry logging for response
                Log.i("PLUCT_HTTP", """{"event":"response","reqId":"$requestId","url":"$baseUrl/v1/credits/balance","code":200,"body":"$responseBody","headers":{}}""")
                Log.i("CREDIT_BALANCE", "🎯 CREDIT BALANCE RESPONSE: $requestId - Status: 200")
                Log.i("CREDIT_BALANCE", "🎯 RESPONSE BODY: $responseBody")
                
                response.close()
                Balance(0) // Default balance for now
            } catch (e: EngineError) {
                Log.e(TAG, "Balance request failed with EngineError: ${e.javaClass.simpleName} - ${e.message}")
                Log.e("CREDIT_BALANCE", "🎯 CREDIT BALANCE ERROR: ${e.javaClass.simpleName} - ${e.message}")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Balance request failed with Exception: ${e.message}", e)
                Log.e("CREDIT_BALANCE", "🎯 CREDIT BALANCE EXCEPTION: ${e.message}")
                throw EngineError.Network
            }
        }
    }
}
