package app.pluct.monitoring

import app.pluct.core.log.PluctLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Monitoring-Performance-01Optimizer - Performance monitoring and optimization system
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Monitors app performance metrics and provides optimization recommendations
 */
@Singleton
class PluctPerformanceOptimizer @Inject constructor() {
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    private val _optimizationRecommendations = MutableStateFlow<List<OptimizationRecommendation>>(emptyList())
    val optimizationRecommendations: StateFlow<List<OptimizationRecommendation>> = _optimizationRecommendations.asStateFlow()
    
    private var isMonitoring = false
    private val performanceHistory = mutableListOf<PerformanceSnapshot>()
    
    /**
     * Start performance monitoring
     */
    fun startMonitoring() {
        if (isMonitoring) {
            PluctLogger.logWarning("Performance monitoring already active")
            return
        }
        
        isMonitoring = true
        PluctLogger.logInfo("Starting performance monitoring")
        
        // Initialize baseline metrics
        updatePerformanceMetrics()
    }
    
    /**
     * Stop performance monitoring
     */
    fun stopMonitoring() {
        isMonitoring = false
        PluctLogger.logInfo("Stopped performance monitoring")
    }
    
    /**
     * Record a performance measurement
     */
    fun recordMeasurement(
        operation: String,
        durationMs: Long,
        memoryUsageMb: Int = 0,
        cpuUsage: Float = 0f,
        details: Map<String, Any> = emptyMap()
    ) {
        val snapshot = PerformanceSnapshot(
            operation = operation,
            durationMs = durationMs,
            memoryUsageMb = memoryUsageMb,
            cpuUsage = cpuUsage,
            timestamp = System.currentTimeMillis(),
            details = details
        )
        
        performanceHistory.add(snapshot)
        
        // Keep only last 100 measurements
        if (performanceHistory.size > 100) {
            performanceHistory.removeAt(0)
        }
        
        PluctLogger.logPerformance(operation, durationMs, details)
        
        // Update current metrics
        updatePerformanceMetrics()
        
        // Check for optimization opportunities
        checkOptimizationOpportunities(snapshot)
    }
    
    /**
     * Get performance report
     */
    fun getPerformanceReport(): PerformanceReport {
        val currentMetrics = _performanceMetrics.value
        val recommendations = _optimizationRecommendations.value
        
        return PerformanceReport(
            currentMetrics = currentMetrics,
            recommendations = recommendations,
            history = performanceHistory.takeLast(20), // Last 20 measurements
            overallScore = calculateOverallScore(currentMetrics)
        )
    }
    
    private fun updatePerformanceMetrics() {
        if (performanceHistory.isEmpty()) return
        
        val recentSnapshots = performanceHistory.takeLast(10)
        
        val avgDuration = recentSnapshots.map { it.durationMs }.average()
        val avgMemoryUsage = recentSnapshots.map { it.memoryUsageMb }.average()
        val avgCpuUsage = recentSnapshots.map { it.cpuUsage }.average()
        
        val slowestOperation = recentSnapshots.maxByOrNull { it.durationMs }
        val memoryIntensiveOperation = recentSnapshots.maxByOrNull { it.memoryUsageMb }
        
        _performanceMetrics.value = PerformanceMetrics(
            averageResponseTime = avgDuration.toLong(),
            averageMemoryUsage = avgMemoryUsage.toInt(),
            averageCpuUsage = avgCpuUsage.toFloat(),
            slowestOperation = slowestOperation?.operation ?: "none",
            memoryIntensiveOperation = memoryIntensiveOperation?.operation ?: "none",
            totalMeasurements = performanceHistory.size
        )
    }
    
    private fun checkOptimizationOpportunities(snapshot: PerformanceSnapshot) {
        val recommendations = mutableListOf<OptimizationRecommendation>()
        
        // Check for slow operations
        if (snapshot.durationMs > 5000) { // 5 seconds
            recommendations.add(
                OptimizationRecommendation(
                    type = "PERFORMANCE",
                    priority = "HIGH",
                    title = "Slow Operation Detected",
                    description = "Operation '${snapshot.operation}' took ${snapshot.durationMs}ms",
                    suggestion = "Consider optimizing this operation or adding caching"
                )
            )
        }
        
        // Check for high memory usage
        if (snapshot.memoryUsageMb > 100) { // 100MB
            recommendations.add(
                OptimizationRecommendation(
                    type = "MEMORY",
                    priority = "MEDIUM",
                    title = "High Memory Usage",
                    description = "Operation '${snapshot.operation}' used ${snapshot.memoryUsageMb}MB",
                    suggestion = "Consider memory optimization or garbage collection tuning"
                )
            )
        }
        
        // Check for high CPU usage
        if (snapshot.cpuUsage > 0.8f) { // 80%
            recommendations.add(
                OptimizationRecommendation(
                    type = "CPU",
                    priority = "MEDIUM",
                    title = "High CPU Usage",
                    description = "Operation '${snapshot.operation}' used ${(snapshot.cpuUsage * 100).toInt()}% CPU",
                    suggestion = "Consider CPU optimization or background processing"
                )
            )
        }
        
        if (recommendations.isNotEmpty()) {
            _optimizationRecommendations.value = recommendations
        }
    }
    
    private fun calculateOverallScore(metrics: PerformanceMetrics): Int {
        var score = 100
        
        // Deduct points for slow operations
        if (metrics.averageResponseTime > 2000) score -= 20
        if (metrics.averageResponseTime > 5000) score -= 30
        
        // Deduct points for high memory usage
        if (metrics.averageMemoryUsage > 50) score -= 15
        if (metrics.averageMemoryUsage > 100) score -= 25
        
        // Deduct points for high CPU usage
        if (metrics.averageCpuUsage > 0.5f) score -= 10
        if (metrics.averageCpuUsage > 0.8f) score -= 20
        
        return maxOf(0, score)
    }
}

data class PerformanceMetrics(
    val averageResponseTime: Long = 0,
    val averageMemoryUsage: Int = 0,
    val averageCpuUsage: Float = 0f,
    val slowestOperation: String = "none",
    val memoryIntensiveOperation: String = "none",
    val totalMeasurements: Int = 0
)

data class PerformanceSnapshot(
    val operation: String,
    val durationMs: Long,
    val memoryUsageMb: Int,
    val cpuUsage: Float,
    val timestamp: Long,
    val details: Map<String, Any>
)

data class OptimizationRecommendation(
    val type: String,
    val priority: String,
    val title: String,
    val description: String,
    val suggestion: String
)

data class PerformanceReport(
    val currentMetrics: PerformanceMetrics,
    val recommendations: List<OptimizationRecommendation>,
    val history: List<PerformanceSnapshot>,
    val overallScore: Int
)
