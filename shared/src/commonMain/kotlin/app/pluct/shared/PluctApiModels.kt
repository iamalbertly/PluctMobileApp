package app.pluct.shared

import kotlinx.serialization.Serializable

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
    val availableUnits: Int = balance,
    val reservedUnits: Int = 0,
    val main: Int = 0,
    val bonus: Int = 0,
    val freeUsesRemaining: Int = 0,
    val freeWeeklyAllowance: Int = 0,
    val freeWeeklyUsed: Int = 0,
    val freeWeeklyResetAt: String? = null,
    val freeWeeklyWeekStart: String? = null,
    val availableCredits: Int = 0,
    val heldCredits: Int = 0,
    val pendingJobs: Int = 0,
    val updatedAt: String,
    val build: BuildInfo? = null
)

@Serializable
data class ProductQuoteLine(
    val code: String,
    val label: String,
    val priceUnits: Int
)

@Serializable
data class QuoteEstimate(
    val durationSeconds: Int = 0,
    val language: String = "unknown",
    val products: List<ProductQuoteLine> = emptyList(),
    val reserveUnits: Int = 0,
    val maxSettleUnits: Int = 0,
    val cacheHit: Boolean = false
)

@Serializable
data class WalletBalance(
    val availableUnits: Int = 0,
    val reservedUnits: Int = 0
)

@Serializable
data class QuoteRequest(
    val inputType: String = "tiktok_url",
    val url: String,
    val requestedProducts: List<String> = listOf("transcript"),
    val clientRequestId: String
)

@Serializable
data class QuoteResponse(
    val ok: Boolean = true,
    val quoteId: String,
    val priceVersion: String,
    val expiresAt: String,
    val estimated: QuoteEstimate,
    val balance: WalletBalance? = null
)

@Serializable
data class FulfillRequest(
    val quoteId: String,
    val clientRequestId: String
)

@Serializable
data class JobSettlement(
    val reservedUnits: Int = 0,
    val settledUnits: Int = 0,
    val refundedUnits: Int = 0
)

@Serializable
data class FulfillResponse(
    val ok: Boolean = true,
    val jobId: String,
    val status: String,
    val reservedUnits: Int = 0,
    val balanceAfterReservation: Int = 0,
    val products: List<String> = emptyList(),
    val settlement: JobSettlement? = null,
    val result: TranscriptionResult? = null
)

@Serializable
data class MobileSyncService(
    val state: String = "HEALTHY",
    val acceptingJobs: Boolean = true,
    val message: String = "Ready",
    val updatedAt: String = ""
)

@Serializable
data class MobileSyncPolicy(
    val minimumVersionCode: Int = 0,
    val recommendedVersionCode: Int = 0,
    val hardUpdate: Boolean = false,
    val transcriptionEnabled: Boolean = true
)

@Serializable
data class MobileSyncWallet(
    val availableUses: Int = 0,
    val reservedUses: Int = 0,
    val displayText: String = "0 uses left"
)

@Serializable
data class MobileSyncJobChange(
    val jobId: String,
    val status: String,
    val updatedAt: String = ""
)

@Serializable
data class MobileSyncJobs(val changedSinceCursor: List<MobileSyncJobChange> = emptyList())

@Serializable
data class MobileSyncEntitlements(
    val plan: String = "free",
    val adsEnabled: Boolean = true,
    val priorityQueue: Boolean = false
)

@Serializable
data class MobilePremiumOffer(
    val enabled: Boolean = false,
    val successfulPlucts: Int = 0,
    val nextPromptAtSuccessfulPluctCount: Int? = null
)

@Serializable
data class MobileSyncResponse(
    val ok: Boolean = true,
    val revision: String,
    val serverTime: String,
    val serverTimeMs: Long = 0,
    val nextSyncAfterSeconds: Int = 300,
    val budgetMode: String = "NORMAL",
    val policy: MobileSyncPolicy = MobileSyncPolicy(),
    val service: MobileSyncService = MobileSyncService(),
    val wallet: MobileSyncWallet = MobileSyncWallet(),
    val jobs: MobileSyncJobs = MobileSyncJobs(),
    val entitlements: MobileSyncEntitlements = MobileSyncEntitlements(),
    val premiumOffer: MobilePremiumOffer = MobilePremiumOffer()
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
data class VendTokenRequest(
    val userId: String,
    val clientRequestId: String
)

@Serializable
data class VendTokenResponse(
    val ok: Boolean = true,
    val token: String,
    val serviceToken: String? = null,
    val pollingToken: String? = null,
    val expiresIn: Int,
    val balanceAfter: Int,
    val freeUsesRemaining: Int = 0,
    val freeWeeklyAllowance: Int = 0,
    val freeWeeklyUsed: Int = 0,
    val freeWeeklyResetAt: String? = null,
    val freeWeeklyWeekStart: String? = null,
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
data class TranscriptionRequest(
    val url: String,
    val clientRequestId: String
)

@Serializable
data class TranscriptionResponse(
    val ok: Boolean = true,
    val jobId: String,
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
    val progress: Int = 0,
    val transcript: String? = null,
    val text: String? = null,
    val confidence: Double? = null,
    val language: String? = null,
    val duration: Int? = null,
    val ok: Boolean? = null,
    val result: TranscriptionResult? = null,
    val metadata: TranscriptionMetadata? = null,
    val build: BuildInfo? = null,
    val _cacheHit: Boolean? = null,
    val _polled: Boolean? = null,
    val _cachedAt: String? = null,
    val message: String? = null,
    val note: String? = null,
    val error: String? = null,
    val url: String? = null,
    val requestId: String? = null,
    val request_id: String? = null,
    val webhookUrl: String? = null,
    val statusUrl: String? = null,
    val statusPollUrl: String? = null,
    val estimatedCredits: Int? = null,
    val estimatedProcessingTime: Int? = null,
    val submittedAt: String? = null,
    val completedAt: String? = null,
    val guidance: String? = null
)

@Serializable
data class TranscriptionResult(
    val transcription: String? = null,
    val transcript: String? = null,
    val text: String? = null,
    val confidence: Double? = null,
    val language: String? = null,
    val duration: Double? = null,
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
    HEALTHY,
    UNHEALTHY,
    DEGRADED
}
