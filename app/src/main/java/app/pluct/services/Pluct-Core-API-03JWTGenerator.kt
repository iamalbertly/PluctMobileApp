package app.pluct.services

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Pluct-Core-API-03JWTGenerator - JWT token generator for Business Engine
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Implements JWT token generation according to Business Engine documentation
 */
class PluctCoreAPIJWTGenerator {
    
    companion object {
        private const val TAG = "PluctCoreAPIJWTGenerator"
        private const val JWT_SECRET = "prod-jwt-secret-Z8qKsL2wDn9rFy6aVbP3tGxE0cH4mN5jR7sT1uC9e"
        private const val ALGORITHM = "HS256"
        private const val TOKEN_EXPIRY_SECONDS = 900L // 15 minutes
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    
        /**
         * Generate user JWT token for Business Engine authentication
         * According to documentation: Algorithm HS256, 15-minute expiration, scope: ttt:transcribe
         */
        fun generateUserJWT(userId: String): String {
            Log.d(TAG, "Generating JWT token for user: $userId")
            
            val now = System.currentTimeMillis() / 1000
            // Add clock skew compensation (Android device is ~5.6 hours behind)
            val clockSkewCompensation = 20150L // 5.6 hours in seconds
            val adjustedNow = now + clockSkewCompensation
            Log.d(TAG, "Current timestamp: $now")
            Log.d(TAG, "Adjusted timestamp: $adjustedNow")
            Log.d(TAG, "Expiration timestamp: ${adjustedNow + TOKEN_EXPIRY_SECONDS}")
            
            val payload = buildJsonObject {
                put("sub", JsonPrimitive(userId))
                put("scope", JsonPrimitive("ttt:transcribe"))
                put("iat", JsonPrimitive(adjustedNow))
                put("exp", JsonPrimitive(adjustedNow + TOKEN_EXPIRY_SECONDS))
            }
        
        val header = buildJsonObject {
            put("alg", JsonPrimitive(ALGORITHM))
            put("typ", JsonPrimitive("JWT"))
        }
        
        val encodedHeader = base64UrlEncode(json.encodeToString(JsonObject.serializer(), header).toByteArray())
        val encodedPayload = base64UrlEncode(json.encodeToString(JsonObject.serializer(), payload).toByteArray())
        
        val signature = generateSignature("$encodedHeader.$encodedPayload")
        
        val token = "$encodedHeader.$encodedPayload.$signature"
        Log.d(TAG, "Generated JWT token: ${token.take(50)}...")
        
        return token
    }
    
    /**
     * Generate HMAC-SHA256 signature for JWT
     */
    private fun generateSignature(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(JWT_SECRET.toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        val signature = mac.doFinal(data.toByteArray())
        return base64UrlEncode(signature)
    }
    
    /**
     * Base64 URL-safe encoding
     */
    private fun base64UrlEncode(data: ByteArray): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data)
    }
    
    /**
     * Validate JWT token (basic validation)
     */
    fun validateJWT(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return false
            
            val header = parts[0]
            val payload = parts[1]
            val signature = parts[2]
            
            val expectedSignature = generateSignature("$header.$payload")
            signature == expectedSignature
        } catch (e: Exception) {
            Log.e(TAG, "JWT validation failed: ${e.message}")
            false
        }
    }
    
    /**
     * Extract user ID from JWT token
     */
    fun extractUserId(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            
            val payload = String(Base64.getUrlDecoder().decode(parts[1]))
            val jsonPayload = json.decodeFromString<JsonObject>(payload)
            jsonPayload["sub"]?.toString()?.trim('"')
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract user ID: ${e.message}")
            null
        }
    }
}
