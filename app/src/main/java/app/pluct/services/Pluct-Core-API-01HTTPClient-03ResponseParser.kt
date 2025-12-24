package app.pluct.services

import android.util.Log
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Pluct-Core-API-01HTTPClient-03ResponseParser - Response parsing logic.
 */
class PluctCoreAPIHTTPClientResponseParser {

    companion object {
        private const val TAG = "PluctCoreAPIHTTPClient"
    }

    private val json = Json { ignoreUnknownKeys = true }

    fun parseErrorResponse(
        statusCode: Int,
        statusMessage: String,
        responseBody: String,
        endpoint: String
    ): ErrorInfo {
        return try {
            val jsonResponse = json.decodeFromString<JsonObject>(responseBody)
            val errorCode = jsonResponse["code"]?.toString()?.trim('"') ?: "unknown_error"
            val errorMessage = jsonResponse["message"]?.toString()?.trim('"') ?: statusMessage

            val details = jsonResponse["details"] as? JsonObject
            val upstreamStatus = details?.get("upstreamStatus")?.toString()?.trim('"')
            val upstreamResponse = details?.get("upstreamResponse")?.toString()?.trim('"')
            
            // Enhanced: Check multiple error field locations
            val upstreamError = details?.get("error")?.toString()?.trim('"')
                ?: details?.get("upstreamError")?.toString()?.trim('"')
                ?: details?.get("message")?.toString()?.trim('"')
                ?: jsonResponse["error"]?.toString()?.trim('"')
                .takeIf { it != null && it.isNotBlank() }

            ErrorInfo(
                statusCode = statusCode,
                errorCode = errorCode,
                errorMessage = errorMessage,
                upstreamStatus = upstreamStatus,
                upstreamResponse = upstreamResponse,
                upstreamError = upstreamError,
                fullResponse = responseBody,
                endpoint = endpoint
            )
        } catch (_: Exception) {
            // Fallback: Try to extract any error-like text from raw body
            val extractedError = extractErrorFromRawBody(responseBody)
            ErrorInfo(
                statusCode = statusCode,
                errorCode = "parse_error",
                errorMessage = extractedError ?: statusMessage,
                upstreamStatus = null,
                upstreamResponse = null,
                upstreamError = extractedError,
                fullResponse = responseBody,
                endpoint = endpoint
            )
        }
    }

    private fun extractErrorFromRawBody(body: String): String? {
        // Try to find error-like patterns in raw body
        val patterns = listOf(
            Regex("""(?i)"error"\s*:\s*"([^"]+)""""),
            Regex("""(?i)error[:\s]+([^\n,}]+)"""),
            Regex("""(?i)failed[:\s]+([^\n,}]+)""")
        )
        return patterns.firstNotNullOfOrNull { it.find(body)?.groupValues?.get(1)?.trim() }
    }

    fun parseResponse(endpoint: String, responseBody: String): Result<Any> {
        return try {
            val result = when {
                endpoint.contains("/credits/balance") -> json.decodeFromString<CreditBalanceResponse>(responseBody)
                endpoint.contains("/estimate") -> json.decodeFromString<EstimateResponse>(responseBody)
                endpoint.contains("/vend-token") -> json.decodeFromString<VendTokenResponse>(responseBody)
                endpoint.contains("/meta") -> json.decodeFromString<MetadataResponse>(responseBody)
                endpoint.contains("/ttt/transcribe") -> json.decodeFromString<TranscriptionResponse>(responseBody)
                endpoint.contains("/ttt/status") -> parseTranscriptionStatusResponse(responseBody, endpoint)
                else -> responseBody
            }
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse response: ${e.message}")
            Result.failure(e)
        }
    }

    private fun parseTranscriptionStatusResponse(responseBody: String, endpoint: String): TranscriptionStatusResponse {
        return try {
            json.decodeFromString(responseBody)
        } catch (e: Exception) {
            // Try alternate shape with nested result
            try {
                val jsonResponse = json.parseToJsonElement(responseBody).jsonObject
                val jobId = jsonResponse["jobId"]?.jsonPrimitive?.content ?: ""
                val status = jsonResponse["status"]?.jsonPrimitive?.content ?: "unknown"
                val progress = jsonResponse["progress"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val resultObj = jsonResponse["result"]?.jsonObject
                val transcription = resultObj?.get("transcription")?.jsonPrimitive?.content

                if (jobId.isNotEmpty() && status.isNotEmpty()) {
                    return TranscriptionStatusResponse(
                        jobId = jobId,
                        status = status,
                        progress = progress,
                        transcript = transcription,
                        result = resultObj?.let {
                            TranscriptionResult(
                                transcription = transcription,
                                confidence = it["confidence"]?.jsonPrimitive?.content?.toDoubleOrNull(),
                                language = it["language"]?.jsonPrimitive?.content,
                                duration = it["duration"]?.jsonPrimitive?.content?.toIntOrNull()
                            )
                        }
                    )
                }
            } catch (_: Exception) {
                // fall through
            }

            throw createDetailedParseError(
                service = "TTTranscribe Service (via Business Engine)",
                endpoint = endpoint,
                operation = "Check Transcription Status",
                expectedFormat = "TranscriptionStatusResponse { jobId: String, status: String, progress: Int, transcript: String?, ... }",
                actualResponse = responseBody,
                error = e
            )
        }
    }

    private fun createDetailedParseError(
        service: String,
        endpoint: String,
        operation: String,
        expectedFormat: String,
        actualResponse: String,
        error: Exception
    ): Exception {
        val errorMessage = buildString {
            appendLine("===== API RESPONSE PARSING ERROR =====")
            appendLine("Service: $service")
            appendLine("Endpoint: $endpoint")
            appendLine("Operation: $operation")
            appendLine()
            appendLine("Expected Response Format:")
            appendLine("  $expectedFormat")
            appendLine()
            appendLine("Actual Response Received:")
            appendLine("  ${actualResponse.take(500)}${if (actualResponse.length > 500) "..." else ""}")
            appendLine()
            appendLine("Parse Error:")
            appendLine("  ${error.message}")
            appendLine("======================================")
        }

        Log.e(TAG, errorMessage)
        return Exception(errorMessage)
    }
}
