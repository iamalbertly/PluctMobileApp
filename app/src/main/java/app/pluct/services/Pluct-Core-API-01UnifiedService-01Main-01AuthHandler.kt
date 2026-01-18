package app.pluct.services

import android.util.Log

/**
 * Pluct-Core-API-01UnifiedService-01Main-01AuthHandler
 * Follows naming convention: [Project]-[Core]-[API]-[UnifiedService]-[Main]-[AuthHandler]
 * 6 scope layers: Project, Core, API, UnifiedService, Main, AuthHandler
 * 
 * Single source of truth for 401 error handler preparation logic.
 * Extracted from UnifiedService01Main to reduce file size and improve maintainability.
 */
object PluctCoreAPIUnifiedService01MainAuthHandler {
    private const val TAG = "AuthHandler"
    
    /**
     * Prepare 401 error handler for retry operations
     * @param authToken Current auth token (if null, no retry handler is created)
     * @param requestId Request ID for logging
     * @param method HTTP method
     * @param endpoint API endpoint
     * @param payload Request payload
     * @param timeoutMs Request timeout in milliseconds
     * @param jwtGenerator JWT generator for creating new tokens
     * @param userIdentification User identification service
     * @param httpClient HTTP client for executing requests
     * @return Lambda function for retry with new token, or null if no auth token provided
     */
    fun <T> prepare401ErrorHandler(
        authToken: String?,
        requestId: String,
        method: String,
        endpoint: String,
        payload: Map<String, Any>?,
        timeoutMs: Long?,
        jwtGenerator: PluctCoreAPIJWTGenerator,
        userIdentification: PluctCoreUserIdentification,
        httpClient: PluctCoreAPIHTTPClientImpl
    ): (suspend () -> Result<T>)? {
        return if (authToken != null) {
            {
                val newToken = jwtGenerator.generateUserJWT(userIdentification.userId)
                Log.d(TAG, "Retrying request $requestId with refreshed token after 401 error")
                httpClient.executeRequest(method, endpoint, payload, newToken, timeoutMs) as Result<T>
            }
        } else {
            null
        }
    }
}
