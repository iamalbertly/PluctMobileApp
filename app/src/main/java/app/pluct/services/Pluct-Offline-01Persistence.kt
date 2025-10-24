package app.pluct.services

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import java.io.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Pluct-Offline-01Persistence - Offline mode and data persistence
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@Singleton
class PluctOfflinePersistence @Inject constructor(
    private val context: Context
) {
    
    private val _offlineMode = MutableStateFlow(false)
    val offlineMode: StateFlow<Boolean> = _offlineMode.asStateFlow()
    
    private val _pendingOperations = MutableStateFlow<List<PendingOperation>>(emptyList())
    val pendingOperations: StateFlow<List<PendingOperation>> = _pendingOperations.asStateFlow()
    
    private val _cachedData = MutableStateFlow<Map<String, CachedData>>(emptyMap())
    val cachedData: StateFlow<Map<String, CachedData>> = _cachedData.asStateFlow()
    
    private val persistenceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    data class PendingOperation(
        val id: String,
        val type: OperationType,
        val data: Map<String, Any>,
        val timestamp: Long = System.currentTimeMillis(),
        val retryCount: Int = 0,
        val maxRetries: Int = 3
    )
    
    data class CachedData(
        val key: String,
        val data: Any,
        val timestamp: Long = System.currentTimeMillis(),
        val expiryTime: Long? = null
    )
    
    enum class OperationType {
        TRANSCRIPTION_REQUEST,
        METADATA_FETCH,
        CREDIT_BALANCE_UPDATE,
        VIDEO_DELETE,
        VIDEO_RETRY
    }
    
    /**
     * Set offline mode
     */
    fun setOfflineMode(offline: Boolean) {
        _offlineMode.value = offline
        Log.d("PluctOfflinePersistence", "Offline mode: $offline")
        
        if (!offline) {
            // When coming back online, process pending operations
            persistenceScope.launch {
                processPendingOperations()
            }
        }
    }
    
    /**
     * Check if we're offline
     */
    fun isOffline(): Boolean {
        return _offlineMode.value
    }
    
    /**
     * Add a pending operation
     */
    fun addPendingOperation(
        type: OperationType,
        data: Map<String, Any>
    ): String {
        val operationId = generateOperationId()
        val operation = PendingOperation(
            id = operationId,
            type = type,
            data = data
        )
        
        val currentOperations = _pendingOperations.value.toMutableList()
        currentOperations.add(operation)
        _pendingOperations.value = currentOperations
        
        Log.d("PluctOfflinePersistence", "Added pending operation: $type")
        return operationId
    }
    
    /**
     * Cache data for offline use
     */
    fun cacheData(key: String, data: Any, expiryTime: Long? = null) {
        val cachedData = CachedData(
            key = key,
            data = data,
            expiryTime = expiryTime
        )
        
        val currentCache = _cachedData.value.toMutableMap()
        currentCache[key] = cachedData
        _cachedData.value = currentCache
        
        Log.d("PluctOfflinePersistence", "Cached data for key: $key")
    }
    
    /**
     * Get cached data
     */
    fun getCachedData(key: String): Any? {
        val cached = _cachedData.value[key]
        if (cached != null) {
            // Check if data has expired
            if (cached.expiryTime == null || System.currentTimeMillis() < cached.expiryTime) {
                return cached.data
            } else {
                // Remove expired data
                val currentCache = _cachedData.value.toMutableMap()
                currentCache.remove(key)
                _cachedData.value = currentCache
            }
        }
        return null
    }
    
    /**
     * Process pending operations when back online
     */
    private suspend fun processPendingOperations() {
        val operations = _pendingOperations.value.toMutableList()
        val processedOperations = mutableListOf<String>()
        
        for (operation in operations) {
            try {
                val success = executeOperation(operation)
                if (success) {
                    processedOperations.add(operation.id)
                    Log.d("PluctOfflinePersistence", "Processed operation: ${operation.id}")
                } else {
                    // Increment retry count
                    val updatedOperation = operation.copy(retryCount = operation.retryCount + 1)
                    val index = operations.indexOf(operation)
                    if (index != -1) {
                        operations[index] = updatedOperation
                    }
                    
                    if (updatedOperation.retryCount >= updatedOperation.maxRetries) {
                        processedOperations.add(operation.id)
                        Log.w("PluctOfflinePersistence", "Max retries reached for operation: ${operation.id}")
                    }
                }
            } catch (e: Exception) {
                Log.e("PluctOfflinePersistence", "Failed to process operation: ${operation.id}", e)
            }
        }
        
        // Remove processed operations
        operations.removeAll { it.id in processedOperations }
        _pendingOperations.value = operations
    }
    
    /**
     * Execute a pending operation
     */
    private suspend fun executeOperation(operation: PendingOperation): Boolean {
        return when (operation.type) {
            OperationType.TRANSCRIPTION_REQUEST -> {
                // Execute transcription request
                executeTranscriptionRequest(operation.data)
            }
            OperationType.METADATA_FETCH -> {
                // Execute metadata fetch
                executeMetadataFetch(operation.data)
            }
            OperationType.CREDIT_BALANCE_UPDATE -> {
                // Execute credit balance update
                executeCreditBalanceUpdate(operation.data)
            }
            OperationType.VIDEO_DELETE -> {
                // Execute video delete
                executeVideoDelete(operation.data)
            }
            OperationType.VIDEO_RETRY -> {
                // Execute video retry
                executeVideoRetry(operation.data)
            }
        }
    }
    
    /**
     * Execute transcription request
     */
    private suspend fun executeTranscriptionRequest(data: Map<String, Any>): Boolean {
        return try {
            // Simulate API call
            delay(1000)
            Log.d("PluctOfflinePersistence", "Executed transcription request")
            true
        } catch (e: Exception) {
            Log.e("PluctOfflinePersistence", "Failed to execute transcription request", e)
            false
        }
    }
    
    /**
     * Execute metadata fetch
     */
    private suspend fun executeMetadataFetch(data: Map<String, Any>): Boolean {
        return try {
            // Simulate API call
            delay(500)
            Log.d("PluctOfflinePersistence", "Executed metadata fetch")
            true
        } catch (e: Exception) {
            Log.e("PluctOfflinePersistence", "Failed to execute metadata fetch", e)
            false
        }
    }
    
    /**
     * Execute credit balance update
     */
    private suspend fun executeCreditBalanceUpdate(data: Map<String, Any>): Boolean {
        return try {
            // Simulate API call
            delay(300)
            Log.d("PluctOfflinePersistence", "Executed credit balance update")
            true
        } catch (e: Exception) {
            Log.e("PluctOfflinePersistence", "Failed to execute credit balance update", e)
            false
        }
    }
    
    /**
     * Execute video delete
     */
    private suspend fun executeVideoDelete(data: Map<String, Any>): Boolean {
        return try {
            // Simulate API call
            delay(200)
            Log.d("PluctOfflinePersistence", "Executed video delete")
            true
        } catch (e: Exception) {
            Log.e("PluctOfflinePersistence", "Failed to execute video delete", e)
            false
        }
    }
    
    /**
     * Execute video retry
     */
    private suspend fun executeVideoRetry(data: Map<String, Any>): Boolean {
        return try {
            // Simulate API call
            delay(1000)
            Log.d("PluctOfflinePersistence", "Executed video retry")
            true
        } catch (e: Exception) {
            Log.e("PluctOfflinePersistence", "Failed to execute video retry", e)
            false
        }
    }
    
    /**
     * Save data to local storage
     */
    fun saveToLocalStorage(filename: String, data: String) {
        try {
            val file = File(context.filesDir, filename)
            file.writeText(data)
            Log.d("PluctOfflinePersistence", "Saved data to local storage: $filename")
        } catch (e: Exception) {
            Log.e("PluctOfflinePersistence", "Failed to save data to local storage", e)
        }
    }
    
    /**
     * Load data from local storage
     */
    fun loadFromLocalStorage(filename: String): String? {
        return try {
            val file = File(context.filesDir, filename)
            if (file.exists()) {
                val data = file.readText()
                Log.d("PluctOfflinePersistence", "Loaded data from local storage: $filename")
                data
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("PluctOfflinePersistence", "Failed to load data from local storage", e)
            null
        }
    }
    
    /**
     * Clear expired cache
     */
    fun clearExpiredCache() {
        val currentTime = System.currentTimeMillis()
        val filteredCache = _cachedData.value.filter { (_, cached) ->
            cached.expiryTime == null || currentTime < cached.expiryTime
        }
        _cachedData.value = filteredCache
        
        Log.d("PluctOfflinePersistence", "Cleared expired cache")
    }
    
    private fun generateOperationId(): String {
        return "op_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}
