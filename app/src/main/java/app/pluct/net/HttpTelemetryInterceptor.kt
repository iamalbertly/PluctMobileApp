package app.pluct.net

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.Request
import java.io.IOException

/**
 * HTTP telemetry interceptor for comprehensive request/response logging
 */
class HttpTelemetryInterceptor : Interceptor {
    
    companion object {
        private const val TAG = "HttpTelemetry"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestId = java.util.UUID.randomUUID().toString()
        
        // Log request details
        logRequest(request, requestId)
        
        val startTime = System.currentTimeMillis()
        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            Log.e(TAG, "Request failed: ${e.message}")
            throw e
        }
        
        val duration = System.currentTimeMillis() - startTime
        
        // Log response details
        logResponse(response, requestId, duration)
        
        return response
    }
    
    private fun logRequest(request: Request, requestId: String) {
        val url = request.url.toString()
        val method = request.method
        val headers = request.headers.toMultimap()
        
        // Log HTTP request with telemetry
        Log.i("PLUCT_HTTP", """{"event":"request","reqId":"$requestId","url":"$url","method":"$method","headers":${headers.toString()},"body":"${request.body?.toString() ?: ""}"}""")
        
        // Log standard HTTP format for test compatibility
        Log.i("HTTP REQUEST", "$method $url")
        if (headers.containsKey("content-length")) {
            Log.i("HTTP REQUEST", "Content-Length: ${headers["content-length"]?.firstOrNull()}")
        }
        if (headers.containsKey("authorization")) {
            val auth = headers["authorization"]?.firstOrNull() ?: ""
            Log.i("HTTP REQUEST", "Authorization: ${auth.take(20)}...")
        }
    }
    
    private fun logResponse(response: Response, requestId: String, duration: Long) {
        val url = response.request.url.toString()
        val code = response.code
        val headers = response.headers.toMultimap()
        val body = response.peekBody(Long.MAX_VALUE).string()
        
        // Log HTTP response with telemetry
        Log.i("PLUCT_HTTP", """{"event":"response","reqId":"$requestId","url":"$url","code":$code,"duration":$duration,"headers":${headers.toString()},"body":"$body"}""")
        
        // Log standard HTTP format for test compatibility
        Log.i("HTTP RESPONSE", "$code in ${duration}ms")
        if (headers.containsKey("content-length")) {
            Log.i("HTTP RESPONSE", "Content-Length: ${headers["content-length"]?.firstOrNull()}")
        }
    }
}