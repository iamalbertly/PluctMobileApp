package app.pluct.data

import android.util.Log
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.UUID
import app.pluct.net.PluctHttpLogger

/**
 * Pluct-Business-Engine-Token - Token vending functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
class PluctBusinessEngineToken(
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
        private const val TAG = "PluctBusinessEngineToken"
    }

    /**
     * Vend a short-lived token for transcription
     */
    suspend fun vendShortToken(userJwt: String, clientRequestId: String = UUID.randomUUID().toString()): VendResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.i("BUSINESS_ENGINE", "🎯 VENDING AUTHENTICATION TOKEN")
                Log.i("BUSINESS_ENGINE", "🎯 TOKEN REQUEST ID: $clientRequestId")
                
                val requestBody = JSONObject().apply {
                    put("clientRequestId", clientRequestId)
                }.toString()
                
                val request = Request.Builder()
                    .url("$baseUrl/v1/tokens/vend-short")
                    .addHeader("Authorization", "Bearer $userJwt")
                    .addHeader("User-Agent", "Pluct-Mobile-App/1.0")
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Correlation-ID", correlationId)
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: "{}"
                
                if (!response.isSuccessful) {
                    Log.e(TAG, "Token vending failed: ${response.code} - $responseBody")
                    Log.e("BUSINESS_ENGINE", "🎯 TOKEN VENDING FAILED: $clientRequestId - Status: ${response.code}")
                    Log.e("BUSINESS_ENGINE", "🎯 ERROR RESPONSE: $responseBody")
                    response.close()
                    throw EngineError.Upstream(response.code, responseBody)
                }
                
                val jsonResponse = JSONObject(responseBody)
                val token = jsonResponse.getString("token")
                val scope = jsonResponse.getString("scope")
                val expiresAt = jsonResponse.getString("expiresAt")
                val balanceAfter = jsonResponse.getInt("balanceAfter")
                
                Log.d(TAG, "Token vended successfully: ${token.take(20)}..., balanceAfter: $balanceAfter")
                Log.i("BUSINESS_ENGINE", "🎯 TOKEN VENDED: ${token.take(20)}...")
                Log.i("BUSINESS_ENGINE", "🎯 TOKEN SCOPE: $scope")
                Log.i("BUSINESS_ENGINE", "🎯 TOKEN EXPIRES: $expiresAt")
                Log.i("BUSINESS_ENGINE", "🎯 BALANCE AFTER: $balanceAfter")
                
                response.close()
                VendResult(token, scope, expiresAt, balanceAfter)
            } catch (e: EngineError) {
                Log.e(TAG, "Token vending failed with EngineError: ${e.javaClass.simpleName} - ${e.message}")
                Log.e("BUSINESS_ENGINE", "🎯 TOKEN VENDING ERROR: ${e.javaClass.simpleName} - ${e.message}")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Token vending failed with Exception: ${e.message}", e)
                Log.e("BUSINESS_ENGINE", "🎯 TOKEN VENDING EXCEPTION: ${e.message}")
                throw EngineError.Network
            }
        }
    }
}
