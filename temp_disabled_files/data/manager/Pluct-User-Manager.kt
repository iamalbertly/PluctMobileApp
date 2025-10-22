package app.pluct.data.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import app.pluct.api.PluctCoreApiService
import app.pluct.api.CreateUserRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * User management for first-time onboarding and business engine integration
 */
@Singleton
class UserManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: PluctCoreApiService // Inject PluctCoreApiService
) {
    companion object {
        private const val TAG = "UserManager"
        private const val PREFS_NAME = "user_preferences"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_JWT = "user_jwt"
        private const val KEY_IS_FIRST_TIME = "is_first_time"
        private const val KEY_CREDITS = "credits"
        
        // Business Engine API
        private const val BUSINESS_ENGINE_URL = "https://pluct-business-engine.romeo-lya2.workers.dev/user/create"
        private const val INITIAL_CREDITS = 5
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Get or create user ID for first-time onboarding
     */
    suspend fun getOrCreateUserId(): String {
        return withContext(Dispatchers.IO) {
            try {
                val existingUserId = prefs.getString(KEY_USER_ID, null)
                if (existingUserId != null) {
                    Log.d(TAG, "Existing user ID found: $existingUserId")
                    return@withContext existingUserId
                }
                
                // Generate new user ID
                val newUserId = UUID.randomUUID().toString()
                Log.d(TAG, "Generated new user ID: $newUserId")
                
                // Save user ID locally
                prefs.edit()
                    .putString(KEY_USER_ID, newUserId)
                    .putBoolean(KEY_IS_FIRST_TIME, true)
                    .apply()
                
                // Register with business engine
                registerUserWithBusinessEngine(newUserId)
                
                newUserId
            } catch (e: Exception) {
                Log.e(TAG, "Error getting or creating user ID: ${e.message}", e)
                // Return a fallback user ID
                UUID.randomUUID().toString()
            }
        }
    }
    
    /**
     * Register user with business engine
     */
    private suspend fun registerUserWithBusinessEngine(userId: String) {
        try {
            Log.d(TAG, "Registering user with business engine: $userId")
            val request = CreateUserRequest(userId = userId, initialCredits = INITIAL_CREDITS)
            val response = apiService.createUser(request) // CLEAN API CALL

            if (response.isSuccessful) {
                Log.d(TAG, "User registered successfully.")
                // Save initial credits locally if needed
                prefs.edit()
                    .putInt(KEY_CREDITS, INITIAL_CREDITS)
                    .apply()
            } else {
                Log.e(TAG, "Failed to register user. Code: ${response.code()}, Body: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering user: ${e.message}", e)
        }
    }
    
    /**
     * Check if this is the first time the user is using the app
     */
    fun isFirstTimeUser(): Boolean {
        return prefs.getBoolean(KEY_IS_FIRST_TIME, true)
    }
    
    /**
     * Mark user as no longer first-time
     */
    fun markUserAsReturning() {
        prefs.edit()
            .putBoolean(KEY_IS_FIRST_TIME, false)
            .apply()
    }
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }
    
    /**
     * Get current credits
     */
    fun getCurrentCredits(): Int {
        return prefs.getInt(KEY_CREDITS, 0)
    }
    
    /**
     * Update credits
     */
    fun updateCredits(newCredits: Int) {
        prefs.edit()
            .putInt(KEY_CREDITS, newCredits)
            .apply()
    }
    
    /**
     * Check if user has sufficient credits
     */
    fun hasSufficientCredits(requiredCredits: Int = 1): Boolean {
        return getCurrentCredits() >= requiredCredits
    }
    
    /**
     * Deduct credits for a transaction
     */
    fun deductCredits(amount: Int = 1): Boolean {
        val currentCredits = getCurrentCredits()
        if (currentCredits >= amount) {
            updateCredits(currentCredits - amount)
            return true
        }
        return false
    }
    
    /**
     * Get or create user JWT for authentication
     */
    suspend fun getOrCreateUserJwt(): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "🎯 GETTING OR CREATING USER JWT")
                Log.i("JWT_GENERATION", "🎯 JWT GENERATION STARTED")
                
                val existingJwt = prefs.getString(KEY_USER_JWT, null)
                if (existingJwt != null) {
                    Log.i(TAG, "🎯 EXISTING USER JWT FOUND: ${existingJwt.take(20)}...")
                    Log.i("JWT_GENERATION", "🎯 EXISTING JWT FOUND: ${existingJwt.take(20)}...")
                    return@withContext existingJwt
                }
                
                Log.i(TAG, "🎯 NO EXISTING JWT - GENERATING NEW ONE")
                Log.i("JWT_GENERATION", "🎯 GENERATING NEW JWT")
                
                // Generate a proper JWT for mobile app authentication
                val userId = getOrCreateUserId()
                Log.i("JWT_GENERATION", "🎯 USER ID FOR JWT: $userId")
                
                val jwt = generateUserJWT(userId)
                
                // Save JWT locally
                prefs.edit()
                    .putString(KEY_USER_JWT, jwt)
                    .apply()
                
                Log.i(TAG, "🎯 GENERATED NEW USER JWT: ${jwt.take(20)}...")
                Log.i("JWT_GENERATION", "🎯 JWT GENERATION COMPLETED: ${jwt.take(20)}...")
                Log.i("JWT_GENERATION", "🎯 JWT FULL TOKEN: $jwt")
                
                jwt
            } catch (e: Exception) {
                Log.e(TAG, "Error getting or creating user JWT: ${e.message}", e)
                Log.e("JWT_GENERATION", "🎯 JWT GENERATION ERROR: ${e.message}")
                // Return a fallback JWT
                val fallbackJwt = generateUserJWT("mobile")
                Log.i("JWT_GENERATION", "🎯 FALLBACK JWT GENERATED: ${fallbackJwt.take(20)}...")
                fallbackJwt
            }
        }
    }
    
    /**
     * Generate a proper JWT for mobile app authentication with HMAC256 signing
     * Uses the same secret as the server for consistency
     */
    private fun generateUserJWT(userId: String): String {
        val now = System.currentTimeMillis() / 1000
        val payload = mapOf(
            "sub" to userId,
            "scope" to "ttt:transcribe", 
            "iat" to now,
            "exp" to (now + 900) // 15 minutes
        )
        
        // Use the same secret as the Business Engine
        val secret = "prod-jwt-secret-Z8qKsL2wDn9rFy6aVbP3tGxE0cH4mN5jR7sT1uC9e"
        
        // Create header
        val header = """{"alg":"HS256","typ":"JWT"}"""
        val headerB64 = android.util.Base64.encodeToString(header.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
        
        // Create payload
        val payloadJson = """{"sub":"$userId","scope":"ttt:transcribe","iat":$now,"exp":${now + 900}}"""
        val payloadB64 = android.util.Base64.encodeToString(payloadJson.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
        
        // Create signature using HMAC256
        val message = "$headerB64.$payloadB64"
        val signature = createHmacSha256Signature(message, secret)
        val signatureB64 = android.util.Base64.encodeToString(signature, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
        
        return "$headerB64.$payloadB64.$signatureB64"
    }
    
    /**
     * Create HMAC SHA256 signature for JWT
     */
    private fun createHmacSha256Signature(message: String, secret: String): ByteArray {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        val secretKey = javax.crypto.spec.SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(message.toByteArray())
    }
}
