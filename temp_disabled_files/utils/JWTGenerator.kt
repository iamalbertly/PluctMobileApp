package app.pluct.utils

import android.util.Log
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

/**
 * JWT Generator for Business Engine authentication
 * Generates JWT tokens with proper claims for Business Engine API calls
 */
object JWTGenerator {
    
    private const val TAG = "JWTGenerator"
    // Test/Development secret - should be configurable in production
    private const val JWT_SECRET = "prod-jwt-secret-Z8qKsL2wDn9rFy6aVbP3tGxE0cH4mN5jR7sT1uC9e"
    private const val USER_ID = "mobile"
    private const val SCOPE = "ttt:transcribe"
    private const val TOKEN_DURATION_SECONDS = 900 // 15 minutes
    
    /**
     * Generate a user JWT token for Business Engine API calls
     * @return JWT token string
     */
    fun generateUserJWT(): String {
        return try {
            val now = System.currentTimeMillis() / 1000
            val algorithm = Algorithm.HMAC256(JWT_SECRET)
            
            val token = JWT.create()
                .withSubject(USER_ID)
                .withClaim("scope", SCOPE)
                .withIssuedAt(Date(now * 1000))
                .withExpiresAt(Date((now + TOKEN_DURATION_SECONDS) * 1000))
                .sign(algorithm)
            
            Log.d(TAG, "Generated JWT token for user: $USER_ID")
            Log.d(TAG, "Token expires in: $TOKEN_DURATION_SECONDS seconds")
            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate JWT token: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Generate a JWT token with custom user ID
     * @param userId Custom user ID
     * @return JWT token string
     */
    fun generateUserJWT(userId: String): String {
        return try {
            val now = System.currentTimeMillis() / 1000
            val algorithm = Algorithm.HMAC256(JWT_SECRET)
            
            val token = JWT.create()
                .withSubject(userId)
                .withClaim("scope", SCOPE)
                .withIssuedAt(Date(now * 1000))
                .withExpiresAt(Date((now + TOKEN_DURATION_SECONDS) * 1000))
                .sign(algorithm)
            
            Log.d(TAG, "Generated JWT token for user: $userId")
            Log.d(TAG, "Token expires in: $TOKEN_DURATION_SECONDS seconds")
            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate JWT token for user $userId: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Validate if a JWT token is still valid
     * @param token JWT token to validate
     * @return true if token is valid, false otherwise
     */
    fun isTokenValid(token: String): Boolean {
        return try {
            val algorithm = Algorithm.HMAC256(JWT_SECRET)
            val verifier = JWT.require(algorithm).build()
            verifier.verify(token)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Token validation failed: ${e.message}")
            false
        }
    }
}
