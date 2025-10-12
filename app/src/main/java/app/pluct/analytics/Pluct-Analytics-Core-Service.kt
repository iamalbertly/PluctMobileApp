package app.pluct.analytics

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct Core Analytics Service - Single source of truth for analytics
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Singleton
class PluctAnalyticsCoreService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "PluctAnalyticsCore"
    }

    private val analyticsData = ConcurrentHashMap<String, AnalyticsMetric>()
    private val _dashboardData = MutableStateFlow(DashboardData())
    val dashboardData: Flow<DashboardData> = _dashboardData.asStateFlow()

    /**
     * Track video processing metrics
     */
    suspend fun trackVideoProcessing(
        videoId: String,
        processingTime: Long,
        success: Boolean,
        errorMessage: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Tracking video processing for: $videoId")
            
            val metric = analyticsData.getOrPut(videoId) {
                AnalyticsMetric(videoId, System.currentTimeMillis())
            }
            
            metric.processingMetrics.add(
                ProcessingMetric(
                    processingTime = processingTime,
                    success = success,
                    errorMessage = errorMessage,
                    timestamp = System.currentTimeMillis()
                )
            )
            
            updateDashboardData()
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking video processing: ${e.message}", e)
        }
    }

    /**
     * Track user engagement metrics
     */
    suspend fun trackUserEngagement(
        videoId: String,
        engagementType: EngagementType,
        value: Float
    ) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Tracking user engagement for: $videoId")
            
            val metric = analyticsData.getOrPut(videoId) {
                AnalyticsMetric(videoId, System.currentTimeMillis())
            }
            
            metric.engagementMetrics.add(
                EngagementMetric(
                    type = engagementType,
                    value = value,
                    timestamp = System.currentTimeMillis()
                )
            )
            
            updateDashboardData()
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking user engagement: ${e.message}", e)
        }
    }

    /**
     * Get analytics report
     */
    suspend fun getAnalyticsReport(): AnalyticsReport = withContext(Dispatchers.IO) {
        try {
            val totalVideos = analyticsData.size
            val totalProcessingTime = analyticsData.values.sumOf { 
                it.processingMetrics.sumOf { metric -> metric.processingTime }
            }
            val averageProcessingTime = if (totalVideos > 0) totalProcessingTime / totalVideos else 0L
            
            val successRate = calculateSuccessRate()
            val engagementRate = calculateEngagementRate()
            
            AnalyticsReport(
                totalVideosProcessed = totalVideos,
                averageProcessingTimeMillis = averageProcessingTime,
                successRate = successRate,
                engagementRate = engagementRate,
                generatedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating analytics report: ${e.message}", e)
            AnalyticsReport(
                totalVideosProcessed = 0,
                averageProcessingTimeMillis = 0L,
                successRate = 0f,
                engagementRate = 0f,
                generatedAt = System.currentTimeMillis(),
                errorMessage = e.message
            )
        }
    }

    private fun calculateSuccessRate(): Float {
        val totalProcessing = analyticsData.values.sumOf { it.processingMetrics.size }
        if (totalProcessing == 0) return 0f
        
        val successfulProcessing = analyticsData.values.sumOf { metric ->
            metric.processingMetrics.count { it.success }
        }
        
        return successfulProcessing.toFloat() / totalProcessing
    }

    private fun calculateEngagementRate(): Float {
        val totalEngagement = analyticsData.values.sumOf { metric ->
            metric.engagementMetrics.sumOf { it.value.toDouble() }
        }
        val totalVideos = analyticsData.size
        
        return if (totalVideos > 0) (totalEngagement / totalVideos).toFloat() else 0f
    }

    private fun updateDashboardData() {
        val report = kotlinx.coroutines.runBlocking { getAnalyticsReport() }
        _dashboardData.value = DashboardData(
            totalVideos = report.totalVideosProcessed,
            averageProcessingTime = report.averageProcessingTimeMillis,
            successRate = report.successRate,
            engagementRate = report.engagementRate,
            lastUpdated = System.currentTimeMillis()
        )
    }
}

/**
 * Analytics data classes
 */
data class AnalyticsMetric(
    val videoId: String,
    val createdAt: Long,
    val processingMetrics: MutableList<ProcessingMetric> = mutableListOf(),
    val engagementMetrics: MutableList<EngagementMetric> = mutableListOf()
)

data class ProcessingMetric(
    val processingTime: Long,
    val success: Boolean,
    val errorMessage: String?,
    val timestamp: Long
)

data class EngagementMetric(
    val type: EngagementType,
    val value: Float,
    val timestamp: Long
)

enum class EngagementType {
    VIEW_TIME, CLICK_RATE, SHARE_RATE, LIKE_RATE
}

data class DashboardData(
    val totalVideos: Int = 0,
    val averageProcessingTime: Long = 0L,
    val successRate: Float = 0f,
    val engagementRate: Float = 0f,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class AnalyticsReport(
    val totalVideosProcessed: Int,
    val averageProcessingTimeMillis: Long,
    val successRate: Float,
    val engagementRate: Float,
    val generatedAt: Long,
    val errorMessage: String? = null
)
