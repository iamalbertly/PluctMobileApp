package app.pluct.services

import kotlinx.serialization.Serializable

/**
 * Pluct-Core-API-01UnifiedService-02DataModels - API response data models
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation]-[Responsibility]
 * Single source of truth for API response structures
 */
@Serializable
data class APIMetrics(
    val totalRequests: Long = 0,
    val successfulRequests: Long = 0,
    val failedRequests: Long = 0,
    val averageRetries: Double = 0.0
)

@Serializable
data class BuildInfo(
    val ref: String? = null,
    val deployedAt: String? = null,
    val gitVersion: String? = null
)

@Serializable
data class CreditBalanceResponse(
    val ok: Boolean = true,
    val userId: String,
    val balance: Int,
    val main: Int = 0,
    val bonus: Int = 0,
    val availableCredits: Int = 0,  // Available credits (not held)
    val heldCredits: Int = 0,        // Credits held for in-progress jobs
    val pendingJobs: Int = 0,        // Number of jobs with holds
    val updatedAt: String,
    val build: BuildInfo? = null
)

@Serializable
data class EstimateResponse(
    val ok: Boolean = true,
    val estimatedCredits: Int,
    val estimatedDuration: Int? = null,
    val videoUrl: String,
    val build: BuildInfo? = null
)

@Serializable
data class VendTokenResponse(
    val ok: Boolean = true,
    val token: String,
    // Some backends may return alternative field names; keep both to avoid blank auth tokens.
    val serviceToken: String? = null,
    val pollingToken: String? = null,
    val expiresIn: Int,
    val balanceAfter: Int,
    val requestId: String,
    val build: BuildInfo? = null
)

@Serializable
data class MetadataResponse(
    val url: String,
    val title: String,
    val description: String,
    val author: String,
    val duration: Int,
    val thumbnail: String? = null,
    val cached: Boolean? = null,
    val timestamp: String? = null,
    val handle: String? = null,
    val build: BuildInfo? = null
)

@Serializable
data class TranscriptionResponse(
    val ok: Boolean = true,
    val jobId: String,
    // Optional status URL for direct polling if provided by BE
    val statusUrl: String? = null,
    val status: String,
    val estimatedTime: Int? = null,
    val url: String,
    val build: BuildInfo? = null
)

@Serializable
data class TranscriptionStatusResponse(
    val jobId: String,
    val status: String,
    val progress: Int,
    val transcript: String? = null,
    val confidence: Double? = null,
    val language: String? = null,
    val duration: Int? = null,
    val ok: Boolean? = null,
    val result: TranscriptionResult? = null,
    val build: BuildInfo? = null
)

@Serializable
data class TranscriptionResult(
    val transcription: String? = null,
    val confidence: Double? = null,
    val language: String? = null,
    val duration: Int? = null
)

enum class HealthStatus {
    HEALTHY, UNHEALTHY, DEGRADED
}





