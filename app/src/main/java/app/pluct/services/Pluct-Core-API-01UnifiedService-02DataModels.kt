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
    val text: String? = null, // Alternative field name for transcript
    val confidence: Double? = null,
    val language: String? = null,
    val duration: Int? = null,
    val ok: Boolean? = null,
    val result: TranscriptionResult? = null,
    val metadata: TranscriptionMetadata? = null, // EDGE CASE FIX: Metadata from production responses
    val build: BuildInfo? = null,
    val _cacheHit: Boolean? = null, // Indicates job was retrieved from cache (instant result)
    val _polled: Boolean? = null, // Indicates response came from poll endpoint
    val _cachedAt: String? = null, // Timestamp when result was cached
    val message: String? = null, // Status message from server
    val note: String? = null, // Additional notes or guidance
    val error: String? = null, // Error message if job failed
    val url: String? = null, // Original request URL
    val requestId: String? = null, // Request ID
    val request_id: String? = null, // Alternative field for requestId
    val webhookUrl: String? = null, // Webhook URL if configured
    val statusUrl: String? = null, // Status polling URL
    val statusPollUrl: String? = null, // Alternative status URL
    val estimatedCredits: Int? = null, // Estimated credit cost
    val estimatedProcessingTime: Int? = null, // Estimated processing time in seconds
    val submittedAt: String? = null, // Timestamp when job was submitted
    val completedAt: String? = null, // Timestamp when job completed
    val guidance: String? = null // Usage guidance from server
)

@Serializable
data class TranscriptionResult(
    val transcription: String? = null,
    val confidence: Double? = null,
    val language: String? = null,
    val duration: Double? = null, // Changed to Double to match production (50.48)
    val wordCount: Int? = null,
    val speakerCount: Int? = null,
    val audioQuality: String? = null,
    val processingTime: Int? = null
)

@Serializable
data class TranscriptionMetadata(
    val transcriptLength: Int? = null,
    val hasMetadata: Boolean? = null,
    val processingTime: Int? = null,
    val cacheHit: Boolean? = null
)

enum class HealthStatus {
    HEALTHY, UNHEALTHY, DEGRADED
}





