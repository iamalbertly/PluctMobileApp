package app.pluct.data

import android.util.Log
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.UUID
import app.pluct.net.PluctNetworkHttp01Logger

/**
 * Pluct-Business-Engine-Metadata - Metadata fetching functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
class PluctBusinessEngineMetadata(
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
        private const val TAG = "PluctBusinessEngineMetadata"
    }

    /**
     * Fetch enhanced metadata for a video URL
     */
    suspend fun fetchMetadata(url: String): Metadata {
        return try {
            val httpUrl: HttpUrl = "$baseUrl/meta".toHttpUrl()
                .newBuilder()
                .addQueryParameter("url", url)
                .build()
            
            val request = Request.Builder()
                .url(httpUrl)
                .addHeader("User-Agent", "Pluct-Mobile-App/1.0")
                .addHeader("Accept", "application/json")
                .addHeader("X-Correlation-ID", correlationId)
                .get()
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }
            
            val responseBody = response.body?.string() ?: "{}"
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Metadata fetch failed: ${response.code} - $responseBody")
                response.close()
                throw EngineError.Upstream(response.code, responseBody)
            }
            
            val jsonResponse = JSONObject(responseBody)
            val title = jsonResponse.optString("title", "Unknown Title")
            val description = jsonResponse.optString("description", "No description available")
            
            Log.d(TAG, "Metadata fetched successfully: $title")
            response.close()
            Metadata(title, description)
        } catch (e: EngineError) {
            Log.e(TAG, "Metadata fetch failed with EngineError: ${e.javaClass.simpleName} - ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Metadata fetch failed with Exception: ${e.message}", e)
            throw EngineError.Network
        }
    }
}
