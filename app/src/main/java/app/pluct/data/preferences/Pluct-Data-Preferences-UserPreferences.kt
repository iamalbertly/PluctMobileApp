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
        private const val DEFAULT_FREE_USES = 3
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
