package app.pluct.services

import android.util.Log
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * PluctCoreAPIModels - JWT token generation and API model utilities
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
object PluctCoreAPIModels {
    
    private const val TAG = "PluctCoreAPIModels"
    private const val JWT_SECRET = "prod-jwt-secret-Z8qKsL2wDn9rFy6aVbP3tGxE0cH4mN5jR7sT1uC9e"
    
    /**
     * Generate JWT token for user authentication
     */
    fun generateUserJWT(userId: String): String {
        try {
            val now = System.currentTimeMillis() / 1000
            val header = """{"alg":"HS256","typ":"JWT"}"""
            val payload = """{"sub":"$userId","scope":"ttt:transcribe","iat":$now,"exp":${now + 900}}"""
            
            val headerEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(header.toByteArray())
            val payloadEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
            
            val signature = generateHMACSignature("$headerEncoded.$payloadEncoded", JWT_SECRET)
            
            return "$headerEncoded.$payloadEncoded.$signature"
        } catch (e: Exception) {
            Log.e(TAG, "Error generating JWT: ${e.message}", e)
            // Fallback to simplified token for development
            val now = System.currentTimeMillis() / 1000
            val header = """{"alg":"HS256","typ":"JWT"}"""
            val payload = """{"sub":"$userId","scope":"ttt:transcribe","iat":$now,"exp":${now + 900}}"""
            
            val headerEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(header.toByteArray())
            val payloadEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
            
            return "$headerEncoded.$payloadEncoded"
        }
    }
    
    /**
     * Generate HMAC signature for JWT
     */
    private fun generateHMACSignature(data: String, secret: String): String {
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            val secretKeySpec = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
            mac.init(secretKeySpec)
            val signature = mac.doFinal(data.toByteArray())
            Base64.getUrlEncoder().withoutPadding().encodeToString(signature)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating HMAC signature: ${e.message}", e)
            ""
        }
    }
}
