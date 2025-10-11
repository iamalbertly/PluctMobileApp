package app.pluct.data.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import app.pluct.data.service.ApiService
import app.pluct.data.service.CreateUserRequest
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
    private val apiService: ApiService // Inject ApiService
) {
    companion object {
        private const val TAG = "UserManager"
        private const val PREFS_NAME = "user_preferences"
        private const val KEY_USER_ID = "user_id"
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
}
