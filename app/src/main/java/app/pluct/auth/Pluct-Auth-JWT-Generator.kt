package app.pluct.auth

import android.util.Log
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

/**
 * Pluct-Auth-JWT-Generator - JWT token generation for Business Engine API
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
object PluctAuthJWTGenerator {
    
    private const val TAG = "PluctAuthJWT"
    private const val JWT_SECRET = "prod-jwt-secret-Z8qKsL2wDn9rFy6aVbP3tGxE0cH4mN5jR7sT1uC9e"
    private const val REQUIRED_SCOPE = "ttt:transcribe"
    private const val TOKEN_DURATION_SECONDS = 900 // 15 minutes
    
    /**
     * Generate JWT token for Business Engine API authentication
     * @param userId User identifier (default: "mobile")
     * @return JWT token string
     */
    fun generateUserJWT(userId: String = "mobile"): String {
        try {
            val now = System.currentTimeMillis() / 1000
            val algorithm = Algorithm.HMAC256(JWT_SECRET)
            
            val token = JWT.create()
                .withSubject(userId)
                .withClaim("scope", REQUIRED_SCOPE)
                .withIssuedAt(Date(now * 1000))
                .withExpiresAt(Date((now + TOKEN_DURATION_SECONDS) * 1000))
                .sign(algorithm)
            
            Log.i(TAG, "🎯 JWT Generated for user: $userId")
            Log.i(TAG, "🎯 JWT Scope: $REQUIRED_SCOPE")
            Log.i(TAG, "🎯 JWT Expires: ${Date((now + TOKEN_DURATION_SECONDS) * 1000)}")
            
            return token
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to generate JWT: ${e.message}", e)
            throw RuntimeException("JWT generation failed", e)
        }
    }
    
    /**
     * Validate JWT token format (basic validation)
     * @param token JWT token to validate
     * @return true if token format is valid
     */
    fun isValidJWTFormat(token: String): Boolean {
        return try {
            val parts = token.split(".")
            parts.size == 3 && parts.all { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e(TAG, "❌ JWT format validation failed: ${e.message}")
            false
        }
    }
    
    /**
     * Get token expiration time
     * @param token JWT token
     * @return expiration timestamp in seconds, or null if invalid
     */
    fun getTokenExpiration(token: String): Long? {
        return try {
            val jwt = JWT.decode(token)
            jwt.expiresAt?.time?.div(1000)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to decode JWT expiration: ${e.message}")
            null
        }
    }
    
    /**
     * Check if token is expired
     * @param token JWT token
     * @return true if token is expired
     */
    fun isTokenExpired(token: String): Boolean {
        val expiration = getTokenExpiration(token) ?: return true
        val now = System.currentTimeMillis() / 1000
        return now >= expiration
    }
}
