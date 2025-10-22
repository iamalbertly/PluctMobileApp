package app.pluct.services

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Performance-Monitor - Performance monitoring and metrics collection
 * Single source of truth for performance monitoring
 * Adheres to 300-line limit with smart separation of concerns
 */

data class PerformanceMetric(
    val operation: String,
    val duration: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean,
    val error: String? = null
)

data class PerformanceSummary(
    val totalOperations: Int,
    val successfulOperations: Int,
    val failedOperations: Int,
    val averageDuration: Long,
    val maxDuration: Long,
    val minDuration: Long
)

@Singleton
class PluctPerformanceMonitor @Inject constructor() {
    private val _metrics = MutableStateFlow<List<PerformanceMetric>>(emptyList())
    val metrics: StateFlow<List<PerformanceMetric>> = _metrics.asStateFlow()
    
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    fun startMonitoring() {
        _isMonitoring.value = true
        Log.d("PluctPerformanceMonitor", "📊 Performance monitoring started")
    }
    
    fun stopMonitoring() {
        _isMonitoring.value = false
        Log.d("PluctPerformanceMonitor", "🛑 Performance monitoring stopped")
    }
    
    fun recordMetric(operation: String, duration: Long, success: Boolean, error: String? = null) {
        val metric = PerformanceMetric(
            operation = operation,
            duration = duration,
            success = success,
            error = error
        )
        
        _metrics.value = _metrics.value + metric
        Log.d("PluctPerformanceMonitor", "📈 Recorded metric: $operation - ${duration}ms - ${if (success) "SUCCESS" else "FAILED"}")
    }
    
    fun getPerformanceSummary(): PerformanceSummary {
        val metrics = _metrics.value
        if (metrics.isEmpty()) {
            return PerformanceSummary(0, 0, 0, 0, 0, 0)
        }
        
        val successful = metrics.count { it.success }
        val failed = metrics.count { !it.success }
        val durations = metrics.map { it.duration }
        
        return PerformanceSummary(
            totalOperations = metrics.size,
            successfulOperations = successful,
            failedOperations = failed,
            averageDuration = durations.average().toLong(),
            maxDuration = durations.maxOrNull() ?: 0,
            minDuration = durations.minOrNull() ?: 0
        )
    }
    
    fun getMetricsByOperation(operation: String): List<PerformanceMetric> {
        return _metrics.value.filter { it.operation == operation }
    }
    
    fun getSlowOperations(thresholdMs: Long = 5000): List<PerformanceMetric> {
        return _metrics.value.filter { it.duration > thresholdMs }
    }
    
    fun getFailedOperations(): List<PerformanceMetric> {
        return _metrics.value.filter { !it.success }
    }
    
    fun clearMetrics() {
        _metrics.value = emptyList()
    }
}

