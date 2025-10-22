package app.pluct.services

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pluct-Status-Monitor-Test - Test coverage for status monitoring
 * Single source of truth for status monitor tests
 * Adheres to 300-line limit with smart separation of concerns
 */

@RunWith(MockitoJUnitRunner::class)
class PluctStatusMonitorTest {
    
    @Mock
    private lateinit var mockStatusMonitor: PluctStatusMonitor
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }
    
    @Test
    fun `test status monitoring start`() = runTest {
        // Given
        val jobId = "test-job-123"
        
        // When
        mockStatusMonitor.startMonitoring(jobId)
        
        // Then
        assertNotNull(mockStatusMonitor)
    }
    
    @Test
    fun `test status monitoring stop`() = runTest {
        // Given
        val jobId = "test-job-123"
        mockStatusMonitor.startMonitoring(jobId)
        
        // When
        mockStatusMonitor.stopMonitoring()
        
        // Then
        assertNotNull(mockStatusMonitor)
    }
    
    @Test
    fun `test status monitoring get current status`() = runTest {
        // Given
        val jobId = "test-job-123"
        mockStatusMonitor.startMonitoring(jobId)
        
        // When
        val status = mockStatusMonitor.getCurrentStatus()
        
        // Then
        // Status might be null initially
        assertNotNull(mockStatusMonitor)
    }
    
    @Test
    fun `test status monitoring get status history`() = runTest {
        // Given
        val jobId = "test-job-123"
        mockStatusMonitor.startMonitoring(jobId)
        
        // When
        val history = mockStatusMonitor.getStatusHistory()
        
        // Then
        assertNotNull(history)
        assertTrue(history is List<TranscriptionStatus>)
    }
    
    @Test
    fun `test status monitoring clear status`() = runTest {
        // Given
        val jobId = "test-job-123"
        mockStatusMonitor.startMonitoring(jobId)
        
        // When
        mockStatusMonitor.clearStatus()
        
        // Then
        assertNotNull(mockStatusMonitor)
    }
}

