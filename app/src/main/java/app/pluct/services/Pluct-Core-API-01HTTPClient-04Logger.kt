package app.pluct.services

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Pluct-Core-API-01HTTPClient-04Logger - Unified API request/response logging.
 */
class PluctCoreAPIHTTPClientLogger {

    companion object {
        private const val TAG = "PluctCoreAPIHTTPClient"
        const val API_LOG_TAG = "PluctAPI"
    }

    private val json = Json { ignoreUnknownKeys = true }

    fun logRequest(
        requestId: String,
        timestamp: Long,
        method: String,
        fullUrl: String,
        endpoint: String,
        payload: Map<String, Any>? = null,
        authToken: String? = null
    ) {
        Log.d(API_LOG_TAG, "API REQUEST [$requestId]")
        Log.d(API_LOG_TAG, "   Timestamp: $timestamp")
        Log.d(API_LOG_TAG, "   Method: $method")
        Log.d(API_LOG_TAG, "   URL: $fullUrl")
        Log.d(API_LOG_TAG, "   Endpoint: $endpoint")

        if (payload != null) {
            val jsonPayload = json.encodeToString(
                JsonObject.serializer(),
                JsonObject(payload.mapValues { JsonPrimitive(it.value.toString()) })
            )
            Log.d(API_LOG_TAG, "   Payload: $jsonPayload")
        }

        if (authToken != null) {
            Log.d(API_LOG_TAG, "   Auth Token (truncated): ${authToken.take(30)}...${authToken.takeLast(10)}")
        }

        Log.d(TAG, "API REQUEST [$requestId] - $method $endpoint")
    }

    fun logResponse(
        requestId: String,
        responseTimestamp: Long,
        requestDuration: Long,
        responseCode: Int,
        responseMessage: String,
        responseBody: String? = null,
        headers: Map<String, List<String>>? = null
    ) {
        Log.d(API_LOG_TAG, "API RESPONSE [$requestId]")
        Log.d(API_LOG_TAG, "   Timestamp: $responseTimestamp")
        Log.d(API_LOG_TAG, "   Duration: ${requestDuration}ms")
        Log.d(API_LOG_TAG, "   Status: $responseCode $responseMessage")

        headers?.forEach { (key, values) ->
            if (key != null) {
                Log.d(API_LOG_TAG, "   Header: $key = ${values.joinToString(", ")}")
            }
        }

        responseBody?.let {
            Log.d(API_LOG_TAG, "   Response Body: $it")
        }

        Log.d(TAG, "API RESPONSE [$requestId] - $responseCode $responseMessage (${requestDuration}ms)")
    }

    fun logError(
        requestId: String,
        responseCode: Int,
        responseMessage: String,
        errorBody: String
    ) {
        Log.e(API_LOG_TAG, "API ERROR [$requestId]")
        Log.e(API_LOG_TAG, "   Status: $responseCode $responseMessage")
        Log.e(API_LOG_TAG, "   Error Body: ${errorBody.take(500)}${if (errorBody.length > 500) "..." else ""}")

        Log.e(TAG, "API ERROR [$requestId] - $responseCode $responseMessage")
    }

    fun logNetworkException(requestId: String, exception: Exception, exceptionType: String) {
        Log.e(API_LOG_TAG, "$exceptionType [$requestId]: ${exception.message}")
        Log.e(TAG, "$exceptionType [$requestId]: ${exception.message}")
    }

    fun logParseError(requestId: String, error: String) {
        Log.e(API_LOG_TAG, "Parse failed [$requestId]: $error")
        Log.e(TAG, "Parse failed [$requestId]: $error")
    }

    fun logSuccess(requestId: String) {
        Log.d(API_LOG_TAG, "Request succeeded [$requestId]")
        Log.d(TAG, "Request succeeded [$requestId]")
    }
}
