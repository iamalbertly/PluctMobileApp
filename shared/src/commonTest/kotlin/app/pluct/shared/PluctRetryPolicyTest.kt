package app.pluct.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
}
