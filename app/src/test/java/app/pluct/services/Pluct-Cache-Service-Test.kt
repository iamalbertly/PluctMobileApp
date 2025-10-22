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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pluct-Cache-Service-Test - Test coverage for cache service
 * Single source of truth for cache service tests
 * Adheres to 300-line limit with smart separation of concerns
 */

@RunWith(MockitoJUnitRunner::class)
class PluctCacheServiceTest {
    
    @Mock
    private lateinit var mockCacheService: PluctCacheService
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }
    
    @Test
    fun `test cache service put and get`() = runTest {
        // Given
        val key = "test-key"
        val value = "test-value"
        
        // When
        mockCacheService.put(key, value)
        val retrievedValue = mockCacheService.get<String>(key)
        
        // Then
        assertNotNull(retrievedValue)
        assertEquals(value, retrievedValue)
    }
    
    @Test
    fun `test cache service put with ttl`() = runTest {
        // Given
        val key = "test-key"
        val value = "test-value"
        val ttl = 1000L // 1 second
        
        // When
        mockCacheService.put(key, value, ttl)
        val retrievedValue = mockCacheService.get<String>(key)
        
        // Then
        assertNotNull(retrievedValue)
        assertEquals(value, retrievedValue)
    }
    
    @Test
    fun `test cache service get non-existent key`() = runTest {
        // Given
        val key = "non-existent-key"
        
        // When
        val retrievedValue = mockCacheService.get<String>(key)
        
        // Then
        assertNull(retrievedValue)
    }
    
    @Test
    fun `test cache service remove`() = runTest {
        // Given
        val key = "test-key"
        val value = "test-value"
        mockCacheService.put(key, value)
        
        // When
        mockCacheService.remove(key)
        val retrievedValue = mockCacheService.get<String>(key)
        
        // Then
        assertNull(retrievedValue)
    }
    
    @Test
    fun `test cache service clear`() = runTest {
        // Given
        val key1 = "test-key-1"
        val key2 = "test-key-2"
        val value1 = "test-value-1"
        val value2 = "test-value-2"
        mockCacheService.put(key1, value1)
        mockCacheService.put(key2, value2)
        
        // When
        mockCacheService.clear()
        val retrievedValue1 = mockCacheService.get<String>(key1)
        val retrievedValue2 = mockCacheService.get<String>(key2)
        
        // Then
        assertNull(retrievedValue1)
        assertNull(retrievedValue2)
    }
    
    @Test
    fun `test cache service get stats`() = runTest {
        // Given
        val key = "test-key"
        val value = "test-value"
        mockCacheService.put(key, value)
        
        // When
        val stats = mockCacheService.getStats()
        
        // Then
        assertNotNull(stats)
        assertTrue(stats.totalEntries >= 0)
        assertTrue(stats.hitCount >= 0)
        assertTrue(stats.missCount >= 0)
        assertTrue(stats.hitRate >= 0.0)
        assertTrue(stats.memoryUsage >= 0)
    }
    
    @Test
    fun `test cache service cleanup expired`() = runTest {
        // Given
        val key = "test-key"
        val value = "test-value"
        val ttl = 1L // 1 millisecond
        mockCacheService.put(key, value, ttl)
        
        // Wait for expiration
        Thread.sleep(10)
        
        // When
        mockCacheService.cleanupExpired()
        val retrievedValue = mockCacheService.get<String>(key)
        
        // Then
        assertNull(retrievedValue)
    }
}

