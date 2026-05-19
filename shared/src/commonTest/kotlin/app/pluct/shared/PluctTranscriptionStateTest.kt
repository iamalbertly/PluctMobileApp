package app.pluct.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PluctTranscriptionStateTest {
    @Test
    fun normalizesServerAliases() {
        assertEquals(PluctProcessingState.COMPLETED, PluctTranscriptionState.normalizeStatus("done"))
        assertEquals(PluctProcessingState.COMPLETED, PluctTranscriptionState.normalizeStatus("success"))
        assertEquals(PluctProcessingState.PROCESSING, PluctTranscriptionState.normalizeStatus("running"))
    }

    @Test
    fun identifiesTerminalStates() {
        assertTrue(PluctTranscriptionState.isTerminal("completed"))
        assertTrue(PluctTranscriptionState.isTerminal("failed"))
        assertFalse(PluctTranscriptionState.isTerminal("processing"))
    }
}
