package app.pluct.services

import android.util.Log
import app.pluct.core.api.PluctCoreAPI00Constants

/**
 * Pluct-Core-API-01UnifiedService-14Status-01Handler
 * Follows naming convention: [Project]-[Core]-[API]-[UnifiedService]-[Status]-[Handler]
 * 6 scope layers: Project, Core, API, UnifiedService, Status, Handler
 * Handles transcription status checking and polling operations
 * Extracted from UnifiedService-01Main to reduce file size and improve separation of concerns
 */
class PluctCoreAPI01UnifiedService14Status01Handler(
    private val tokenRefreshManager: PluctCoreAPI01UnifiedService02TokenRefresh01Manager,
    private val authRetryHandler: PluctCoreAPI01UnifiedService15AuthRetry01Handler,
    private val execute: suspend (String, String, Map<String, Any>?, String?, Long?) -> Result<*>
) {
    companion object {
        private const val TAG = "StatusHandler"
    }

    /**
     * Check transcription status using service token
     */
    suspend fun checkTranscriptionStatus(jobId: String, serviceToken: String): Result<TranscriptionStatusResponse> {
        return authRetryHandler.executeWithProactiveRefresh(
            currentToken = serviceToken,
            apiCall = { token ->
                @Suppress("UNCHECKED_CAST")
                execute("GET", "/ttt/status/$jobId", null, token, null) as Result<TranscriptionStatusResponse>
            },
            retryBlock = { newToken ->
                @Suppress("UNCHECKED_CAST")
                execute("GET", "/ttt/status/$jobId", null, newToken, null) as Result<TranscriptionStatusResponse>
            }
        )
    }

    /**
     * Poll transcription status using the new /ttt/poll/:id endpoint
     * This endpoint accepts user JWT (long-lived, 1 hour) instead of service token (15 min)
     * Recommended for long-running transcriptions that may exceed 15 minutes
     */
    suspend fun pollTranscriptionStatus(jobId: String, userJWT: String): Result<TranscriptionStatusResponse> {
        return authRetryHandler.executeWithProactiveRefresh(
            currentToken = userJWT,
            apiCall = { token ->
                @Suppress("UNCHECKED_CAST")
                execute("GET", "/ttt/poll/$jobId", null, token, null) as Result<TranscriptionStatusResponse>
            },
            retryBlock = { newToken ->
                @Suppress("UNCHECKED_CAST")
                execute("GET", "/ttt/poll/$jobId", null, newToken, null) as Result<TranscriptionStatusResponse>
            }
        )
    }
}
