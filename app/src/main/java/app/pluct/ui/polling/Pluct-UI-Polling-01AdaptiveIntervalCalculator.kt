package app.pluct.ui.polling

import android.util.Log
import kotlin.math.min

/**
 * Pluct-UI-Polling-01AdaptiveIntervalCalculator - Adaptive polling interval management
 * Follows naming convention: [Project]-[Scope]-[Module]-[Feature]-[CoreResponsibility]
 * 
 * UX FIX #2: Adaptive polling intervals reduce backend load during long-running jobs
 * - Starts at 2 seconds
 * - Increases by 50% every 3 attempts (2s → 3s → 4.5s → 6.75s → 10s)
 * - Caps at 10 seconds maximum
 * - Reduces server load while maintaining acceptable responsiveness
 */
object PluctUIPolling01AdaptiveIntervalCalculator {
    private const val TAG = "AdaptivePolling"

    data class PollingConfig(
        val initialIntervalMs: Long = 2000L,      // Start at 2 seconds
        val scaleFactor: Float = 1.5f,             // Increase by 50% each cycle
        val maxIntervalMs: Long = 10000L,          // Cap at 10 seconds
        val scaleIntervalAttempts: Int = 3        // Scale every 3 attempts
    )

    /**
     * Calculate polling interval for given attempt number
     * Example progression: 2s, 2s, 2s (attempt 0-2), 3s, 3s, 3s (attempt 3-5), etc.
     */
    fun calculateNextPollIntervalMs(
        attemptNumber: Int,
        config: PollingConfig = PollingConfig()
    ): Long {
        // Every N attempts, increase the interval
        val scalingCycles = attemptNumber / config.scaleIntervalAttempts
        
        // Calculate interval with exponential scaling
        var interval = (config.initialIntervalMs * Math.pow(config.scaleFactor.toDouble(), scalingCycles.toDouble())).toLong()
        
        // Cap at maximum
        interval = min(interval, config.maxIntervalMs)
        
        Log.d(TAG, "📊 Polling interval for attempt $attemptNumber: ${interval}ms (scale cycle: $scalingCycles)")
        
        return interval
    }

    /**
     * Get total time estimate for polling with given max attempts
     * Useful for calculating timeout thresholds
     */
    fun estimateTotalPollingTimeMs(
        maxAttempts: Int,
        config: PollingConfig = PollingConfig()
    ): Long {
        var totalTime = 0L
        for (i in 0 until maxAttempts) {
            totalTime += calculateNextPollIntervalMs(i, config)
        }
        return totalTime
    }

    /**
     * Build human-readable description of polling strategy
     */
    fun describeStrategy(config: PollingConfig = PollingConfig()): String {
        return buildString {
            appendLine("Adaptive Polling Strategy:")
            appendLine("  - Initial interval: ${config.initialIntervalMs}ms")
            appendLine("  - Scale factor: ${config.scaleFactor}x every ${config.scaleIntervalAttempts} attempts")
            appendLine("  - Maximum interval: ${config.maxIntervalMs}ms")
            appendLine("  - Example: ${calculateNextPollIntervalMs(0, config)}ms → " +
                       "${calculateNextPollIntervalMs(3, config)}ms → " +
                       "${calculateNextPollIntervalMs(6, config)}ms")
        }
    }
}
