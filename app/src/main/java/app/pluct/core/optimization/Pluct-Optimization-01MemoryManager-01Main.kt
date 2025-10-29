package app.pluct.core.optimization

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.lang.ref.WeakReference
import app.pluct.architecture.PluctComponent

/**
 * Pluct-Optimization-01MemoryManager - Main memory manager orchestrator
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Single source of truth for memory management functionality
 */
class PluctMemoryManager(
    private val context: Context
) : PluctComponent {
    override val componentId = "PluctMemoryManager"
    override val dependencies = emptyList<String>()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _memoryStatus = MutableStateFlow(MemoryStatus())
    val memoryStatus: StateFlow<MemoryStatus> = _memoryStatus.asStateFlow()

    private val weakReferences = ConcurrentHashMap<String, WeakReference<Any>>()
    private val memoryLeakDetector = PluctMemoryLeakDetector()
    private val garbageCollector = PluctGarbageCollector()
    private val memoryMonitor = PluctMemoryMonitor(context)

    init {
        startMemoryMonitoring()
    }

    override fun initialize() {
        Log.i("PluctMemoryManager", "PluctMemoryManager initialized")
    }

    override fun cleanup() {
        scope.cancel()
        weakReferences.clear()
        memoryLeakDetector.cleanup()
    }

    /**
     * Start continuous memory monitoring
     */
    private fun startMemoryMonitoring() {
        scope.launch {
            while (isActive) {
                try {
                    val currentStatus = memoryMonitor.getCurrentMemoryStatus()
                    _memoryStatus.value = currentStatus
                    
                    // Check for memory leaks
                    memoryLeakDetector.checkForLeaks(weakReferences)
                    
                    // Trigger garbage collection if needed
                    if (currentStatus.usedMemoryMB > currentStatus.maxMemoryMB * 0.8) {
                        garbageCollector.forceGarbageCollection()
                    }
                    
                    delay(5000) // Check every 5 seconds
                } catch (e: Exception) {
                    Log.e("PluctMemoryManager", "Error in memory monitoring", e)
                }
            }
        }
    }

    /**
     * Register an object for memory tracking
     */
    fun registerObject(key: String, obj: Any) {
        weakReferences[key] = WeakReference(obj)
    }

    /**
     * Unregister an object from memory tracking
     */
    fun unregisterObject(key: String) {
        weakReferences.remove(key)
    }

    /**
     * Get current memory status
     */
    fun getCurrentMemoryStatus(): MemoryStatus {
        return memoryMonitor.getCurrentMemoryStatus()
    }

    /**
     * Force garbage collection
     */
    fun forceGarbageCollection() {
        garbageCollector.forceGarbageCollection()
    }
}

/**
 * Memory status data class
 */
data class MemoryStatus(
    val usedMemoryMB: Long = 0,
    val maxMemoryMB: Long = 0,
    val availableMemoryMB: Long = 0,
    val memoryPressure: MemoryPressure = MemoryPressure.NORMAL,
    val leakCount: Int = 0
)

enum class MemoryPressure {
    LOW, NORMAL, HIGH, CRITICAL
}
