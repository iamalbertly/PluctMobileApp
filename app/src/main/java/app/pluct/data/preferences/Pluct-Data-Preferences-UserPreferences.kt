package app.pluct.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Pluct-Data-Preferences-UserPreferences - Client-side user preferences manager
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Manages free uses tracking using SharedPreferences for simplicity
 */
class PluctUserPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "pluct_user_preferences", 
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_FREE_USES_REMAINING = "free_uses_remaining"
        private const val KEY_PREFILLED_URL = "prefilled_url"
        private const val DEFAULT_FREE_USES = 3
        private const val KEY_INTENT_FEEDBACK_MESSAGE = "intent_feedback_message"
        private const val KEY_INTENT_FEEDBACK_IS_ERROR = "intent_feedback_is_error"
        private const val KEY_HAS_LAUNCHED_BEFORE = "has_launched_before"
        private const val KEY_PERMISSION_ONBOARDING_NOTIFICATION_SEEN = "permission_onboarding_notification_seen"
        private const val KEY_PERMISSION_ONBOARDING_OVERLAY_SEEN = "permission_onboarding_overlay_seen"
        private const val KEY_OVERLAY_NOTIFICATIONS_ENABLED = "overlay_notifications_enabled"
        private const val KEY_LAST_PERMISSION_CHECK_TIMESTAMP = "last_permission_check_timestamp"
        private const val KEY_ONBOARDING_TUTORIAL_SEEN = "onboarding_tutorial_seen"
        private const val KEY_FIRST_TRANSCRIPT_COMPLETED = "first_transcript_completed"
        private const val KEY_INLINE_HINT_ENABLED = "inline_hint_enabled"
        private const val KEY_INLINE_HINT_DISMISSED_AT = "inline_hint_dismissed_at"
        private const val KEY_THEME_MODE = "theme_mode" // "system", "light", "dark"
        
        /**
         * Check if this is the first time the user has launched the app
         */
        fun isFirstTimeUser(context: Context): Boolean {
            val prefs = context.getSharedPreferences("pluct_user_preferences", Context.MODE_PRIVATE)
            return !prefs.getBoolean(KEY_HAS_LAUNCHED_BEFORE, false)
        }
        
        /**
         * Mark the user as returning (not first time)
         */
        fun markUserAsReturning(context: Context) {
            val prefs = context.getSharedPreferences("pluct_user_preferences", Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_HAS_LAUNCHED_BEFORE, true).apply()
        }
        
        /**
         * Set prefilled URL from intent
         */
        fun setPrefilledUrl(context: Context, url: String) {
            val prefs = context.getSharedPreferences("pluct_user_preferences", Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_PREFILLED_URL, url).apply()
        }
        
        /**
         * Get and clear prefilled URL
         */
        fun getAndClearPrefilledUrl(context: Context): String? {
            val prefs = context.getSharedPreferences("pluct_user_preferences", Context.MODE_PRIVATE)
            val url = prefs.getString(KEY_PREFILLED_URL, null)
            if (url != null) {
                prefs.edit().remove(KEY_PREFILLED_URL).apply()
            }
            return url
        }

        /**
         * Persist intent feedback so UI can show it on next composition
         */
        fun setIntentFeedback(context: Context, message: String, isError: Boolean) {
            val prefs = context.getSharedPreferences("pluct_user_preferences", Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_INTENT_FEEDBACK_MESSAGE, message)
                .putBoolean(KEY_INTENT_FEEDBACK_IS_ERROR, isError)
                .apply()
        }

        /**
         * Consume (get and clear) the stored intent feedback message
         */
        fun getAndClearIntentFeedback(context: Context): IntentFeedback? {
            val prefs = context.getSharedPreferences("pluct_user_preferences", Context.MODE_PRIVATE)
            val message = prefs.getString(KEY_INTENT_FEEDBACK_MESSAGE, null)
            return if (message != null) {
                val isError = prefs.getBoolean(KEY_INTENT_FEEDBACK_IS_ERROR, true)
                prefs.edit()
                    .remove(KEY_INTENT_FEEDBACK_MESSAGE)
                    .remove(KEY_INTENT_FEEDBACK_IS_ERROR)
                    .apply()
                IntentFeedback(message, isError)
            } else {
                null
            }
        }
    }
    
    /**
     * Get current free uses remaining
     */
    fun getFreeUsesRemaining(): Int {
        return prefs.getInt(KEY_FREE_USES_REMAINING, DEFAULT_FREE_USES)
    }
    
    /**
     * Decrement free uses by 1
     */
    fun decrementFreeUses(): Int {
        val current = getFreeUsesRemaining()
        val newValue = (current - 1).coerceAtLeast(0)
        prefs.edit().putInt(KEY_FREE_USES_REMAINING, newValue).apply()
        return newValue
    }
    
    /**
     * Reset free uses to default value (for testing or admin purposes)
     */
    fun resetFreeUses(): Int {
        prefs.edit().putInt(KEY_FREE_USES_REMAINING, DEFAULT_FREE_USES).apply()
        return DEFAULT_FREE_USES
    }
    
    /**
     * Set free uses to specific value (for testing)
     */
    fun setFreeUsesRemaining(value: Int): Int {
        val clampedValue = value.coerceAtLeast(0)
        prefs.edit().putInt(KEY_FREE_USES_REMAINING, clampedValue).apply()
        return clampedValue
    }
    
    /**
     * Check if user has free uses remaining
     */
    fun hasFreeUsesRemaining(): Boolean {
        return getFreeUsesRemaining() > 0
    }
    
    /**
     * Check if user has seen notification permission onboarding
     */
    fun hasSeenNotificationOnboarding(): Boolean {
        return prefs.getBoolean(KEY_PERMISSION_ONBOARDING_NOTIFICATION_SEEN, false)
    }
    
    /**
     * Mark notification permission onboarding as seen
     */
    fun markNotificationOnboardingSeen() {
        prefs.edit().putBoolean(KEY_PERMISSION_ONBOARDING_NOTIFICATION_SEEN, true).apply()
    }
    
    /**
     * Check if user has seen overlay permission onboarding
     */
    fun hasSeenOverlayOnboarding(): Boolean {
        return prefs.getBoolean(KEY_PERMISSION_ONBOARDING_OVERLAY_SEEN, false)
    }
    
    /**
     * Mark overlay permission onboarding as seen
     */
    fun markOverlayOnboardingSeen() {
        prefs.edit().putBoolean(KEY_PERMISSION_ONBOARDING_OVERLAY_SEEN, true).apply()
    }
    
    /**
     * Check if overlay notifications are enabled
     * Default: true (enabled by default)
     */
    fun getOverlayNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_OVERLAY_NOTIFICATIONS_ENABLED, true)
    }
    
    /**
     * Set overlay notifications enabled/disabled
     */
    fun setOverlayNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OVERLAY_NOTIFICATIONS_ENABLED, enabled).apply()
    }
    
    /**
     * Get last permission check timestamp
     */
    fun getLastPermissionCheckTimestamp(): Long {
        return prefs.getLong(KEY_LAST_PERMISSION_CHECK_TIMESTAMP, 0)
    }
    
    /**
     * Set last permission check timestamp
     */
    fun setLastPermissionCheckTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_PERMISSION_CHECK_TIMESTAMP, timestamp).apply()
    }

    /**
     * Check if user has seen onboarding tutorial
     */
    fun hasSeenOnboardingTutorial(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_TUTORIAL_SEEN, false)
    }

    /**
     * Mark onboarding tutorial as seen
     */
    fun markOnboardingTutorialSeen() {
        prefs.edit().putBoolean(KEY_ONBOARDING_TUTORIAL_SEEN, true).apply()
    }

    /**
     * Check if user has completed their first transcript
     */
    fun isFirstTranscriptCompleted(): Boolean {
        return prefs.getBoolean(KEY_FIRST_TRANSCRIPT_COMPLETED, false)
    }

    /**
     * Mark first transcript as completed
     */
    fun markFirstTranscriptCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_TRANSCRIPT_COMPLETED, true).apply()
    }

    /**
     * Get theme mode: "system", "light", or "dark"
     */
    fun getThemeMode(): String {
        // Default dark matches product mockups (Customer: first paint matches designed UI; user can switch in Settings).
        return prefs.getString(KEY_THEME_MODE, "dark") ?: "dark"
    }

    /**
     * Set theme mode
     */
    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }
}

