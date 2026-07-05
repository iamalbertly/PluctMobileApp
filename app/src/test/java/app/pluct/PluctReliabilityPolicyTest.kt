package app.pluct

import app.pluct.core.debug.PluctCoreDebug02DiagnosticShare01Builder
import app.pluct.data.entity.DebugLogEntry
import app.pluct.data.entity.LogLevel
import app.pluct.ui.polling.PluctUIPolling01AdaptiveIntervalCalculator
import app.pluct.premium.PluctAdResult
import app.pluct.premium.PluctAdSlot
import app.pluct.premium.PluctHouseAdProvider
import app.pluct.premium.PluctPremiumPromptPolicy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PluctReliabilityPolicyTest {
    @Test fun `foreground polling starts at fifteen seconds`() {
        assertEquals(15_000L, PluctUIPolling01AdaptiveIntervalCalculator.calculateNextPollIntervalMs(0))
    }

    @Test fun `long jobs back off to two minutes`() {
        assertEquals(120_000L, PluctUIPolling01AdaptiveIntervalCalculator.calculateNextPollIntervalMs(99))
    }

    @Test fun `background jobs start slower`() {
        assertEquals(30_000L, PluctUIPolling01AdaptiveIntervalCalculator.calculateNextPollIntervalMs(0, isBackground = true))
    }

    @Test fun `diagnostic bundle excludes URLs and secrets by default`() {
        val log = DebugLogEntry(
            id = 1,
            timestamp = 1,
            level = LogLevel.ERROR,
            category = "API",
            operation = "submit",
            message = "failed https://tiktok.com/private token=abc123",
            requestUrl = "https://tiktok.com/private"
        )
        val output = PluctCoreDebug02DiagnosticShare01Builder.buildText(listOf(log))
        assertFalse(output.contains("tiktok.com"))
        assertFalse(output.contains("abc123"))
        assertTrue(output.contains("<url-redacted>"))
    }

    @Test fun `premium prompt follows delivered value and cooldown`() {
        assertTrue(PluctPremiumPromptPolicy.shouldShow(7, "free", true, false, 0L, 100_000_000L))
        assertFalse(PluctPremiumPromptPolicy.shouldShow(7, "free", false, false, 0L, 100_000_000L))
        assertFalse(PluctPremiumPromptPolicy.shouldShow(7, "premium", true, false, 0L, 100_000_000L))
        assertFalse(PluctPremiumPromptPolicy.shouldShow(7, "free", true, true, 0L, 100_000_000L))
    }

    @Test fun `premium users never load house ads`() = runBlocking {
        assertTrue(PluctHouseAdProvider().load(PluctAdSlot.HISTORY_INLINE, "premium", true, false) is PluctAdResult.Suppressed)
    }
}
