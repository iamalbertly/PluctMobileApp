package app.pluct.core.optimization

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis
import app.pluct.architecture.PluctComponent

/**
 * Pluct-Optimization-01MemoryManager - Optimize memory usage and prevent memory leaks
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Implements comprehensive memory management with leak detection and optimization
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
    private val memoryLeakDetector = MemoryLeakDetector()
    private val garbageCollector = GarbageCollector()

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
                monitorMemoryUsage()
                delay(5000) // Check every 5 seconds
            }
        }
    }

    /**
     * Monitor current memory usage
     */
    private suspend fun monitorMemoryUsage() {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val availableMemory = memoryInfo.availMem

        val memoryStatus = MemoryStatus(
            usedMemory = usedMemory,
            maxMemory = maxMemory,
            availableMemory = availableMemory,
            memoryPressure = calculateMemoryPressure(usedMemory, maxMemory),
            timestamp = System.currentTimeMillis()
        )

        _memoryStatus.value = memoryStatus

        // Trigger garbage collection if memory pressure is high
        if (memoryStatus.memoryPressure > 0.8) {
            Log.w("PluctMemoryManager", "High memory pressure detected: ${memoryStatus.memoryPressure}")
            garbageCollector.forceGarbageCollection()
        }
    }

    /**
     * Calculate memory pressure (0.0 to 1.0)
     */
    private fun calculateMemoryPressure(usedMemory: Long, maxMemory: Long): Double {
        return usedMemory.toDouble() / maxMemory.toDouble()
    }

    /**
     * Register object for leak detection
     */
    fun registerForLeakDetection(key: String, obj: Any) {
        weakReferences[key] = WeakReference(obj)
        memoryLeakDetector.registerObject(key, obj)
    }

    /**
     * Unregister object from leak detection
     */
    fun unregisterFromLeakDetection(key: String) {
        weakReferences.remove(key)
        memoryLeakDetector.unregisterObject(key)
    }

    /**
     * Check for memory leaks
     */
    suspend fun checkForLeaks(): List<MemoryLeak> {
        return memoryLeakDetector.detectLeaks()
    }

    /**
     * Optimize memory usage
     */
    suspend fun optimizeMemory(): OptimizationResult {
        val startTime = System.currentTimeMillis()
        var freedMemory = 0L

        // Clear weak references to unreachable objects
        val iterator = weakReferences.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.get() == null) {
                iterator.remove()
                freedMemory += estimateObjectSize(entry.key)
            }
        }

        // Force garbage collection
        val gcTime = measureTimeMillis {
            garbageCollector.forceGarbageCollection()
        }

        val endTime = System.currentTimeMillis()
        
        return OptimizationResult(
            freedMemory = freedMemory,
            optimizationTime = endTime - startTime,
            garbageCollectionTime = gcTime,
            success = true
        )
    }

    /**
     * Estimate object size (simplified)
     */
    private fun estimateObjectSize(key: String): Long {
        return key.length * 2L + 64L // Rough estimate
    }

    /**
     * Get memory recommendations
     */
    fun getMemoryRecommendations(): List<MemoryRecommendation> {
        val currentStatus = _memoryStatus.value
        val recommendations = mutableListOf<MemoryRecommendation>()

        if (currentStatus.memoryPressure > 0.8) {
            recommendations.add(
                MemoryRecommendation(
                    type = RecommendationType.CRITICAL,
                    message = "Memory usage is critically high. Consider reducing cache size or clearing unused data.",
                    action = "Clear caches and unused objects"
                )
            )
        } else if (currentStatus.memoryPressure > 0.6) {
            recommendations.add(
                MemoryRecommendation(
                    type = RecommendationType.WARNING,
                    message = "Memory usage is high. Monitor for potential memory leaks.",
                    action = "Review object lifecycle and consider optimization"
                )
            )
        }

        if (weakReferences.size > 1000) {
            recommendations.add(
                MemoryRecommendation(
                    type = RecommendationType.INFO,
                    message = "Large number of weak references detected. Consider cleanup.",
                    action = "Review and clean up unused weak references"
                )
            )
        }

        return recommendations
    }

}

/**
 * Memory status data class
 */
data class MemoryStatus(
    val usedMemory: Long = 0,
    val maxMemory: Long = 0,
    val availableMemory: Long = 0,
    val memoryPressure: Double = 0.0,
    val timestamp: Long = 0
)

/**
 * Memory leak data class
 */
data class MemoryLeak(
    val objectKey: String,
    val objectType: String,
    val leakTime: Long,
    val severity: LeakSeverity
)

/**
 * Leak severity levels
 */
enum class LeakSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Memory recommendation data class
 */
data class MemoryRecommendation(
    val type: RecommendationType,
    val message: String,
    val action: String
)

/**
 * Recommendation types
 */
enum class RecommendationType {
    INFO, WARNING, CRITICAL
}

/**
 * Optimization result data class
 */
data class OptimizationResult(
    val freedMemory: Long,
    val optimizationTime: Long,
    val garbageCollectionTime: Long,
    val success: Boolean
)

/**
 * Memory leak detector
 */
class MemoryLeakDetector {
    private val registeredObjects = ConcurrentHashMap<String, Long>()
    private val objectTypes = ConcurrentHashMap<String, String>()

    fun registerObject(key: String, obj: Any) {
        registeredObjects[key] = System.currentTimeMillis()
        objectTypes[key] = obj::class.java.simpleName
    }

    fun unregisterObject(key: String) {
        registeredObjects.remove(key)
        objectTypes.remove(key)
    }

    suspend fun detectLeaks(): List<MemoryLeak> {
        val leaks = mutableListOf<MemoryLeak>()
        val currentTime = System.currentTimeMillis()
        
        for ((key, registrationTime) in registeredObjects) {
            val age = currentTime - registrationTime
            if (age > 300000) { // 5 minutes
                val severity = when {
                    age > 1800000 -> LeakSeverity.CRITICAL // 30 minutes
                    age > 900000 -> LeakSeverity.HIGH // 15 minutes
                    age > 600000 -> LeakSeverity.MEDIUM // 10 minutes
                    else -> LeakSeverity.LOW
                }
                
                leaks.add(
                    MemoryLeak(
                        objectKey = key,
                        objectType = objectTypes[key] ?: "Unknown",
                        leakTime = age,
                        severity = severity
                    )
                )
            }
        }
        
        return leaks
    }

    fun cleanup() {
        registeredObjects.clear()
        objectTypes.clear()
    }
}

/**
 * Garbage collector helper
 */
class GarbageCollector {
    fun forceGarbageCollection() {
        System.gc()
        System.runFinalization()
        System.gc()
    }
}
