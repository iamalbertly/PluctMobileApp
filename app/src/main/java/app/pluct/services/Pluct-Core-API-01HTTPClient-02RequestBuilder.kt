package app.pluct.services

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Pluct-Core-API-01HTTPClient-02RequestBuilder - Request construction helper.
 */
class PluctCoreAPIHTTPClientRequestBuilder {

    companion object {
        private const val TAG = "PluctCoreAPIHTTPClient"
        private const val BASE_URL = "https://pluct-business-engine.romeo-lya2.workers.dev"
        private const val TIMEOUT_MS = 60000L
    }

    private val json = Json { ignoreUnknownKeys = true }

    fun buildConnection(
        method: String,
        endpoint: String,
        payload: Map<String, Any>? = null,
        authToken: String? = null,
        requestId: String,
        userId: String? = null,
        timeoutOverrideMs: Long? = null
    ): HttpURLConnection {
        val fullUrl = "$BASE_URL$endpoint"
        val url = URL(fullUrl)
        val connection = url.openConnection() as HttpURLConnection

        connection.doInput = true
        connection.useCaches = false
        val timeoutMs = timeoutOverrideMs ?: TIMEOUT_MS
        connection.requestMethod = method
        connection.connectTimeout = timeoutMs.toInt()
        connection.readTimeout = timeoutMs.toInt()
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("User-Agent", "PluctMobile/1.0")
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("X-Request-ID", requestId)
        connection.setRequestProperty("X-Client-Version", app.pluct.BuildConfig.VERSION_NAME)
        connection.setRequestProperty("X-Client-Platform", app.pluct.BuildConfig.PLATFORM)
        userId?.let {
            connection.setRequestProperty("X-User-Id", it)
            connection.setRequestProperty("X-Device-Id", it)
        }

        if (authToken != null) {
            connection.setRequestProperty("Authorization", "Bearer $authToken")
            Log.d(TAG, "Authorization header set")
        }

        if (payload != null && method in listOf("POST", "PUT", "PATCH")) {
            connection.doOutput = true
            // Use kotlinx.serialization for proper JSON encoding
            val jsonObject = buildJsonObject {
                payload.forEach { (key, value) ->
                    when (value) {
                        is String -> put(key, value)
                        is Int -> put(key, value)
                        is Long -> put(key, value)
                        is Double -> put(key, value)
                        is Float -> put(key, value)
                        is Boolean -> put(key, value)
                        else -> put(key, value.toString())
                    }
                }
            }
            val jsonPayload = json.encodeToString(JsonObject.serializer(), jsonObject)
            Log.d(TAG, "Request payload: $jsonPayload")
            val bytes = jsonPayload.toByteArray(StandardCharsets.UTF_8)
            connection.setRequestProperty("Content-Length", bytes.size.toString())
            connection.outputStream.use { os ->
                os.write(bytes)
                os.flush()
            }
        }

        return connection
    }

    fun readResponseBody(connection: HttpURLConnection): String {
        return java.io.BufferedReader(
            java.io.InputStreamReader(connection.inputStream)
        ).use { it.readText() }
    }

    fun readErrorBody(connection: HttpURLConnection): String {
        val errorStream = connection.errorStream
        return if (errorStream != null) {
            java.io.BufferedReader(
                java.io.InputStreamReader(errorStream)
            ).use { it.readText() }
        } else {
            "No error body available"
        }
    }
}
