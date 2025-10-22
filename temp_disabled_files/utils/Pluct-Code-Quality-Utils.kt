package app.pluct.utils

import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Code quality and maintainability utilities
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Singleton
class PluctCodeQualityUtils @Inject constructor() {
    
    private val operationCounter = AtomicInteger(0)
    private val errorCounter = AtomicInteger(0)
    
    /**
     * Execute operation with quality monitoring
     */
    suspend fun <T> executeWithQualityMonitoring(
        operation: suspend () -> T,
        operationName: String = "Operation"
    ): T {
        val operationId = operationCounter.incrementAndGet()
        val startTime = System.currentTimeMillis()
        
        Log.i("CodeQuality", "🎯 Starting operation $operationId: $operationName")
        
        try {
            val result = operation()
            val duration = System.currentTimeMillis() - startTime
            
            Log.i("CodeQuality", "✅ Operation $operationId completed: $operationName (${duration}ms)")
            return result
            
        } catch (e: Exception) {
            errorCounter.incrementAndGet()
            val duration = System.currentTimeMillis() - startTime
            
            Log.e("CodeQuality", "❌ Operation $operationId failed: $operationName (${duration}ms)", e)
            throw e
        }
    }
    
    /**
     * Validate input parameters
     */
    fun validateInput(
        value: Any?,
        parameterName: String,
        validationRules: List<(Any?) -> Boolean> = emptyList()
    ): Boolean {
        if (value == null) {
            Log.w("CodeQuality", "⚠️ Validation failed: $parameterName is null")
            return false
        }
        
        for (rule in validationRules) {
            if (!rule(value)) {
                Log.w("CodeQuality", "⚠️ Validation failed: $parameterName failed custom rule")
                return false
            }
        }
        
        Log.d("CodeQuality", "✅ Validation passed: $parameterName")
        return true
    }
    
    /**
     * Validate string input
     */
    fun validateString(
        value: String?,
        parameterName: String,
        minLength: Int = 0,
        maxLength: Int = Int.MAX_VALUE,
        allowEmpty: Boolean = true
    ): Boolean {
        return validateInput(
            value = value,
            parameterName = parameterName,
            validationRules = listOf(
                { it is String },
                { (it as? String)?.length ?: 0 >= minLength },
                { (it as? String)?.length ?: 0 <= maxLength },
                { if (!allowEmpty) !(it as? String).isNullOrBlank() else true }
            )
        )
    }
    
    /**
     * Validate numeric input
     */
    fun validateNumeric(
        value: Number?,
        parameterName: String,
        minValue: Double = Double.MIN_VALUE,
        maxValue: Double = Double.MAX_VALUE
    ): Boolean {
        return validateInput(
            value = value,
            parameterName = parameterName,
            validationRules = listOf(
                { it is Number },
                { (it as? Number)?.toDouble() ?: 0.0 >= minValue },
                { (it as? Number)?.toDouble() ?: 0.0 <= maxValue }
            )
        )
    }
    
    /**
     * Get operation statistics
     */
    fun getOperationStats(): OperationStats {
        return OperationStats(
            totalOperations = operationCounter.get(),
            totalErrors = errorCounter.get(),
            successRate = if (operationCounter.get() > 0) {
                1.0 - (errorCounter.get().toDouble() / operationCounter.get().toDouble())
            } else 1.0
        )
    }
    
    /**
     * Reset statistics
     */
    fun resetStats() {
        operationCounter.set(0)
        errorCounter.set(0)
        Log.i("CodeQuality", "🔄 Statistics reset")
    }
    
    /**
     * Check code complexity
     */
    fun analyzeCodeComplexity(
        functionName: String,
        lineCount: Int,
        cyclomaticComplexity: Int
    ): ComplexityAnalysis {
        val complexityLevel = when {
            cyclomaticComplexity <= 5 -> "LOW"
            cyclomaticComplexity <= 10 -> "MEDIUM"
            cyclomaticComplexity <= 20 -> "HIGH"
            else -> "VERY_HIGH"
        }
        
        val recommendations = mutableListOf<String>()
        
        if (cyclomaticComplexity > 10) {
            recommendations.add("Consider breaking down this function into smaller functions")
        }
        
        if (lineCount > 50) {
            recommendations.add("Consider reducing function length")
        }
        
        if (cyclomaticComplexity > 20) {
            recommendations.add("This function is too complex and should be refactored")
        }
        
        Log.i("CodeQuality", "📊 Complexity analysis for $functionName: $complexityLevel")
        
        return ComplexityAnalysis(
            functionName = functionName,
            lineCount = lineCount,
            cyclomaticComplexity = cyclomaticComplexity,
            complexityLevel = complexityLevel,
            recommendations = recommendations
        )
    }
    
    /**
     * Monitor memory usage
     */
    fun monitorMemoryUsage(): MemoryUsage {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        
        val usagePercentage = (usedMemory.toDouble() / maxMemory.toDouble()) * 100
        
        Log.d("CodeQuality", "💾 Memory usage: ${usedMemory / 1024 / 1024}MB / ${maxMemory / 1024 / 1024}MB (${usagePercentage.toInt()}%)")
        
        return MemoryUsage(
            totalMemory = totalMemory,
            freeMemory = freeMemory,
            usedMemory = usedMemory,
            maxMemory = maxMemory,
            usagePercentage = usagePercentage
        )
    }
    
    /**
     * Check for potential memory leaks
     */
    fun checkForMemoryLeaks(): MemoryLeakAnalysis {
        val runtime = Runtime.getRuntime()
        val currentMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // This is a simplified check - in a real implementation, you'd use more sophisticated tools
        val potentialLeak = currentMemory > (runtime.maxMemory() * 0.8)
        
        if (potentialLeak) {
            Log.w("CodeQuality", "⚠️ Potential memory leak detected")
        }
        
        return MemoryLeakAnalysis(
            currentMemory = currentMemory,
            maxMemory = runtime.maxMemory(),
            potentialLeak = potentialLeak,
            recommendations = if (potentialLeak) {
                listOf("Consider checking for memory leaks", "Review object lifecycle management")
            } else {
                emptyList()
            }
        )
    }
    
    data class OperationStats(
        val totalOperations: Int,
        val totalErrors: Int,
        val successRate: Double
    )
    
    data class ComplexityAnalysis(
        val functionName: String,
        val lineCount: Int,
        val cyclomaticComplexity: Int,
        val complexityLevel: String,
        val recommendations: List<String>
    )
    
    data class MemoryUsage(
        val totalMemory: Long,
        val freeMemory: Long,
        val usedMemory: Long,
        val maxMemory: Long,
        val usagePercentage: Double
    )
    
    data class MemoryLeakAnalysis(
        val currentMemory: Long,
        val maxMemory: Long,
        val potentialLeak: Boolean,
        val recommendations: List<String>
    )
}
