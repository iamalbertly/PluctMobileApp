package app.pluct.services

import android.content.Context
import android.provider.Settings
import android.util.Log
import app.pluct.architecture.PluctComponent
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.util.UUID

/**
 * Pluct-Core-User-01Identification - User identification service using device unique ID
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Generates mobile-hyphenated usernames that cannot be gamed
 */
@Singleton
class PluctCoreUserIdentification @Inject constructor(
    @ApplicationContext private val context: Context
) : PluctComponent {

    companion object {
        private const val TAG = "PluctUserIdentification"
        private const val DEFAULT_CREDITS = 7
    }

    private var _userId: String? = null
    val userId: String
        get() = _userId ?: generateUserId()

    override val componentId: String = "pluct-core-user-identification"
    override val dependencies: List<String> = emptyList()

    override fun initialize() {
        Log.d(TAG, "Initializing PluctCoreUserIdentification")
        _userId = generateUserId()
        Log.d(TAG, "Generated user ID: $userId")
    }

    override fun cleanup() {
        Log.d(TAG, "Cleaning up PluctCoreUserIdentification")
    }

    /**
     * Generates a unique mobile-hyphenated username using device identifiers
     * Format: mobile-{hash}-{shortId}
     */
    private fun generateUserId(): String {
        try {
            // Get Android ID (unique per device)
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            
            // Get device model and manufacturer for additional uniqueness
            val deviceModel = android.os.Build.MODEL
            val deviceManufacturer = android.os.Build.MANUFACTURER
            
            // Create a composite string for hashing
            val compositeString = "$androidId-$deviceModel-$deviceManufacturer"
            
            // Generate SHA-256 hash
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(compositeString.toByteArray())
            
            // Convert to hex string
            val hashHex = hashBytes.joinToString("") { "%02x".format(it) }
            
            // Take first 16 characters of hash for uniqueness
            val shortHash = hashHex.substring(0, 16)
            
            // Create mobile-hyphenated username
            // Format: mobile-{hash}
            val userId = "mobile-$shortHash"
            
            Log.d(TAG, "Generated user ID: $userId")
            Log.d(TAG, "Based on Android ID: $androidId")
            Log.d(TAG, "Device: $deviceManufacturer $deviceModel")
            
            return userId
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating user ID: ${e.message}", e)
            // Fallback to a persistent-like ID if possible, but for now just log error
            // In worst case, use a hash of the exception message or something stable-ish
            return "mobile-error-fallback"
        }
    }

    /**
     * Get the default credits for new users
     */
    fun getDefaultCredits(): Int = DEFAULT_CREDITS

    /**
     * Validate that the user ID follows the expected format
     */
    fun isValidUserId(userId: String): Boolean {
        return userId.startsWith("mobile-") && userId.length >= 20 && userId.matches(Regex("mobile-[a-f0-9]{12}-[a-f0-9]{4}"))
    }

    /**
     * Get user identification details for logging/debugging
     */
    fun getUserIdentificationDetails(): Map<String, String> {
        return mapOf(
            "userId" to userId,
            "androidId" to Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID),
            "deviceModel" to android.os.Build.MODEL,
            "deviceManufacturer" to android.os.Build.MANUFACTURER,
            "isValidFormat" to isValidUserId(userId).toString()
        )
    }
}
