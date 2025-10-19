package app.pluct.net

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.Request
import okio.Buffer
import java.io.IOException
import java.nio.charset.Charset
import java.util.UUID

/**
 * Consolidated HTTP logger for Pluct network requests
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[increment][CoreResponsibility]
 */
class PluctNetworkHttp01Logger : Interceptor {
    
    companion object {
        private const val TAG = "PLUCT_HTTP"
        private const val MAX_BODY_SIZE = 4096
        private val UTF8: Charset = Charsets.UTF_8
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestId = UUID.randomUUID().toString()
        val startTime = System.nanoTime()
        
        // Log request
        logRequest(request, requestId)
        
        return try {
            val response = chain.proceed(request)
            val endTime = System.nanoTime()
            val durationMs = (endTime - startTime) / 1_000_000
            
            // Log response
            logResponse(response, requestId, durationMs)
            response
            
        } catch (e: IOException) {
            Log.e(TAG, "Request failed: ${e.message}")
            throw e
        }
    }
    
    private fun logRequest(request: Request, requestId: String) {
        val method = request.method
        val url = request.url.toString()
        val headers = redactSensitiveHeaders(request.headers.toMultimap())
        val body = extractRequestBody(request)
        
        val requestJson = buildJson(mapOf(
            "event" to "request",
            "reqId" to requestId,
            "method" to method,
            "url" to url,
            "headers" to headers,
            "body" to body
        ))
        
        Log.i(TAG, "PLUCT_HTTP>OUT $requestJson")
    }
    
    private fun logResponse(response: Response, requestId: String, durationMs: Long) {
        val code = response.code
        val url = response.request.url.toString()
        val headers = redactSensitiveHeaders(response.headers.toMultimap())
        val body = extractResponseBody(response)
        
        val responseJson = buildJson(mapOf(
            "event" to "response",
            "reqId" to requestId,
            "code" to code,
            "url" to url,
            "headers" to headers,
            "body" to body,
            "durationMs" to durationMs
        ))
        
        Log.i(TAG, "PLUCT_HTTP>IN $responseJson")
    }
    
    private fun redactSensitiveHeaders(headers: Map<String, List<String>>): Map<String, List<String>> {
        return headers.mapValues { (key, values) ->
            when (key.lowercase()) {
                "authorization", "cookie", "x-api-key" -> 
                    values.map { "***REDACTED***" }
                else -> values
            }
        }
    }
    
    private fun extractRequestBody(request: Request): String {
        return try {
            request.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                val bodyString = buffer.readString(UTF8)
                if (bodyString.length > MAX_BODY_SIZE) {
                    bodyString.substring(0, MAX_BODY_SIZE) + "...(truncated)"
                } else {
                    bodyString
                }
            } ?: ""
        } catch (e: Exception) {
            "Error reading request body: ${e.message}"
        }
    }
    
    private fun extractResponseBody(response: Response): String {
        return try {
            val body = response.peekBody(MAX_BODY_SIZE.toLong())
            val bodyString = body.string()
            if (bodyString.length > MAX_BODY_SIZE) {
                bodyString.substring(0, MAX_BODY_SIZE) + "...(truncated)"
            } else {
                bodyString
            }
        } catch (e: Exception) {
            "Error reading response body: ${e.message}"
        }
    }
    
    private fun buildJson(map: Map<String, Any?>): String {
        fun escapeString(s: String): String {
            return s.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t")
        }
        
        fun formatValue(value: Any?): String = when (value) {
            null -> "null"
            is String -> "\"${escapeString(value)}\""
            is Number, is Boolean -> value.toString()
            is Map<*, *> -> {
                val entries = value.entries.joinToString(",") { (k, v) ->
                    "\"${escapeString(k.toString())}\":${formatValue(v)}"
                }
                "{$entries}"
            }
            is List<*> -> {
                val items = value.joinToString(",") { formatValue(it) }
                "[$items]"
            }
            else -> "\"${escapeString(value.toString())}\""
        }
        
        val entries = map.entries.joinToString(",") { (key, value) ->
            "\"${escapeString(key)}\":${formatValue(value)}"
        }
        return "{$entries}"
    }
}
