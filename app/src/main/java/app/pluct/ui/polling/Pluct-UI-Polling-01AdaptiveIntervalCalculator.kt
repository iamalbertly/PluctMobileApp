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
        val initialIntervalMs: Long = 15000L,
        val scaleFactor: Float = 2f,
        val maxIntervalMs: Long = 120000L,
        val scaleIntervalAttempts: Int = 4
    )

    /**
     * UX FIX #3: Incremental progress logging for visibility during long waits
     * Logs current progress state in structured format for logcat monitoring
     */
    fun logProgressIncrement(
        attemptNumber: Int,
        maxAttempts: Int,
        currentStatus: String?,
        jobId: String?,
        intervalMs: Long
    ) {
        val progressPercent = ((attemptNumber.toFloat() / maxAttempts) * 100).toInt().coerceIn(0, 99)
        val elapsedEstimate = estimateTotalPollingTimeMs(attemptNumber)
        Log.i(TAG, "PROGRESS[$progressPercent%] attempt=$attemptNumber/$maxAttempts | status=$currentStatus | job=${jobId?.take(8) ?: "pending"} | interval=${intervalMs}ms | elapsed~${elapsedEstimate}ms")
    }

    /**
     * Calculate polling interval for given attempt number
     * TECH DEBT #1: isBackground now properly used - background jobs poll 50% slower
     * Example progression (foreground): 2s, 2s, 2s (attempt 0-2), 3s, 3s, 3s (attempt 3-5), etc.
     * Example progression (background): 3s, 3s, 3s (attempt 0-2), 4.5s, 4.5s, 4.5s (attempt 3-5), etc.
     */
    fun calculateNextPollIntervalMs(
        attemptNumber: Int,
        isBackground: Boolean = false,
        config: PollingConfig = PollingConfig()
    ): Long {
        // Every N attempts, increase the interval
        val scalingCycles = attemptNumber / config.scaleIntervalAttempts

        // TECH DEBT #1: Background jobs use 50% longer initial interval to reduce battery/resource usage
        val effectiveInitialInterval = if (isBackground) {
            (config.initialIntervalMs * 2).toLong()
        } else {
            config.initialIntervalMs
        }

        // Calculate interval with exponential scaling
        var interval = (effectiveInitialInterval * Math.pow(config.scaleFactor.toDouble(), scalingCycles.toDouble())).toLong()

        // Cap at maximum (background uses same max)
        interval = min(interval, config.maxIntervalMs)

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
            totalTime += calculateNextPollIntervalMs(i, false, config)
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
            appendLine("  - Example: ${calculateNextPollIntervalMs(0, false, config)}ms → " +
                       "${calculateNextPollIntervalMs(3, false, config)}ms → " +
                       "${calculateNextPollIntervalMs(6, false, config)}ms")
        }
    }
}
