package app.pluct

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automation tests that cover the three flaggedJourneys and use Espresso where possible.
 * Each test is scoped to the existing MainActivity flow without inventing new UI elements.
 */
@RunWith(AndroidJUnit4::class)
class PluctMobileAndroidTestAutomationJourney01Instrumentation {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun intentAutoProcessingJourney() {
        val testUrl = TestConfig.TestUrls.VALID_VM_TIKTOK
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out this video: $testUrl")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        logStep("Intent journey: launching MainActivity with share intent")
        ActivityScenario.launch<PluctUIScreen01MainActivity>(shareIntent).use { _ ->
            waitFor("Let intent settle", 1200)
            logStep("Intent journey: verifying capture card and extract option")
            onView(withContentDescription("Video URL input field")).check(matches(isDisplayed()))
            onView(withContentDescription("Extract Script option")).check(matches(isDisplayed()))

            logStep("Intent journey: triggering extraction")
            onView(withContentDescription("Extract Script option")).perform(click())
            waitFor("Processing indicator should appear", 1500)
            onView(withContentDescription("Processing indicator")).check(matches(isDisplayed()))
        }
        logStep("Intent journey: completed")
    }

    @Test
    fun manualEntryJourney() {
        logStep("Manual entry: launching MainActivity")
        ActivityScenario.launch<PluctUIScreen01MainActivity>(Intent(context, PluctUIScreen01MainActivity::class.java)).use { _ ->
            waitFor("Manual entry: allow UI to settle", 800)
            logStep("Manual entry: typing TikTok URL")
            onView(withContentDescription("Video URL input field"))
                .perform(click(), replaceText(TestConfig.TestUrls.VALID_FULL_TIKTOK), closeSoftKeyboard())

            logStep("Manual entry: submitting via Extract Script button")
            onView(withContentDescription("Extract Script option")).perform(click())
            waitFor("Manual entry: processing should start", 1200)
            onView(withContentDescription("Processing indicator")).check(matches(isDisplayed()))
        }
        logStep("Manual entry: completed")
    }

    @Test
    fun creditsRequestJourney() {
        logStep("Credits request: launching MainActivity")
        ActivityScenario.launch<PluctUIScreen01MainActivity>(Intent(context, PluctUIScreen01MainActivity::class.java)).use { _ ->
            waitFor("Credits request: allow UI to render", 800)
            logStep("Credits request: opening settings dialog")
            onView(withContentDescription("Settings button")).perform(click())
            onView(withContentDescription("Settings dialog")).check(matches(isDisplayed()))

            logStep("Credits request: entering add credits mode")
            onView(withContentDescription("Add more credits")).perform(click())
            waitFor("Credits request: input visible", 500)

            logStep("Credits request: providing payment note")
            onView(withContentDescription("Paste payment confirmation message"))
                .perform(click(), replaceText("Paid via automation run"), closeSoftKeyboard())

            logStep("Credits request: submitting request")
            onView(withContentDescription("Send credits request")).perform(click())
            waitFor("Credits request: closing dialog", 800)
            onView(withContentDescription("Settings dialog")).check(doesNotExist())
        }
        logStep("Credits request: completed")
    }

    private fun waitFor(reason: String, duration: Long) {
        logStep("Waiting: $reason ($duration ms)")
        runBlocking { TestConfig.waitWithLog(duration, reason) }
    }

    private fun logStep(step: String) {
        println("AutomationTest: $step")
    }
}
