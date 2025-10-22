package app.pluct.net

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

/**
 * HTTP logger that emits request/response details in JSON format for Node.js test parsing
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
class PluctHttpLogger : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.nanoTime()
        
        // Log request
        val requestBody = request.body?.let { body ->
            try {
                val buffer = okio.Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            } catch (e: Exception) {
                "Error reading request body: ${e.message}"
            }
        } ?: ""
        
        val requestHeaders = request.headers.toMultimap()
        val requestJson = JSONObject().apply {
            put("method", request.method)
            put("url", request.url.toString())
            put("headers", JSONObject(requestHeaders))
            put("body", requestBody)
        }
        
        Log.i("pluct-http", "PLUCT_HTTP>OUT $requestJson")
        
        return try {
            val response = chain.proceed(request)
            val endTime = System.nanoTime()
            val durationMs = (endTime - startTime) / 1_000_000
            
            // Log response
            val responseBody = response.peekBody(1024 * 1024).string()
            val responseHeaders = response.headers.toMultimap()
            val responseJson = JSONObject().apply {
                put("code", response.code)
                put("url", request.url.toString())
                put("headers", JSONObject(responseHeaders))
                put("bodyMillis", durationMs)
                put("body", responseBody)
            }
            
            Log.i("pluct-http", "PLUCT_HTTP>IN $responseJson")
            response
            
        } catch (e: Exception) {
            val errorJson = JSONObject().apply {
                put("error", e.message ?: "Unknown error")
                put("url", request.url.toString())
            }
            Log.e("pluct-http", "PLUCT_HTTP>IN $errorJson")
            throw e
        }
    }
}