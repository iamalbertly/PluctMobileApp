package app.pluct.services

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Authentication-01JWTManager - JWT token management and authentication
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation][CoreResponsibility]
 * Handles JWT token generation, validation, and refresh
 */
@Singleton
class PluctAuthenticationJWTManager @Inject constructor() {
    
    companion object {
        private const val TAG = "PluctJWTManager"
        
        // JWT Secret - matches the Business Engine
        private const val JWT_SECRET = "prod-jwt-secret-Z8qKsL2wDn9rFy6aVbP3tGxE0cH4mN5jR7sT1uC9e"
        
        // Token configuration
        private const val TOKEN_VALIDITY_MINUTES = 15
        private const val TOKEN_REFRESH_THRESHOLD_MINUTES = 5
        private const val MAX_TOKEN_AGE_MINUTES = 15
        
        // Required scope for transcription
        private const val REQUIRED_SCOPE = "ttt:transcribe"
        
        // Default user ID for mobile app
        private const val DEFAULT_USER_ID = "mobile"
    }
    
    data class TokenInfo(
        val token: String,
        val expiresAt: Date,
        val scope: String,
        val userId: String,
        val issuedAt: Date
    ) {
        val isExpired: Boolean
            get() = Date().after(expiresAt)
            
        val isNearExpiry: Boolean
            get() = Date().time + TimeUnit.MINUTES.toMillis(TOKEN_REFRESH_THRESHOLD_MINUTES.toLong()) >= expiresAt.time
            
        val timeUntilExpiry: Long
            get() = expiresAt.time - Date().time
    }
    
    private val tokenMutex = Mutex()
    private var currentToken: TokenInfo? = null
    
    /**
     * Generate a new JWT token for the mobile app
     */
    suspend fun generateToken(userId: String = DEFAULT_USER_ID): TokenInfo = tokenMutex.withLock {
        try {
            Log.d(TAG, "🔐 Generating new JWT token for user: $userId")
            
            val now = System.currentTimeMillis() / 1000L
            val expiresAt = now + TimeUnit.MINUTES.toSeconds(TOKEN_VALIDITY_MINUTES.toLong())
            
            // Create JWT payload
            val payload = mapOf(
                "sub" to userId,                    // Subject (User ID)
                "scope" to REQUIRED_SCOPE,         // Required scope for transcription
                "iat" to now,                      // Issued at timestamp
                "exp" to expiresAt                 // Expiration timestamp
            )
            
            // Generate JWT token using HMAC256
            val token = generateJWTToken(payload, JWT_SECRET)
            
            val tokenInfo = TokenInfo(
                token = token,
                expiresAt = Date(expiresAt * 1000),
                scope = REQUIRED_SCOPE,
                userId = userId,
                issuedAt = Date(now * 1000)
            )
            
            currentToken = tokenInfo
            
            Log.d(TAG, "✅ JWT token generated successfully")
            Log.d(TAG, "   User ID: $userId")
            Log.d(TAG, "   Scope: $REQUIRED_SCOPE")
            Log.d(TAG, "   Expires at: ${tokenInfo.expiresAt}")
            Log.d(TAG, "   Time until expiry: ${tokenInfo.timeUntilExpiry}ms")
            
            return tokenInfo
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to generate JWT token: ${e.message}", e)
            throw Exception("Failed to generate JWT token: ${e.message}", e)
        }
    }
    
    /**
     * Get current valid token or generate a new one
     */
    suspend fun getValidToken(userId: String = DEFAULT_USER_ID): TokenInfo = tokenMutex.withLock {
        val current = currentToken
        
        return when {
            // No token exists
            current == null -> {
                Log.d(TAG, "🔄 No current token, generating new one")
                generateToken(userId)
            }
            
            // Token is expired
            current.isExpired -> {
                Log.d(TAG, "🔄 Current token expired, generating new one")
                generateToken(userId)
            }
            
            // Token is near expiry
            current.isNearExpiry -> {
                Log.d(TAG, "🔄 Current token near expiry, generating new one")
                generateToken(userId)
            }
            
            // Token is still valid
            else -> {
                Log.d(TAG, "✅ Using existing valid token")
                Log.d(TAG, "   Time until expiry: ${current.timeUntilExpiry}ms")
                current
            }
        }
    }
    
    /**
     * Validate token format and expiration
     */
    fun validateToken(token: String): Boolean {
        return try {
            Log.d(TAG, "🔍 Validating token format and expiration")
            
            // Basic JWT format validation (header.payload.signature)
            val parts = token.split(".")
            if (parts.size != 3) {
                Log.w(TAG, "❌ Invalid JWT format: expected 3 parts, got ${parts.size}")
                return false
            }
            
            // Decode payload (base64url)
            val payload = decodeJWTPayload(parts[1])
            if (payload == null) {
                Log.w(TAG, "❌ Failed to decode JWT payload")
                return false
            }
            
            // Check expiration
            val exp = payload["exp"] as? Number
            if (exp == null) {
                Log.w(TAG, "❌ No expiration claim in token")
                return false
            }
            
            val now = System.currentTimeMillis() / 1000
            val isExpired = now >= exp.toLong()
            
            if (isExpired) {
                Log.w(TAG, "❌ Token expired at ${Date(exp.toLong() * 1000)}")
                return false
            }
            
            // Check scope
            val scope = payload["scope"] as? String
            if (scope != REQUIRED_SCOPE) {
                Log.w(TAG, "❌ Invalid scope: expected '$REQUIRED_SCOPE', got '$scope'")
                return false
            }
            
            Log.d(TAG, "✅ Token validation successful")
            Log.d(TAG, "   Expires at: ${Date(exp.toLong() * 1000)}")
            Log.d(TAG, "   Scope: $scope")
            
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Token validation failed: ${e.message}", e)
            false
        }
    }
    
