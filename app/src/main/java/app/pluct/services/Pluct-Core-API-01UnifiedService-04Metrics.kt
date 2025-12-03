package app.pluct.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pluct-Core-API-01UnifiedService-04Metrics - Metrics tracking
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[SubScope]-[Separation]-[Responsibility]
 * Single source of truth for API metrics
 */
class PluctCoreAPIUnifiedServiceMetrics {
    
    private val _apiMetrics = MutableStateFlow(APIMetrics())
    val apiMetrics: StateFlow<APIMetrics> = _apiMetrics.asStateFlow()
    
    /**
     * Update metrics with request result
     */
    fun updateMetrics(success: Boolean, attempt: Int) {
        val currentMetrics = _apiMetrics.value
        val newMetrics = currentMetrics.copy(
            totalRequests = currentMetrics.totalRequests + 1,
            successfulRequests = if (success) currentMetrics.successfulRequests + 1 else currentMetrics.successfulRequests,
            failedRequests = if (!success) currentMetrics.failedRequests + 1 else currentMetrics.failedRequests,
            averageRetries = (currentMetrics.averageRetries * (currentMetrics.totalRequests - 1) + attempt) / currentMetrics.totalRequests
        )
        _apiMetrics.value = newMetrics
    }
}

