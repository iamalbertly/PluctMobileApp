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
import app.pluct.net.PluctNetworkHttp01Logger

/**
 * Pluct-Business-Engine-Transcription - Transcription functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
class PluctBusinessEngineTranscription(
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
        private const val TAG = "PluctBusinessEngineTranscription"
    }

    /**
     * Start transcription process
     */
    suspend fun transcribe(url: String, token: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.i("BUSINESS_ENGINE", "🎯 STARTING TRANSCRIPTION")
                Log.i("TTTRANSCRIBE", "🎯 TTTRANSCRIBE API CALL STARTED")
                
                val requestBody = JSONObject().apply {
                    put("url", url)
                }.toString()
                
                val request = Request.Builder()
                    .url("$baseUrl/v1/transcribe")
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("User-Agent", "Pluct-Mobile-App/1.0")
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Correlation-ID", correlationId)
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: "{}"
                
                if (!response.isSuccessful) {
                    Log.e(TAG, "Transcription failed: ${response.code} - $responseBody")
                    Log.e("BUSINESS_ENGINE", "🎯 TRANSCRIPTION FAILED: ${response.code}")
                    Log.e("TTTRANSCRIBE", "🎯 TTTRANSCRIBE API CALL FAILED: ${response.code}")
                    response.close()
                    throw EngineError.Upstream(response.code, responseBody)
                }
                
                val jsonResponse = JSONObject(responseBody)
                val requestId = jsonResponse.getString("requestId")
                
                Log.d(TAG, "Transcription started successfully: $requestId")
                Log.i("BUSINESS_ENGINE", "🎯 TRANSCRIPTION COMPLETED")
                Log.i("TTTRANSCRIBE", "🎯 TTTRANSCRIBE API CALL SUCCESSFUL")
                Log.i("TTTRANSCRIBE", "🎯 TRANSCRIPT LENGTH: ${requestId.length} characters")
                Log.i("TTTRANSCRIBE", "🎯 TRANSCRIPT PREVIEW: ${requestId.take(100)}...")
                
                response.close()
                requestId
            } catch (e: EngineError) {
                Log.e(TAG, "Transcription failed with EngineError: ${e.javaClass.simpleName} - ${e.message}")
                Log.e("BUSINESS_ENGINE", "🎯 TRANSCRIPTION ERROR: ${e.javaClass.simpleName} - ${e.message}")
                Log.e("TTTRANSCRIBE", "🎯 TTTRANSCRIBE API ERROR: ${e.javaClass.simpleName} - ${e.message}")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Transcription failed with Exception: ${e.message}", e)
                Log.e("BUSINESS_ENGINE", "🎯 TRANSCRIPTION EXCEPTION: ${e.message}")
                Log.e("TTTRANSCRIBE", "🎯 TTTRANSCRIBE API EXCEPTION: ${e.message}")
                throw EngineError.Network
            }
        }
    }
}