package app.pluct.services

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

/**
 * Pluct-Analytics-01Tracking - Advanced analytics and usage tracking
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@Singleton
class PluctAnalyticsTracking @Inject constructor() {
    
    private val _analyticsEvents = MutableStateFlow<List<AnalyticsEvent>>(emptyList())
    val analyticsEvents: StateFlow<List<AnalyticsEvent>> = _analyticsEvents.asStateFlow()
    
    private val _userMetrics = MutableStateFlow<UserMetrics>(UserMetrics())
    val userMetrics: StateFlow<UserMetrics> = _userMetrics.asStateFlow()
    
    private val _appMetrics = MutableStateFlow<AppMetrics>(AppMetrics())
    val appMetrics: StateFlow<AppMetrics> = _appMetrics.asStateFlow()
    
    private val analyticsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    data class AnalyticsEvent(
        val id: String,
        val eventType: EventType,
        val eventName: String,
        val properties: Map<String, Any>,
        val timestamp: Long = System.currentTimeMillis(),
        val sessionId: String,
        val userId: String? = null
    )
    
    data class UserMetrics(
        val totalSessions: Int = 0,
        val totalTranscriptions: Int = 0,
        val totalVideosProcessed: Int = 0,
        val averageProcessingTime: Long = 0,
        val totalCreditsUsed: Int = 0,
        val favoriteTier: String? = null,
        val lastActiveTime: Long = 0
    )
    
    data class AppMetrics(
        val totalAppLaunches: Int = 0,
        val totalErrors: Int = 0,
        val totalRecoveries: Int = 0,
        val averageSessionDuration: Long = 0,
        val totalOfflineTime: Long = 0,
        val totalOnlineTime: Long = 0,
        val performanceScore: Double = 0.0
    )
    
    enum class EventType {
        USER_ACTION,
        SYSTEM_EVENT,
        PERFORMANCE_METRIC,
        ERROR_EVENT,
        BUSINESS_EVENT
    }
    
    /**
     * Track an analytics event
     */
    fun trackEvent(
        eventName: String,
        properties: Map<String, Any> = emptyMap(),
        eventType: EventType = EventType.USER_ACTION,
        sessionId: String = generateSessionId(),
        userId: String? = null
    ) {
        val event = AnalyticsEvent(
            id = generateEventId(),
            eventType = eventType,
            eventName = eventName,
            properties = properties,
            sessionId = sessionId,
            userId = userId
        )
        
        val currentEvents = _analyticsEvents.value.toMutableList()
        currentEvents.add(event)
        _analyticsEvents.value = currentEvents
        
        Log.d("PluctAnalyticsTracking", "Tracked event: $eventName")
        
        // Update metrics based on event
        updateMetricsFromEvent(event)
    }
    
    /**
     * Track user action
     */
    fun trackUserAction(action: String, properties: Map<String, Any> = emptyMap()) {
        trackEvent(
            eventName = "user_action_$action",
            properties = properties,
            eventType = EventType.USER_ACTION
        )
    }
    
    /**
     * Track system event
     */
    fun trackSystemEvent(event: String, properties: Map<String, Any> = emptyMap()) {
        trackEvent(
            eventName = "system_$event",
            properties = properties,
            eventType = EventType.SYSTEM_EVENT
        )
    }
    
    /**
     * Track performance metric
     */
    fun trackPerformanceMetric(metric: String, value: Number, properties: Map<String, Any> = emptyMap()) {
        trackEvent(
            eventName = "performance_$metric",
            properties = properties + mapOf("value" to value),
            eventType = EventType.PERFORMANCE_METRIC
        )
    }
    
    /**
     * Track error event
     */
    fun trackError(error: String, properties: Map<String, Any> = emptyMap()) {
        trackEvent(
            eventName = "error_$error",
            properties = properties,
            eventType = EventType.ERROR_EVENT
        )
    }
    
    /**
     * Track business event
     */
    fun trackBusinessEvent(event: String, properties: Map<String, Any> = emptyMap()) {
        trackEvent(
            eventName = "business_$event",
            properties = properties,
            eventType = EventType.BUSINESS_EVENT
        )
    }
    
    /**
     * Update metrics based on event
     */
    private fun updateMetricsFromEvent(event: AnalyticsEvent) {
        val currentUserMetrics = _userMetrics.value
        val currentAppMetrics = _appMetrics.value
        
        when (event.eventName) {
            "user_action_app_launch" -> {
                _appMetrics.value = currentAppMetrics.copy(
                    totalAppLaunches = currentAppMetrics.totalAppLaunches + 1
                )
            }
            "user_action_start_transcription" -> {
                _userMetrics.value = currentUserMetrics.copy(
                    totalTranscriptions = currentUserMetrics.totalTranscriptions + 1
                )
            }
            "system_video_processed" -> {
                _userMetrics.value = currentUserMetrics.copy(
                    totalVideosProcessed = currentUserMetrics.totalVideosProcessed + 1
                )
            }
            "performance_processing_time" -> {
                val processingTime = event.properties["value"] as? Long ?: 0L
                val newAverage = if (currentUserMetrics.totalTranscriptions > 0) {
                    (currentUserMetrics.averageProcessingTime * currentUserMetrics.totalTranscriptions + processingTime) / (currentUserMetrics.totalTranscriptions + 1)
                } else {
                    processingTime
                }
                _userMetrics.value = currentUserMetrics.copy(
                    averageProcessingTime = newAverage
                )
            }
            "business_credit_used" -> {
                val creditsUsed = event.properties["value"] as? Int ?: 0
                _userMetrics.value = currentUserMetrics.copy(
                    totalCreditsUsed = currentUserMetrics.totalCreditsUsed + creditsUsed
                )
            }
            "error_occurred" -> {
                _appMetrics.value = currentAppMetrics.copy(
                    totalErrors = currentAppMetrics.totalErrors + 1
                )
            }
            "system_recovery_successful" -> {
                _appMetrics.value = currentAppMetrics.copy(
                    totalRecoveries = currentAppMetrics.totalRecoveries + 1
                )
            }
        }
    }
    
    /**
     * Get analytics summary
     */
    fun getAnalyticsSummary(): AnalyticsSummary {
        val events = _analyticsEvents.value
        val userMetrics = _userMetrics.value
        val appMetrics = _appMetrics.value
        
        val eventCounts = events.groupBy { it.eventType }.mapValues { it.value.size }
        val topEvents = events.groupBy { it.eventName }.mapValues { it.value.size }
            .toList().sortedByDescending { it.second }.take(10)
        
        return AnalyticsSummary(
            totalEvents = events.size,
            eventCounts = eventCounts,
            topEvents = topEvents,
            userMetrics = userMetrics,
            appMetrics = appMetrics,
            sessionCount = events.map { it.sessionId }.distinct().size
        )
    }
    
    /**
     * Get events by type
     */
    fun getEventsByType(eventType: EventType): List<AnalyticsEvent> {
        return _analyticsEvents.value.filter { it.eventType == eventType }
    }
    
    /**
     * Get events by time range
     */
    fun getEventsByTimeRange(startTime: Long, endTime: Long): List<AnalyticsEvent> {
        return _analyticsEvents.value.filter { 
            it.timestamp in startTime..endTime 
        }
    }
    
    /**
     * Clear old events (older than 30 days)
     */
    fun clearOldEvents() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)
        val filteredEvents = _analyticsEvents.value.filter { it.timestamp > thirtyDaysAgo }
        _analyticsEvents.value = filteredEvents
        
        Log.d("PluctAnalyticsTracking", "Cleared old analytics events")
    }
    
    /**
     * Export analytics data
     */
    fun exportAnalyticsData(): String {
        val summary = getAnalyticsSummary()
        return """
            Analytics Summary:
            - Total Events: ${summary.totalEvents}
            - Session Count: ${summary.sessionCount}
            - User Metrics: ${summary.userMetrics}
            - App Metrics: ${summary.appMetrics}
            - Event Counts: ${summary.eventCounts}
            - Top Events: ${summary.topEvents}
        """.trimIndent()
    }
    
    private fun generateEventId(): String {
        return "event_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
    
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
    
    data class AnalyticsSummary(
        val totalEvents: Int,
        val eventCounts: Map<EventType, Int>,
        val topEvents: List<Pair<String, Int>>,
        val userMetrics: UserMetrics,
        val appMetrics: AppMetrics,
        val sessionCount: Int
    )
}
