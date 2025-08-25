package app.pluct

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay

/**
 * Test Configuration for Pluct App Automation Tests
 * 
 * This class contains all the configuration parameters and utilities
 * needed for running comprehensive automation tests.
 */
object TestConfig {
    
    // Test URLs for different scenarios
    object TestUrls {
        const val VALID_VM_TIKTOK = "https://vm.tiktok.com/ZMAF56hjK//"
        const val VALID_FULL_TIKTOK = "https://www.tiktok.com/@chris219m/video/7539882214209686840"
        const val VALID_VM_TIKTOK_2 = "https://vm.tiktok.com/ABC123/"
        const val INVALID_URL = "https://invalid-url.com"
        const val MALFORMED_URL = "not-a-url"
        const val NONEXISTENT_TIKTOK = "https://tiktok.com/nonexistent"
    }
    
    // Timeout configurations
    object Timeouts {
        const val WEBVIEW_LOAD = 10000L // 10 seconds
        const val AUTOMATION_COMPLETE = 30000L // 30 seconds
        const val RESULT_DETECTION = 60000L // 60 seconds
        const val ELEMENT_WAIT = 5000L // 5 seconds
        const val BETWEEN_TESTS = 2000L // 2 seconds
        const val BETWEEN_RUNS = 5000L // 5 seconds
    }
    
    // UI Element selectors
    object Selectors {
        const val WEBVIEW_CLASS = "android.webkit.WebView"
        const val PROCESSING_INDICATOR = "Get Transcript"
        const val TRANSCRIPT_DIALOG = "Transcript"
        const val NO_TRANSCRIPT_DIALOG = "No Transcript"
        const val ERROR_DIALOG = "Error"
        const val MAIN_SCREEN = "Pluct"
        const val CLOSE_BUTTON = "Close"
        const val COPY_BUTTON = "Copy"
        const val START_BUTTON = "START"
    }
    
    // Test parameters
    object TestParams {
        const val DEFAULT_TEST_RUNS = 3
        const val MIN_SUCCESS_RATE = 80.0 // 80%
        const val MAX_WORKFLOW_TIME = 60000L // 60 seconds
        const val POLLING_INTERVAL = 2000L // 2 seconds
        const val MAX_POLLING_ATTEMPTS = 30
    }
    
    // Performance thresholds
    object Performance {
        const val MAX_PAGE_LOAD_TIME = 5000L // 5 seconds
        const val MAX_AUTOMATION_TIME = 30000L // 30 seconds
        const val MAX_TOTAL_TIME = 60000L // 60 seconds
        const val MIN_SUCCESS_RATE = 80.0 // 80%
    }
    
    /**
     * Get the application context for tests
     */
    fun getTestContext(): Context {
        return InstrumentationRegistry.getInstrumentation().targetContext
    }
    
    /**
     * Get the instrumentation context for tests
     */
    fun getInstrumentationContext(): Context {
        return InstrumentationRegistry.getInstrumentation().context
    }
    
    /**
     * Wait for a specified duration with logging
     */
    suspend fun waitWithLog(duration: Long, reason: String) {
        println("TestConfig: Waiting $duration ms - $reason")
        delay(duration)
    }
    
    /**
     * Create a share intent for testing
     */
    fun createShareIntent(url: String, subject: String = "TikTok Video"): android.content.Intent {
        return android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, "Check out this video: $url")
            putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
        }
    }
    
    /**
     * Validate test results
     */
    fun validateTestResult(result: String): Boolean {
        return when {
            result.contains("Transcript received") -> true
            result.contains("No transcript available") -> true
            result.contains("Error occurred") -> false
            result.contains("Timeout") -> false
            else -> false
        }
    }
    
    /**
     * Calculate success rate
     */
    fun calculateSuccessRate(passed: Int, total: Int): Double {
        return if (total > 0) (passed.toDouble() / total.toDouble()) * 100.0 else 0.0
    }
    
    /**
     * Check if performance is acceptable
     */
    fun isPerformanceAcceptable(totalTime: Long, successRate: Double): Boolean {
        return totalTime <= Performance.MAX_TOTAL_TIME && successRate >= Performance.MIN_SUCCESS_RATE
    }
    
    /**
     * Get test URL by category
     */
    fun getTestUrlsByCategory(category: String): List<String> {
        return when (category.lowercase()) {
            "valid" -> listOf(
                TestUrls.VALID_VM_TIKTOK,
                TestUrls.VALID_FULL_TIKTOK,
                TestUrls.VALID_VM_TIKTOK_2
            )
            "invalid" -> listOf(
                TestUrls.INVALID_URL,
                TestUrls.MALFORMED_URL,
                TestUrls.NONEXISTENT_TIKTOK
            )
            "all" -> listOf(
                TestUrls.VALID_VM_TIKTOK,
                TestUrls.VALID_FULL_TIKTOK,
                TestUrls.VALID_VM_TIKTOK_2,
                TestUrls.INVALID_URL,
                TestUrls.MALFORMED_URL,
                TestUrls.NONEXISTENT_TIKTOK
            )
            else -> listOf(TestUrls.VALID_VM_TIKTOK)
        }
    }
    
    /**
     * Log test metrics
     */
    fun logTestMetrics(
        testName: String,
        duration: Long,
        success: Boolean,
        details: String = ""
    ) {
        val status = if (success) "PASSED" else "FAILED"
        val durationSeconds = duration / 1000.0
        println("TestConfig: [$testName] $status in ${durationSeconds}s $details")
    }
    
    /**
     * Log performance metrics
     */
    fun logPerformanceMetrics(
        totalTime: Long,
        successRate: Double,
        totalRuns: Int,
        passedRuns: Int
    ) {
        println("TestConfig: Performance Metrics:")
        println("  Total Time: ${totalTime}ms (${totalTime / 1000.0}s)")
        println("  Success Rate: ${String.format("%.2f", successRate)}%")
        println("  Total Runs: $totalRuns")
        println("  Passed Runs: $passedRuns")
        println("  Performance Acceptable: ${isPerformanceAcceptable(totalTime, successRate)}")
    }
}
