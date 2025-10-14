package app.pluct.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Business Engine Credit Manager - Handles user creation and credit management
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
object BusinessEngineCreditManager {
    private const val TAG = "BusinessEngineCreditManager"
    private const val BUSINESS_ENGINE_BASE_URL = "https://pluct-business-engine.romeo-lya2.workers.dev"
    
    // Configure HTTP client with proper timeouts
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Ensure user exists and has sufficient credits
     */
    suspend fun ensureUserWithCredits(userId: String = "mobile", initialCredits: Int = 10): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Ensuring user '$userId' has sufficient credits...")
            
            // Step 1: Try to get existing user info by attempting token vending
            if (checkUserExists(userId)) {
                Log.d(TAG, "User '$userId' already exists and has credits")
                return@withContext true
            }
            
            // Step 2: Create user if doesn't exist
            Log.d(TAG, "Creating user '$userId' with $initialCredits credits...")
            val createResult = createUser(userId, initialCredits)
            
            if (createResult) {
                Log.i(TAG, "User '$userId' created successfully with $initialCredits credits")
                return@withContext true
            } else {
                // Step 3: If creation failed with 409 (user exists), try to vend token anyway
                Log.w(TAG, "User creation failed, but user might already exist. Trying token vending...")
                val tokenVendResult = checkUserExists(userId)
                if (tokenVendResult) {
                    Log.i(TAG, "User '$userId' exists and has credits")
                    return@withContext true
                } else {
                    Log.e(TAG, "User '$userId' exists but has no credits")
                    return@withContext false
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring user credits: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Check if user exists by attempting to vend a token
     */
    private suspend fun checkUserExists(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("userId", userId)
            }
            
            val request = Request.Builder()
                .url("$BUSINESS_ENGINE_BASE_URL/vend-token")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = httpClient.newCall(request).execute()
            val exists = response.isSuccessful
            
            response.close()
            exists
        } catch (e: Exception) {
            Log.e(TAG, "Error checking user existence: ${e.message}")
            false
        }
    }

    /**
     * Create user with initial credits
     */
    private suspend fun createUser(userId: String, initialCredits: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("userId", userId)
                put("initialCredits", initialCredits)
            }
            
            val request = Request.Builder()
                .url("$BUSINESS_ENGINE_BASE_URL/user/create")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = httpClient.newCall(request).execute()
            val success = response.isSuccessful
            
            if (success) {
                Log.i(TAG, "User creation successful")
            } else {
                val errorCode = response.code
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "User creation failed: $errorCode - $errorBody")
                
                // Handle specific error codes
                when (errorCode) {
                    409 -> {
                        Log.w(TAG, "User already exists (409) - this is expected for existing users")
                        // Don't treat 409 as a failure - user exists
                        response.close()
                        return@withContext true
                    }
                    500 -> {
                        Log.e(TAG, "Business Engine server error (500)")
                    }
                    else -> {
                        Log.e(TAG, "Unexpected error code: $errorCode")
                    }
                }
            }
            
            response.close()
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user: ${e.message}")
            false
        }
    }

    /**
     * Get user credit balance
     */
    suspend fun getUserCredits(userId: String = "mobile"): Int? = withContext(Dispatchers.IO) {
        try {
            // This would need to be implemented in the Business Engine
            // For now, we'll return null to indicate unknown
            Log.d(TAG, "Getting credit balance for user '$userId'")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user credits: ${e.message}")
            null
        }
    }

    /**
     * Handle credit-related errors
     */
    fun handleCreditError(error: String, userId: String = "mobile") {
        Log.e(TAG, "Credit error for user '$userId': $error")
        
        when {
            error.contains("403") -> {
                Log.e(TAG, "Insufficient credits - user needs to purchase more credits")
                // TODO: Implement credit purchase flow
            }
            error.contains("401") -> {
                Log.e(TAG, "Authentication failed - user may not exist")
                // TODO: Implement user creation flow
            }
            error.contains("500") -> {
                Log.e(TAG, "Business Engine server error - retry later")
            }
            else -> {
                Log.e(TAG, "Unknown credit error: $error")
            }
        }
    }
}
