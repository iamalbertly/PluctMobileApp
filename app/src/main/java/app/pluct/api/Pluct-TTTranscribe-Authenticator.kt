package app.pluct.api

import android.util.Log
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct TTTranscribe Authenticator - Handles HMAC-SHA256 authentication
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Singleton
class PluctTTTranscribeAuthenticator @Inject constructor() {
    companion object {
        private const val TAG = "PluctTTTranscribeAuth"
        
        // TTTranscribe API credentials (from README)
        private const val API_KEY = "key_live_89f590e1f8cd3e4b19cfcf14"
        private const val API_SECRET = "b0b5638935304b247195ff2cece8ed3bb307e1728397fce07bd2158866c73fa6"
        private const val BASE_URL = "https://iamromeoly-tttranscibe.hf.space"
    }

    /**
     * Generate HMAC-SHA256 signature for TTTranscribe API
     */
    fun generateSignature(
        method: String,
        path: String,
        body: String,
        timestamp: String
    ): String {
        return try {
            val stringToSign = "$method\n$path\n$body\n$timestamp"
            val mac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(API_SECRET.toByteArray(), "HmacSHA256")
            mac.init(secretKey)
            
            val signature = mac.doFinal(stringToSign.toByteArray())
            signature.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating signature: ${e.message}", e)
            ""
        }
    }

    /**
     * Generate current timestamp in milliseconds
     */
    fun generateTimestamp(): String {
        return System.currentTimeMillis().toString()
    }

    /**
     * Get API key
     */
    fun getApiKey(): String = API_KEY

    /**
     * Get base URL
     */
    fun getBaseUrl(): String = BASE_URL

    /**
     * Create authentication headers for TTTranscribe API
     */
    fun createAuthHeaders(
        method: String,
        path: String,
        body: String
    ): Map<String, String> {
        val timestamp = generateTimestamp()
        val signature = generateSignature(method, path, body, timestamp)
        
        return mapOf(
            "X-API-Key" to API_KEY,
            "X-Timestamp" to timestamp,
            "X-Signature" to signature
        )
    }
}
