package app.pluct.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PluctApiErrorMapperTest {
    @Test
    fun mapsAuthenticationErrorsAsRetryable() {
        val result = PluctApiErrorMapper.map(401, "token expired")

        assertEquals(PluctApiErrorCategory.AUTHENTICATION, result.category)
        assertTrue(result.retryable)
    }

    @Test
    fun mapsValidationErrorsAsNonRetryable() {
        val result = PluctApiErrorMapper.map(400, "bad url")

        assertEquals(PluctApiErrorCategory.VALIDATION, result.category)
        assertFalse(result.retryable)
    }

    @Test
    fun mapsServerErrorsAsRetryable() {
        val result = PluctApiErrorMapper.map(503, "unavailable")

        assertEquals(PluctApiErrorCategory.SERVER, result.category)
        assertTrue(result.retryable)
    }
}
