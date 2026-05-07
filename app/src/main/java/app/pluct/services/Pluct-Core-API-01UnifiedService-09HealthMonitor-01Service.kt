package app.pluct.services

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Core-API-01UnifiedService-09HealthMonitor-01Service
 * Follows naming convention: [Project]-[Core]-[API]-[UnifiedService]-[HealthMonitor]-[Service]
 * 6 scope layers: Project, Core, API, UnifiedService, HealthMonitor, Service
 * Handles health monitoring for Business Engine and TTTranscribe services
 */
@Singleton
class PluctCoreAPI01UnifiedService09HealthMonitor01Service @Inject constructor(
    private val httpClient: PluctCoreAPIHTTPClientImpl
) {
    companion object {
        private const val TAG = "HealthMonitor"
        private const val HEALTH_CHECK_INTERVAL_MS = 60000L
        private const val HEALTH_TTL_MS = 45000L
        private const val DEGRADED_TTL_MS = 120000L
    }

    private val _healthStatus = MutableStateFlow<Map<String, HealthStatus>>(emptyMap())
    val healthStatus: StateFlow<Map<String, HealthStatus>> = _healthStatus.asStateFlow()

    private var healthMonitoringJob: Job? = null
    private val refreshMutex = Mutex()
    @Volatile private var lastRefreshAtMs = 0L
    @Volatile private var lastHealthSnapshot: Map<String, HealthStatus> = emptyMap()

    suspend fun refreshNow(force: Boolean = false): Map<String, HealthStatus> {
        return refreshMutex.withLock {
            val now = System.currentTimeMillis()
            val ttl = if (lastHealthSnapshot["ttt"] == HealthStatus.UNHEALTHY || lastHealthSnapshot["ttt"] == HealthStatus.DEGRADED) DEGRADED_TTL_MS else HEALTH_TTL_MS
            if (!force && lastHealthSnapshot.isNotEmpty() && now - lastRefreshAtMs < ttl) {
                Log.d(TAG, "Reusing health snapshot ageMs=${now - lastRefreshAtMs}")
                return@withLock lastHealthSnapshot
            }
            withContext(Dispatchers.IO) {
            val healthResult = httpClient.executeRequest("GET", "/health/services", null, null)
            if (healthResult.isSuccess) {
                val (_, tttHealth) = parseHealthResponse(healthResult.getOrNull())
                val updated = buildMap {
                    put("api", HealthStatus.HEALTHY)
                    tttHealth?.let { put("ttt", it) }
                }
                _healthStatus.value = updated
                lastHealthSnapshot = updated
                lastRefreshAtMs = System.currentTimeMillis()
                updated
            } else {
                val updated = mapOf("api" to HealthStatus.UNHEALTHY)
                _healthStatus.value = updated
                lastHealthSnapshot = updated
                lastRefreshAtMs = System.currentTimeMillis()
                updated
            }
            }
        }
    }

    /**
     * Start health monitoring
     */
    fun startMonitoring() {
        if (healthMonitoringJob?.isActive == true) {
            Log.d(TAG, "Health monitoring already active")
            return
        }
        
        healthMonitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                refreshNow()
                delay(HEALTH_CHECK_INTERVAL_MS)
            }
        }
        Log.d(TAG, "Health monitoring started")
    }

    /**
     * Stop health monitoring
     */
    fun stopMonitoring() {
        healthMonitoringJob?.cancel()
        healthMonitoringJob = null
        Log.d(TAG, "Health monitoring stopped")
    }

    /**
     * Parse health response from Business Engine
     */
    private fun parseHealthResponse(raw: Any?): Pair<HealthStatus, HealthStatus?> {
        val jsonPayload = raw as? String ?: return HealthStatus.HEALTHY to null
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val obj = json.parseToJsonElement(jsonPayload).jsonObject
            val connectivity = obj["connectivity"]?.jsonObject
            val services = obj["ttt"]?.jsonObject
            val ttt = connectivity?.get("ttt")?.jsonPrimitive?.content?.lowercase()
                ?: services?.get("status")?.jsonPrimitive?.content?.lowercase()
            val tttHealth = when (ttt) {
                "healthy" -> HealthStatus.HEALTHY
                "unhealthy" -> HealthStatus.UNHEALTHY
                "error" -> HealthStatus.UNHEALTHY
                "degraded" -> HealthStatus.DEGRADED
                else -> null
            }
            HealthStatus.HEALTHY to tttHealth
        } catch (_: Exception) {
            HealthStatus.HEALTHY to null
        }
    }
}
