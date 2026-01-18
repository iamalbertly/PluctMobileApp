package app.pluct.services

import android.util.Log
import app.pluct.core.error.PluctCoreError01AuthErrorDetector

/**
 * Pluct-Core-API-01UnifiedService-15AuthRetry-01Handler
 * Follows naming convention: [Project]-[Core]-[API]-[UnifiedService]-[AuthRetry]-[Handler]
 * 6 scope layers: Project, Core, API, UnifiedService, AuthRetry, Handler
 * Single source of truth for 401 authentication error retry logic
 * Eliminates duplication across all API methods
 */
class PluctCoreAPI01UnifiedService15AuthRetry01Handler(
    private val tokenRefreshManager: PluctCoreAPI01UnifiedService02TokenRefresh01Manager
) {
    companion object {
        private const val TAG = "AuthRetryHandler"
    }

    /**
     * Execute API call with automatic 401 retry using token refresh
     * Single source of truth for auth retry pattern used across all API methods
     * 
     * @param initialResult The initial API call result
     * @param retryBlock Lambda to retry the API call with a new token
     * @return Result with automatic 401 retry if needed
     */
    suspend fun <T> executeWithAuthRetry(
        initialResult: Result<T>,
        retryBlock: suspend (String) -> Result<T>
    ): Result<T> {
        return if (initialResult.isFailure && 
            PluctCoreError01AuthErrorDetector.isAuthError(initialResult.exceptionOrNull())) {
            Log.d(TAG, "401 error detected, refreshing token and retrying")
            tokenRefreshManager.handle401Error { newToken ->
                retryBlock(newToken)
            }
        } else {
            initialResult
        }
    }

    /**
     * Execute API call with proactive token refresh and 401 retry
     * Checks if token should be refreshed before making the call
     * 
     * @param currentToken The current authentication token
     * @param apiCall Lambda to execute the API call with a token
     * @param retryBlock Lambda to retry the API call with a new token (for 401 errors)
     * @return Result with proactive refresh and automatic 401 retry
     */
    suspend fun <T> executeWithProactiveRefresh(
        currentToken: String,
        apiCall: suspend (String) -> Result<T>,
        retryBlock: suspend (String) -> Result<T>
    ): Result<T> {
        // Proactive token refresh before API call
        var token = currentToken
        val refreshedToken = tokenRefreshManager.refreshTokenIfNeeded(token)
        if (refreshedToken != null) {
            token = refreshedToken
            Log.d(TAG, "Token refreshed proactively before API call")
        }
        
        // Execute API call
        val result = apiCall(token)
        
        // Handle 401 errors with retry
        return executeWithAuthRetry(result, retryBlock)
    }
}