    /**
     * Clear current token (force refresh on next request)
     */
    suspend fun clearToken() = tokenMutex.withLock {
        Log.d(TAG, "🗑️ Clearing current token")
        currentToken = null
    }
    
    /**
     * Get token information without validation
     */
    fun getTokenInfo(): TokenInfo? = currentToken
    
    /**
     * Check if current token is valid
     */
    fun isTokenValid(): Boolean {
        val current = currentToken
        return current != null && !current.isExpired
    }
    
    /**
     * Get time until token expiry
     */
    fun getTimeUntilExpiry(): Long? {
        return currentToken?.timeUntilExpiry
    }
    
    /**
     * Generate JWT token using HMAC256
     */
    private fun generateJWTToken(payload: Map<String, Any>, secret: String): String {
        try {
            // JWT Header
            val header = mapOf(
                "alg" to "HS256",
                "typ" to "JWT"
            )
            
            // Encode header
            val headerEncoded = base64UrlEncode(header.toJson())
            
            // Encode payload
            val payloadEncoded = base64UrlEncode(payload.toJson())
            
            // Create signature
            val signature = createHMACSignature("$headerEncoded.$payloadEncoded", secret)
            val signatureEncoded = base64UrlEncode(signature)
            
            return "$headerEncoded.$payloadEncoded.$signatureEncoded"
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to generate JWT token: ${e.message}", e)
            throw Exception("JWT token generation failed: ${e.message}", e)
        }
    }
    
    /**
     * Decode JWT payload
     */
    private fun decodeJWTPayload(encodedPayload: String): Map<String, Any>? {
        return try {
            // Add padding if needed
            val padded = encodedPayload.padEnd(encodedPayload.length + (4 - encodedPayload.length % 4) % 4, '=')
            
            // Decode base64url
            val decoded = android.util.Base64.decode(padded, android.util.Base64.URL_SAFE)
            val json = String(decoded)
            
            // Parse JSON (simplified - in production use proper JSON library)
            parseJsonToMap(json)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to decode JWT payload: ${e.message}", e)
            null
        }
    }
    
    /**
     * Create HMAC signature
     */
    private fun createHMACSignature(data: String, secret: String): ByteArray {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        val secretKey = javax.crypto.spec.SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(data.toByteArray())
    }
    
    /**
     * Base64URL encode
     */
    private fun base64UrlEncode(data: String): String {
        val encoded = android.util.Base64.encodeToString(data.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING)
        return encoded
    }
    
    /**
     * Base64URL encode ByteArray
     */
    private fun base64UrlEncode(data: ByteArray): String {
        val encoded = android.util.Base64.encodeToString(data, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING)
        return encoded
    }
    
    /**
     * Base64URL decode
     */
    private fun base64UrlDecode(encoded: String): String {
        val decoded = android.util.Base64.decode(encoded, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING)
        return String(decoded)
    }
    
    /**
     * Convert map to JSON string (simplified)
     */
    private fun Map<String, Any>.toJson(): String {
        val json = StringBuilder()
        json.append("{")
        entries.forEachIndexed { index, (key, value) ->
            if (index > 0) json.append(",")
            json.append("\"$key\":")
            when (value) {
                is String -> json.append("\"$value\"")
                is Number -> json.append(value.toString())
                is Boolean -> json.append(value.toString())
                else -> json.append("\"$value\"")
            }
        }
        json.append("}")
        return json.toString()
    }
    
    /**
     * Parse JSON to map (simplified - in production use proper JSON library)
     */
    private fun parseJsonToMap(json: String): Map<String, Any> {
        // This is a simplified JSON parser for JWT payloads
        // In production, use a proper JSON library like Gson or Moshi
        val map = mutableMapOf<String, Any>()
        
        // Remove braces
        val content = json.trim().removePrefix("{").removeSuffix("}")
        
        // Split by commas (this is simplified and may not handle all cases)
        val pairs = content.split(",")
        
        for (pair in pairs) {
            val keyValue = pair.split(":", limit = 2)
            if (keyValue.size == 2) {
                val key = keyValue[0].trim().removeSurrounding("\"")
                val value = keyValue[1].trim()
                
                when {
                    value.startsWith("\"") && value.endsWith("\"") -> {
                        map[key] = value.removeSurrounding("\"")
                    }
                    value == "true" -> map[key] = true
                    value == "false" -> map[key] = false
                    value.matches(Regex("\\d+")) -> map[key] = value.toLong()
                    value.matches(Regex("\\d+\\.\\d+")) -> map[key] = value.toDouble()
                    else -> map[key] = value
                }
            }
        }
        
        return map
    }
}
