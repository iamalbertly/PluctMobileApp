package app.pluct.services

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.util.Base64

/**
 * Pluct-Core-API-01UnifiedService-02TokenRefresh-01Manager
 * Follows naming convention: [Project]-[Core]-[API]-[UnifiedService]-[TokenRefresh]-[Manager]
 * 6 scope layers: Project, Core, API, UnifiedService, TokenRefresh, Manager
 * Manages JWT token refresh proactively before expiration
 */
class PluctCoreAPI01UnifiedService02TokenRefresh01Manager(
    private val jwtGenerator: PluctCoreAPIJWTGenerator,
    private val userIdentification: PluctCoreUserIdentification
) {
    private val TAG = "TokenRefreshManager"
    private val REFRESH_THRESHOLD_SECONDS = 300L // Refresh 5 minutes before expiration
    
    /**
     * Check if token should be refreshed
     * Returns true if token expires within REFRESH_THRESHOLD_SECONDS
     */
    fun shouldRefreshToken(currentToken: String): Boolean {
        return try {
            val expirationTime = getTokenExpirationTime(currentToken)
            if (expirationTime == null) {
                Log.w(TAG, "Could not extract expiration time from token")
                return true // Refresh if we can't determine expiration
            }
            
            val now = System.currentTimeMillis() / 1000
            val timeUntilExpiration = expirationTime - now
            
            val shouldRefresh = timeUntilExpiration <= REFRESH_THRESHOLD_SECONDS
            if (shouldRefresh) {
                Log.d(TAG, "Token expires in ${timeUntilExpiration}s, should refresh")
            }
            
            shouldRefresh
        } catch (e: Exception) {
            Log.e(TAG, "Error checking token expiration: ${e.message}")
            true // Refresh on error to be safe
        }
    }
    
    /**
     * Extract expiration time from JWT token
     */
    private fun getTokenExpirationTime(token: String): Long? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            
            val payload = String(Base64.getUrlDecoder().decode(parts[1]))
            val json = Json { ignoreUnknownKeys = true }
            val jsonPayload = json.decodeFromString<JsonObject>(payload)
            
            val exp = jsonPayload["exp"]?.toString()?.trim('"')?.toLongOrNull()
            exp
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting expiration time: ${e.message}")
            null
        }
    }
    
    /**
     * Refresh token if needed
     * Returns new token if refreshed, null if not needed
     */
    fun refreshTokenIfNeeded(currentToken: String?): String? {
        if (currentToken == null) {
            Log.d(TAG, "No current token, generating new one")
            return jwtGenerator.generateUserJWT(userIdentification.userId)
        }
        
        if (shouldRefreshToken(currentToken)) {
            Log.d(TAG, "Refreshing token proactively")
            return jwtGenerator.generateUserJWT(userIdentification.userId)
        }
        
        return null // Token still valid
    }
    
    /**
     * Handle 401 error by refreshing token and retrying request
     */
    suspend fun <T> handle401Error(
        originalRequest: suspend (String) -> Result<T>
    ): Result<T> {
        Log.d(TAG, "Handling 401 error - refreshing token")
        
        // Generate new token
        val newToken = jwtGenerator.generateUserJWT(userIdentification.userId)
        
        // Retry original request with new token
        return try {
            originalRequest(newToken)
        } catch (e: Exception) {
            Log.e(TAG, "Retry after token refresh failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get time until token expiration in seconds
     */
    fun getTimeUntilExpiration(token: String): Long? {
        val expirationTime = getTokenExpirationTime(token) ?: return null
        val now = System.currentTimeMillis() / 1000
        return expirationTime - now
    }
}

