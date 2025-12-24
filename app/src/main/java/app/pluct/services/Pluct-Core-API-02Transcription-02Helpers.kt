package app.pluct.services

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Pluct-Core-API-02Transcription-02Helpers - Transcription flow helper functions
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 * Helper functions for transcription flow operations
 */
object PluctCoreAPITranscriptionHelpers {
    
    fun extractVendToken(response: VendTokenResponse): String? {
        return listOf(response.token, response.serviceToken, response.pollingToken)
            .firstOrNull { !it.isNullOrBlank() }
            ?.trim()
    }

    fun isSubmitTimeout(error: Throwable?): Boolean {
        if (error == null) return false
        val msg = error.message ?: ""
        if (error is PluctCoreAPIDetailedError) {
            val status = error.technicalDetails.responseStatusCode
            if (status == 408 || status == 504) return true
            if (error.technicalDetails.errorCode.contains("timeout", ignoreCase = true)) return true
            if (error.userMessage.contains("timed out", ignoreCase = true)) return true
        }
        return msg.contains("timeout", ignoreCase = true) || msg.contains("timed out", ignoreCase = true)
    }

    fun extractJobIdFromError(error: Throwable?): String? {
        if (error !is PluctCoreAPIDetailedError) return null
        val body = error.technicalDetails.responseBody.takeIf { it.isNotBlank() } ?: return null
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val obj = json.parseToJsonElement(body).jsonObject
            obj["jobId"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            Regex("""jobId"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)
        }
    }
}












