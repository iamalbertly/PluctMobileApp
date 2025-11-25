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
}

/**
 * Simple data class for communicating feedback from intents to the UI layer
 */
data class IntentFeedback(
    val message: String,
    val isError: Boolean
)
