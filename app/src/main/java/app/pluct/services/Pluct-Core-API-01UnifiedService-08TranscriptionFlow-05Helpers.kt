package app.pluct.services

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.regex.Pattern

/**
 * Pluct-Core-API-01UnifiedService-08TranscriptionFlow-05Helpers
 * Follows naming convention: [Project]-[Core]-[API]-[UnifiedService]-[TranscriptionFlow]-[Helpers]
 * 6 scope layers: Project, Core, API, UnifiedService, TranscriptionFlow, Helpers
 * 
 * Single source of truth for transcription flow helper functions
 * Extracted from TranscriptionFlowHandler to reduce file size and improve maintainability
 */
object PluctCoreAPI01UnifiedService08TranscriptionFlow05Helpers {
    
    /**
     * Check if error is a submit timeout
     */
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

    /**
     * Extract jobId from error response body
     */
    fun extractJobIdFromError(error: Throwable?): String? {
        if (error !is PluctCoreAPIDetailedError) return null
        val body = error.technicalDetails.responseBody.takeIf { it.isNotBlank() } ?: return null
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val obj = json.parseToJsonElement(body).jsonObject
            obj["jobId"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            Pattern.compile("""jobId"\s*:\s*"([^"]+)"""").matcher(body).let {
                if (it.find()) it.group(1) else null
            }
        }
    }
}
