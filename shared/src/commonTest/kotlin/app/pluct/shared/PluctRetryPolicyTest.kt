package app.pluct.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PluctRetryPolicyTest {
    @Test
    fun retriesTimeoutAndServerFailures() {
        assertTrue(PluctRetryPolicy.isRetryable(PluctApiError(message = "request timeout")))
        assertTrue(PluctRetryPolicy.isRetryable(PluctApiError(statusCode = 500)))
    }

    @Test
    fun doesNotRetryNormalClientFailure() {
        assertFalse(PluctRetryPolicy.isRetryable(PluctApiError(statusCode = 404, message = "not found")))
        assertFalse(PluctRetryPolicy.shouldCountForCircuitBreaker(PluctApiError(statusCode = 404)))
    }

    @Test
    fun calculatesCappedExponentialBackoff() {
        assertEquals(1000L, PluctRetryPolicy.calculateRetryDelayMs(1))
        assertEquals(2000L, PluctRetryPolicy.calculateRetryDelayMs(2))
        assertEquals(30000L, PluctRetryPolicy.calculateRetryDelayMs(10))
    }

    @Test
    fun clientPolicyUsesMinimumVersionCodeForHardUpdate() {
        val policy = """{"platforms":{"android":{"minimumVersionCode":42,"forceUpdate":false}}}"""

        assertTrue(PluctClientPolicyModels.isHardUpdateRequiredByCode(policy, 41))
        assertFalse(PluctClientPolicyModels.isHardUpdateRequiredByCode(policy, 42))
    }

    @Test
    fun clientPolicyReadsNestedApkUrlAndFeatureSubmitGate() {
        val policy = """{"platforms":{"android":{"apkUrl":"https://example.test/latest.apk"}},"features":{"transcriptionSubmit":false}}"""

        assertEquals("https://example.test/latest.apk", PluctClientPolicyModels.updateUrl(policy))
        assertTrue(PluctClientPolicyModels.isTranscribeDisabled(policy))
        assertNull(PluctClientPolicyModels.updateUrl("{}"))
    }

    @Test
    fun clientPolicyDetectsSoftUpdateAndProductionMessageFields() {
        val policy = """{"updateMode":"soft","messageShort":"Update available","messageDetail":"Latest Pluct is faster.","platforms":{"android":{"minimumVersionCode":3,"latestVersionCode":5,"apkUrl":"https://play.google.com/store/apps/details?id=app.pluct"}}}"""

        assertFalse(PluctClientPolicyModels.isHardUpdateRequiredByCode(policy, 3))
        assertTrue(PluctClientPolicyModels.isSoftUpdateAvailableByCode(policy, 4))
        assertFalse(PluctClientPolicyModels.isSoftUpdateAvailableByCode(policy, 5))
        assertEquals("Latest Pluct is faster.", PluctClientPolicyModels.updateMessage(policy, hardUpdateRequired = false, softUpdateAvailable = true))
        assertEquals(PluctClientPolicyModels.SOFT_UPDATE_CTA, PluctClientPolicyModels.ctaMessage(hardUpdateRequired = false, softUpdateAvailable = true))
        assertTrue(PluctClientPolicyModels.isHardUpdateCta(PluctClientPolicyModels.HARD_UPDATE_CTA))
    }
}
