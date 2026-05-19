package app.pluct.services

import android.content.Context
import android.provider.Settings
import android.util.Log
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
) {

    companion object {
        private const val TAG = "PluctUserIdentification"
        private const val DEFAULT_CREDITS = 7
    }

    private var _userId: String? = null
    val userId: String
        get() = _userId ?: generateUserId()

    private var _deviceId: String? = null
    val deviceId: String
        get() = _deviceId ?: generateDeviceId()

    init {
        _userId = generateUserId()
        _deviceId = generateDeviceId()
        Log.d(TAG, "Generated user identity")
    }

    /**
     * Generates a unique mobile-hyphenated username using device identifiers
     * Format: mobile-{hash}-{shortId}
     */
    private fun generateUserId(): String {
        try {
            val shortHash = generateStableDeviceHash()
            val userId = "mobile-$shortHash"
            Log.d(TAG, "Generated user ID prefix: ${userId.take(12)}")
            return userId
        } catch (e: Exception) {
            Log.e(TAG, "Error generating user ID: ${e.message}", e)
            return "mobile-error-fallback"
        }
    }

    private fun generateDeviceId(): String {
        return try {
            "device-${generateStableDeviceHash()}"
        } catch (e: Exception) {
            Log.e(TAG, "Error generating device ID: ${e.message}", e)
            "device-error-fallback"
        }
    }

    private fun generateStableDeviceHash(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        val compositeString = "$androidId-${android.os.Build.MODEL}-${android.os.Build.MANUFACTURER}"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(compositeString.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }.substring(0, 16)
    }

    /**
     * Get the default credits for new users
     */
    fun getDefaultCredits(): Int = DEFAULT_CREDITS

    /**
     * Validate that the user ID follows the expected format
     */
    fun isValidUserId(userId: String): Boolean {
        return userId.matches(Regex("mobile-[a-f0-9]{16}"))
    }

    /**
     * Get user identification details for logging/debugging
     */
    fun getUserIdentificationDetails(): Map<String, String> {
        return mapOf(
            "userId" to userId,
            "deviceId" to deviceId,
            "deviceModel" to android.os.Build.MODEL,
            "deviceManufacturer" to android.os.Build.MANUFACTURER,
            "isValidFormat" to isValidUserId(userId).toString()
        )
    }
}
