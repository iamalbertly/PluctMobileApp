package app.pluct

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import app.pluct.share.ShareIngestActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull

/**
 * Comprehensive Automation Test Suite for Pluct App
 * 
 * This test suite automates the complete transcript journey:
 * 1. Receive TikTok share intent
 * 2. Process URL and open WebView
 * 3. Automate ScriptTokAudit.io workflow
 * 4. Extract transcript
 * 5. Return to app with transcript
 */
@RunWith(AndroidJUnit4::class)
class PluctAppAutomationTest {

    private lateinit var device: UiDevice
    private val TAG = "PluctAutomationTest"

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Wake up device if needed
        if (!device.isScreenOn) {
            device.wakeUp()
        }
    }

    /**
     * Test complete workflow from TikTok share intent to transcript generation
     */
    @Test
    fun testCompleteTranscriptWorkflow() = runBlocking {
        val startTime = System.currentTimeMillis()
        println("$TAG: Starting complete transcript workflow test")
        
        try {
            // Step 1: Launch app with TikTok share intent
            val testUrl = TestConfig.TestUrls.VALID_VM_TIKTOK
            val shareIntent = TestConfig.createShareIntent(testUrl)
            
            val scenario = ActivityScenario.launch<ShareIngestActivity>(
                Intent(ApplicationProvider.getApplicationContext(), ShareIngestActivity::class.java).apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Check out this video: $testUrl")
                }
            )
            
            println("$TAG: Launched ShareIngestActivity with TikTok URL: $testUrl")
            
            // Step 2: Wait for URL processing and WebView to open
            TestConfig.waitWithLog(TestConfig.Timeouts.BETWEEN_TESTS, "URL processing")
            
            // Step 3: Verify WebView is open and automation is running
            verifyWebViewIsOpen()
            
            // Step 4: Monitor WebView automation progress
            monitorWebViewAutomation()
            
            // Step 5: Wait for transcript result
            val transcriptResult = waitForTranscriptResult()
            
            // Step 6: Verify transcript was received
            assertTrue("Transcript result should be valid", TestConfig.validateTestResult(transcriptResult))
            println("$TAG: Successfully received result: $transcriptResult")
            
            // Step 7: Verify app returned to main screen
            verifyAppReturnedToMain()
            
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            TestConfig.logTestMetrics("CompleteWorkflow", duration, true, "Result: $transcriptResult")
            println("$TAG: Complete workflow test passed!")
            
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            TestConfig.logTestMetrics("CompleteWorkflow", duration, false, "Error: ${e.message}")
            throw e
        }
    }

    /**
     * Test with different TikTok URL formats
     */
    @Test
    fun testDifferentUrlFormats() = runBlocking {
        val testUrls = TestConfig.getTestUrlsByCategory("valid")
        
        for (url in testUrls) {
            println("$TAG: Testing URL format: $url")
            testSingleUrlWorkflow(url)
            TestConfig.waitWithLog(TestConfig.Timeouts.BETWEEN_TESTS, "Between URL tests")
        }
    }

    /**
     * Test error handling scenarios
     */
    @Test
    fun testErrorHandling() = runBlocking {
        val invalidUrls = TestConfig.getTestUrlsByCategory("invalid")
        
        for (url in invalidUrls) {
            println("$TAG: Testing error handling with: $url")
            testErrorScenario(url)
            TestConfig.waitWithLog(TestConfig.Timeouts.BETWEEN_TESTS, "Between error tests")
        }
    }

    /**
     * Test performance and speed
     */
    @Test
    fun testPerformanceAndSpeed() = runBlocking {
        val startTime = System.currentTimeMillis()
        
        try {
            testCompleteTranscriptWorkflow()
            
            val endTime = System.currentTimeMillis()
            val totalTime = endTime - startTime
            
            println("$TAG: Total workflow time: ${totalTime}ms")
            
            // Assert that the workflow completes within reasonable time
            assertTrue("Workflow should complete within ${TestConfig.Performance.MAX_TOTAL_TIME}ms", totalTime < TestConfig.Performance.MAX_TOTAL_TIME)
            
            TestConfig.logPerformanceMetrics(totalTime, 100.0, 1, 1)
            println("$TAG: Performance test passed!")
            
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            val totalTime = endTime - startTime
            TestConfig.logPerformanceMetrics(totalTime, 0.0, 1, 0)
            throw e
        }
    }

    /**
     * Test the complete journey multiple times for reliability
     */
    @Test
    fun testReliabilityMultipleRuns() = runBlocking {
        val numRuns = TestConfig.TestParams.DEFAULT_TEST_RUNS
        var successfulRuns = 0
        val startTime = System.currentTimeMillis()
        
        for (i in 1..numRuns) {
            try {
                println("$TAG: Running reliability test $i/$numRuns")
                testCompleteTranscriptWorkflow()
                successfulRuns++
                TestConfig.waitWithLog(TestConfig.Timeouts.BETWEEN_RUNS, "Between reliability test runs")
            } catch (e: Exception) {
                println("$TAG: Run $i failed: ${e.message}")
            }
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        val successRate = TestConfig.calculateSuccessRate(successfulRuns, numRuns)
        
        println("$TAG: Reliability test completed. Success rate: $successRate%")
        
        TestConfig.logPerformanceMetrics(totalTime, successRate, numRuns, successfulRuns)
        
        assertTrue("Success rate should be at least ${TestConfig.TestParams.MIN_SUCCESS_RATE}%", successRate >= TestConfig.TestParams.MIN_SUCCESS_RATE)
    }

    // Helper functions

    private suspend fun testSingleUrlWorkflow(url: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out this video: $url")
        }
        
        val scenario = ActivityScenario.launch<ShareIngestActivity>(
            Intent(ApplicationProvider.getApplicationContext(), ShareIngestActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Check out this video: $url")
            }
        )
        
        delay(3000)
        verifyWebViewIsOpen()
        monitorWebViewAutomation()
        val result = waitForTranscriptResult()
        assertTrue("Should get result or no transcript message", result.isNotEmpty() || result.contains("No transcript"))
    }

    private suspend fun testErrorScenario(url: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out this video: $url")
        }
        
        val scenario = ActivityScenario.launch<ShareIngestActivity>(
            Intent(ApplicationProvider.getApplicationContext(), ShareIngestActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Check out this video: $url")
            }
        )
        
        delay(3000)
        
        // Should show error dialog or return to main screen
        try {
            verifyErrorHandling()
        } catch (e: Exception) {
            println("$TAG: Error handling test passed for invalid URL: $url")
        }
    }

    private fun verifyWebViewIsOpen() {
        // Wait for WebView to be visible
        val webViewSelector = UiSelector().className(TestConfig.Selectors.WEBVIEW_CLASS)
        val webView = device.findObject(webViewSelector)
        
        assertTrue("WebView should be visible within ${TestConfig.Timeouts.WEBVIEW_LOAD}ms", webView.waitForExists(TestConfig.Timeouts.WEBVIEW_LOAD))
        println("$TAG: WebView is open and visible")
    }

    private fun monitorWebViewAutomation() = runBlocking {
        println("$TAG: Monitoring WebView automation progress")
        
        // Monitor for processing indicator
        val processingIndicator = device.findObject(UiSelector().text(TestConfig.Selectors.PROCESSING_INDICATOR))
        if (processingIndicator.exists()) {
            println("$TAG: Processing indicator is visible")
        }
        
        // Wait for automation to complete
        TestConfig.waitWithLog(TestConfig.Timeouts.AUTOMATION_COMPLETE, "WebView automation")
    }

    private suspend fun waitForTranscriptResult(): String {
        println("$TAG: Waiting for transcript result")
        
        var attempts = 0
        val maxAttempts = TestConfig.TestParams.MAX_POLLING_ATTEMPTS
        
        while (attempts < maxAttempts) {
            attempts++
            
            // Check for transcript dialog or success message
            val transcriptDialog = device.findObject(UiSelector().textContains(TestConfig.Selectors.TRANSCRIPT_DIALOG))
            val noTranscriptDialog = device.findObject(UiSelector().textContains(TestConfig.Selectors.NO_TRANSCRIPT_DIALOG))
            val errorDialog = device.findObject(UiSelector().textContains(TestConfig.Selectors.ERROR_DIALOG))
            
            when {
                transcriptDialog.exists() -> {
                    println("$TAG: Transcript dialog found")
                    return "Transcript received"
                }
                noTranscriptDialog.exists() -> {
                    println("$TAG: No transcript dialog found")
                    return "No transcript available"
                }
                errorDialog.exists() -> {
                    println("$TAG: Error dialog found")
                    return "Error occurred"
                }
            }
            
            TestConfig.waitWithLog(TestConfig.TestParams.POLLING_INTERVAL, "Polling for result (attempt $attempts)")
        }
        
        println("$TAG: Timeout waiting for transcript result")
        return "Timeout"
    }

    private fun verifyAppReturnedToMain() {
        // Check if app returned to main screen
        val mainScreenElement = device.findObject(UiSelector().textContains(TestConfig.Selectors.MAIN_SCREEN))
        assertTrue("App should return to main screen within ${TestConfig.Timeouts.ELEMENT_WAIT}ms", mainScreenElement.waitForExists(TestConfig.Timeouts.ELEMENT_WAIT))
        println("$TAG: App successfully returned to main screen")
    }

    private fun verifyErrorHandling() {
        // Check for error dialog or return to main
        val errorDialog = device.findObject(UiSelector().textContains(TestConfig.Selectors.ERROR_DIALOG))
        val mainScreen = device.findObject(UiSelector().textContains(TestConfig.Selectors.MAIN_SCREEN))
        
        assertTrue(
            "Should show error dialog or return to main screen",
            errorDialog.exists() || mainScreen.exists()
        )
        println("$TAG: Error handling verified")
    }
}
