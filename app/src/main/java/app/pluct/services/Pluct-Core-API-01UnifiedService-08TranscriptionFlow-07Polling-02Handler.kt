package app.pluct.services

import app.pluct.core.debug.PluctCoreDebug01LogManager
import app.pluct.services.api.PluctCoreAPITranscriptionResult01Extractor
import kotlinx.coroutines.delay

/**
 * Pluct-Core-API-01UnifiedService-08TranscriptionFlow-07Polling-02Handler
 * Follows naming convention: [Project]-[Core]-[API]-[UnifiedService]-[TranscriptionFlow]-[Polling]-[Handler]
 * 7 scope layers: Project, Core, API, UnifiedService, TranscriptionFlow, Polling, Handler
 * 
 * Single source of truth for transcription status polling logic.
 * Extracted from TranscriptionFlow01Handler to reduce file size and improve maintainability.
 * Handles adaptive intervals, auth refresh, transcript extraction, completion detection, and timeout handling.
 */
class PluctCoreAPI01UnifiedService08TranscriptionFlow07Polling02Handler(
    private val debugLogManager: PluctCoreDebug01LogManager,
    private val circuitBreaker: PluctCoreAPIUnifiedServiceCircuitBreaker,
    private val jwtGenerator: PluctCoreAPIJWTGenerator,
    private val userIdentification: PluctCoreUserIdentification,
    private val baseUrl: String
) {
    companion object {
        private const val TAG = "Polling02Handler"
    }

    /**
     * Poll transcription status until completion or timeout
     * @param jobId The transcription job ID
     * @param pollAuthToken Initial polling auth token (may be refreshed)
     * @param sanitizedUrl Sanitized URL for logging
     * @param flowRequestId Flow request ID for debug correlation
     * @param clientRequestId Client request ID for logging
     * @param isBackground Whether this is a background operation
     * @param pollIntervalMsFast Fast polling interval in milliseconds
     * @param pollIntervalMsSlow Slow polling interval in milliseconds
     * @param fastPollAttempts Number of fast polling attempts before switching to slow
     * @param maxPollAttempts Maximum number of polling attempts
     * @param hasRefreshedAuth Whether auth has already been refreshed (to avoid redundant refresh)
     * @param pollTranscriptionStatus Function to poll transcription status
     * @param updateDebug Function to update debug timeline
     * @param cleanupAndReturn Function to cleanup and return result
     * @return Result containing final transcription status or failure
     */
    suspend fun pollUntilComplete(
        jobId: String,
        pollAuthToken: String,
        sanitizedUrl: String,
        flowRequestId: String,
        clientRequestId: String,
        isBackground: Boolean,
        pollIntervalMsFast: Long,
        pollIntervalMsSlow: Long,
        fastPollAttempts: Int,
        maxPollAttempts: Int,
        hasRefreshedAuth: Boolean,
        pollTranscriptionStatus: suspend (String, String) -> Result<TranscriptionStatusResponse>,
        updateDebug: (OperationStep, String?, Int?, Int?, OperationTimelineEntry?) -> Unit,
        cleanupAndReturn: suspend (Result<TranscriptionStatusResponse>) -> Result<TranscriptionStatusResponse>
    ): Result<TranscriptionStatusResponse> {
        var currentPollAuthToken = pollAuthToken
        var currentHasRefreshedAuth = hasRefreshedAuth

        val getPollInterval = { attempt: Int ->
            if (isBackground) {
                pollIntervalMsSlow * 2
            } else {
                if (attempt <= fastPollAttempts) pollIntervalMsFast else pollIntervalMsSlow
            }
        }

        // Poll for completion
        repeat(maxPollAttempts) { attempt ->
            val pollingAttempt = attempt + 1
            val currentPollInterval = getPollInterval(pollingAttempt)

            if (circuitBreaker.isOpen()) {
                debugLogManager.logWarning(
                    category = "TRANSCRIPTION",
                    operation = "polling_circuit_open",
                    message = "Circuit breaker open during polling; waiting to retry",
                    details = "Attempt $pollingAttempt of $maxPollAttempts"
                )
                delay(currentPollInterval)
                return@repeat
            }
            updateDebug(OperationStep.POLLING, jobId, pollingAttempt, maxPollAttempts, null)
            delay(currentPollInterval)
            val statusResult = pollTranscriptionStatus(jobId, currentPollAuthToken)
            if (statusResult.isFailure) {
                val cause = statusResult.exceptionOrNull()
                val authError = cause?.message?.contains("401", ignoreCase = true) == true ||
                    cause?.message?.contains("authentication failed", ignoreCase = true) == true ||
                    (cause?.message?.contains("500", ignoreCase = true) == true && cause.message?.contains("authentication", ignoreCase = true) == true)

                if (authError) {
                    if (!currentHasRefreshedAuth) {
                        debugLogManager.logWarning(
                            category = "TRANSCRIPTION",
                            operation = "polling_auth_refresh",
                            message = "Polling auth expired; regenerating user JWT",
                            details = "JobId=$jobId; Attempt=$pollingAttempt"
                        )
                        currentPollAuthToken = jwtGenerator.generateUserJWT(userIdentification.userId)
                        currentHasRefreshedAuth = true
                        return@repeat
                    }
                    return@repeat
                } else {
                    if (pollingAttempt < maxPollAttempts) {
                        debugLogManager.logWarning(
                            category = "TRANSCRIPTION",
                            operation = "polling_transient_failure",
                            message = cause?.message ?: "Polling failed; retrying",
                            details = "Attempt $pollingAttempt of $maxPollAttempts"
                        )
                        return@repeat
                    }
                }

                updateDebug(
                    OperationStep.FAILED,
                    jobId,
                    pollingAttempt,
                    maxPollAttempts,
                    OperationTimelineEntry(
                        OperationStep.POLLING,
                        System.currentTimeMillis(),
                        System.currentTimeMillis(),
                        null, null, null,
                        cause?.message,
                        expected = "2xx status with transcript or in-progress",
                        received = cause?.message ?: "Polling failure",
                        nextAction = "Stop polling; surface error",
                        correlationId = flowRequestId,
                        retryCount = pollingAttempt
                    )
                )
                return cleanupAndReturn(Result.failure(cause ?: Exception("Status check failed")))
            }
            val status = statusResult.getOrNull()
            if (status == null) {
                debugLogManager.logError(
                    category = "TRANSCRIPTION",
                    operation = "polling_null_status",
                    message = "Status result was null despite successful HTTP response - JobId=$jobId; Attempt=$pollingAttempt"
                )
                // Continue polling instead of failing immediately - may be transient
                if (pollingAttempt >= maxPollAttempts) {
                    return cleanupAndReturn(Result.failure(Exception("Status check returned null after $maxPollAttempts attempts")))
                }
                return@repeat
            }

            debugLogManager.logInfo(
                category = "TRANSCRIPTION",
                operation = "polling_status_received",
                message = "Received status from server",
                details = "JobId=$jobId; Status=${status.status}; Progress=${status.progress}; " +
                    "HasTranscript=${status.transcript != null}; " +
                    "HasResult=${status.result != null}; " +
                    "HasResultTranscription=${status.result?.transcription != null}"
            )

            val extraction = PluctCoreAPITranscriptionResult01Extractor.extract(status)
            val transcript = extraction.transcript

            if (transcript == null && (status.status.equals("completed", ignoreCase = true) || status.status.equals("done", ignoreCase = true))) {
                debugLogManager.logError(
                    category = "TRANSCRIPTION",
                    operation = "transcript_extraction_failed",
                    message = "Job marked completed but transcript not found in any expected field",
                    responseBody = buildString {
                        appendLine("JobId=$jobId; Attempt=$pollingAttempt")
                        appendLine("Status=${status.status}")
                        appendLine("HasTranscript=${status.transcript != null}")
                        appendLine("HasResult=${status.result != null}")
                        appendLine("HasResultTranscription=${status.result?.transcription != null}")
                        appendLine("HasText=${status.text != null}")
                        appendLine("TranscriptSource=${extraction.source}")
                    },
                    requestUrl = "$baseUrl/ttt/poll/$jobId"
                )
            }

            if (transcript != null) {
                debugLogManager.logInfo(
                    category = "TRANSCRIPTION",
                    operation = "transcript_found",
                    message = "Transcript extracted successfully",
                    details = "JobId=$jobId; Length=${transcript.length}; Source=${extraction.source}"
                )
            }

            val isCompleted = status.status.equals("completed", ignoreCase = true)
                || status.status.equals("done", ignoreCase = true)
                || transcript != null
                || status._cacheHit == true

            if (isCompleted) {
                if (status._cacheHit == true) {
                    debugLogManager.logInfo(
                        category = "TRANSCRIPTION",
                        operation = "cache_hit_detected",
                        message = "Job already completed and cached (instant result)",
                        details = "JobId=$jobId; Attempt=$pollingAttempt; TranscriptLength=${transcript?.length ?: 0}"
                    )
                }

                val normalizedStatus = status.copy(
                    transcript = transcript ?: status.transcript,
                    status = "completed"
                )
                updateDebug(
                    OperationStep.COMPLETED,
                    jobId,
                    pollingAttempt,
                    maxPollAttempts,
                    OperationTimelineEntry(
                        OperationStep.POLLING,
                        System.currentTimeMillis(),
                        System.currentTimeMillis(),
                        null, null, null, null,
                        expected = "status=completed with transcript",
                        received = "status=${status.status}; hasTranscript=${transcript != null}; cacheHit=${status._cacheHit}",
                        nextAction = "Render transcript",
                        correlationId = flowRequestId,
                        retryCount = pollingAttempt
                    )
                )
                return cleanupAndReturn(Result.success(normalizedStatus))
            }
            if (status.status.equals("failed", ignoreCase = true)) {
                updateDebug(
                    OperationStep.FAILED,
                    jobId,
                    pollingAttempt,
                    maxPollAttempts,
                    OperationTimelineEntry(
                        OperationStep.POLLING,
                        System.currentTimeMillis(),
                        System.currentTimeMillis(),
                        null, null, null,
                        "Remote status failed",
                        expected = "status=completed or transcript",
                        received = "status=${status.status}",
                        nextAction = "Stop polling; surface remote failure",
                        correlationId = flowRequestId,
                        retryCount = pollingAttempt
                    )
                )
                return cleanupAndReturn(Result.failure(Exception("Transcription failed remotely")))
            }
        }

        updateDebug(
            OperationStep.FAILED,
            jobId,
            maxPollAttempts,
            maxPollAttempts,
            OperationTimelineEntry(
                OperationStep.POLLING,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                null, null, null,
                "Polling timeout",
                expected = "status=completed within ${maxPollAttempts * pollIntervalMsSlow}ms",
                received = "Timed out at attempt $maxPollAttempts",
                nextAction = "Stop; ask user to retry",
                correlationId = flowRequestId,
                retryCount = maxPollAttempts
            )
        )
        return cleanupAndReturn(Result.failure(Exception("Transcription timed out. Please retry in a moment.")))
    }
}