/**
 * Inline hint helper methods
 */
object PluctUserPreferencesInlineHint {
    private const val KEY_INLINE_HINT_ENABLED = "inline_hint_enabled"
    private const val KEY_INLINE_HINT_DISMISSED_AT = "inline_hint_dismissed_at"
    
    /**
     * Check if inline hint should be shown
     */
    fun getInlineHintEnabled(context: android.content.Context): Boolean {
        val prefs = context.getSharedPreferences("pluct_user_preferences", android.content.Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_INLINE_HINT_ENABLED, false)
        
        // Check if hint was dismissed more than 24 hours ago
        if (!enabled) {
            val dismissedAt = prefs.getLong(KEY_INLINE_HINT_DISMISSED_AT, 0)
            val now = System.currentTimeMillis()
            val hoursSinceDismissal = (now - dismissedAt) / (1000 * 60 * 60)
            
            // Re-enable hint after 24 hours if user still hasn't completed first transcript
            if (hoursSinceDismissal >= 24) {
                val userPrefs = PluctUserPreferences(context)
                if (!userPrefs.isFirstTranscriptCompleted()) {
                    return true
                }
            }
        }
        
        return enabled
    }
    
    /**
     * Enable inline hint (called when user skips tutorial)
     */
    fun setInlineHintEnabled(context: android.content.Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("pluct_user_preferences", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_INLINE_HINT_ENABLED, enabled).apply()
        
        if (!enabled) {
            // Record dismissal time for 24-hour reappearance logic
            prefs.edit().putLong(KEY_INLINE_HINT_DISMISSED_AT, System.currentTimeMillis()).apply()
        }
    }
}

/**
 * Simple data class for communicating feedback from intents to the UI layer
 */
data class IntentFeedback(
    val message: String,
    val isError: Boolean
)
