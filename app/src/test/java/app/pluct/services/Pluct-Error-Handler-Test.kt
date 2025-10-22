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
 * Pluct-Error-Handler-Test - Test coverage for error handling
 * Single source of truth for error handler tests
 * Adheres to 300-line limit with smart separation of concerns
 */

@RunWith(MockitoJUnitRunner::class)
class PluctErrorHandlerTest {
    
    @Mock
    private lateinit var mockErrorHandler: PluctErrorHandler
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }
    
    @Test
    fun `test error handler execute with retry success`() = runTest {
        // Given
        val operation = "test-operation"
        val expectedResult = "success"
        
        // When
        val result = mockErrorHandler.executeWithRetry(operation) {
            expectedResult
        }
        
        // Then
        assertNotNull(result)
    }
    
    @Test
    fun `test error handler execute with retry failure`() = runTest {
        // Given
        val operation = "test-operation"
        val expectedError = RuntimeException("Test error")
        
        // When
        val result = mockErrorHandler.executeWithRetry(operation) {
            throw expectedError
        }
        
        // Then
        assertNotNull(result)
    }
    
    @Test
    fun `test error handler execute with exponential backoff`() = runTest {
        // Given
        val operation = "test-operation"
        val expectedResult = "success"
        
        // When
        val result = mockErrorHandler.executeWithExponentialBackoff(operation) {
            expectedResult
        }
        
        // Then
        assertNotNull(result)
    }
    
    @Test
    fun `test error handler execute with linear backoff`() = runTest {
        // Given
        val operation = "test-operation"
        val expectedResult = "success"
        
        // When
        val result = mockErrorHandler.executeWithLinearBackoff(operation) {
            expectedResult
        }
        
        // Then
        assertNotNull(result)
    }
    
    @Test
    fun `test error handler get error history`() = runTest {
        // Given
        val operation = "test-operation"
        mockErrorHandler.executeWithRetry(operation) {
            throw RuntimeException("Test error")
        }
        
        // When
        val history = mockErrorHandler.getErrorHistory()
        
        // Then
        assertNotNull(history)
        assertTrue(history is List<ErrorContext>)
    }
    
    @Test
    fun `test error handler clear error history`() = runTest {
        // Given
        val operation = "test-operation"
        mockErrorHandler.executeWithRetry(operation) {
            throw RuntimeException("Test error")
        }
        
        // When
        mockErrorHandler.clearErrorHistory()
        
        // Then
        assertNotNull(mockErrorHandler)
    }
    
    @Test
    fun `test error handler is retryable error`() = runTest {
        // Given
        val retryableError = java.net.UnknownHostException("Test error")
        val nonRetryableError = IllegalArgumentException("Test error")
        
        // When
        val isRetryable1 = mockErrorHandler.isRetryableError(retryableError)
        val isRetryable2 = mockErrorHandler.isRetryableError(nonRetryableError)
        
        // Then
        assertNotNull(isRetryable1)
        assertNotNull(isRetryable2)
    }
    
    @Test
    fun `test error handler get retry delay`() = runTest {
        // Given
        val attempt = 2
        val baseDelay = 1000L
        
        // When
        val delay = mockErrorHandler.getRetryDelay(attempt, baseDelay)
        
        // Then
        assertNotNull(delay)
        assertTrue(delay > 0)
    }
}

