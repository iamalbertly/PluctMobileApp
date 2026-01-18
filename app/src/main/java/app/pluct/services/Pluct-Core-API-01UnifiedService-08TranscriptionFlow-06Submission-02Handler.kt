package app.pluct.services

import app.pluct.core.debug.PluctCoreDebug01LogManager
import kotlinx.coroutines.delay

/**
 * Pluct-Core-API-01UnifiedService-08TranscriptionFlow-06Submission-02Handler
 * Follows naming convention: [Project]-[Core]-[API]-[UnifiedService]-[TranscriptionFlow]-[Submission]-[Handler]
 * 7 scope layers: Project, Core, API, UnifiedService, TranscriptionFlow, Submission, Handler
 * 
 * Single source of truth for transcription job submission logic.
 * Extracted from TranscriptionFlow01Handler to reduce file size and improve maintainability.
 * Handles timeout retries, auth refresh, jobId recovery, and debug updates.
 */
class PluctCoreAPI01UnifiedService08TranscriptionFlow06Submission02Handler(
    private val debugLogManager: PluctCoreDebug01LogManager,
    private val tokenCache: PluctCoreAPIUnifiedServiceTokenCache,
    private val baseUrl: String,
    private val userIdentification: PluctCoreUserIdentification
) {
    companion object {
        private const val TAG = "Submission02Handler"
    }

    /**
     * Submit transcription job with retry logic, auth refresh, and jobId recovery
     * @param url The sanitized TikTok URL
     * @param vendToken The service token for submission (mutable - may be refreshed)
     * @param clientRequestId Unique client request ID for idempotency
     * @param flowRequestId Flow request ID for debug correlation
     * @param sanitizedUrl Sanitized URL for logging
     * @param submitTranscriptionJob Function to submit transcription job
     * @param createTokenVendingHandler Function to create token vending handler for auth retry
     * @param updateDebug Function to update debug timeline
     * @return Result containing jobId and hasRefreshedAuth flag, or failure
     */
    suspend fun submitWithRetry(
        url: String,
        vendToken: String,
        clientRequestId: String,
        flowRequestId: String,
        sanitizedUrl: String,
        submitTranscriptionJob: suspend (String, String, String) -> Result<TranscriptionResponse>,
        createTokenVendingHandler: (String) -> PluctCoreAPI01UnifiedService08TranscriptionFlow02TokenVending01Handler,
        updateDebug: (OperationStep, String?, OperationTimelineEntry?) -> Unit
    ): Result<Pair<String, Boolean>> {
        val submitStart = System.currentTimeMillis()
        updateDebug(OperationStep.SUBMIT, null, null)
        var submitResult = submitTranscriptionJob(sanitizedUrl, vendToken, clientRequestId)
        var hasRefreshedAuth = false
        var recoveredJobId: String? = null
        var currentVendToken = vendToken

        if (submitResult.isFailure) {
            val submitError = submitResult.exceptionOrNull()
            val timeoutSubmit = PluctCoreAPI01UnifiedService08TranscriptionFlow05Helpers.isSubmitTimeout(submitError)

            if (timeoutSubmit) {
                debugLogManager.logWarning(
                    category = "TRANSCRIPTION",
                    operation = "submit_timeout_retry",
                    message = submitError?.message ?: "Submit timed out; retrying with same clientRequestId",
                    details = "URL=$sanitizedUrl; requestId=$clientRequestId"
                )
                delay(1000)
                val retry = submitTranscriptionJob(sanitizedUrl, currentVendToken, clientRequestId)
                if (retry.isSuccess) {
                    submitResult = retry
                } else {
                    recoveredJobId = PluctCoreAPI01UnifiedService08TranscriptionFlow05Helpers.extractJobIdFromError(retry.exceptionOrNull())
                        ?: PluctCoreAPI01UnifiedService08TranscriptionFlow05Helpers.extractJobIdFromError(submitError)
                }
            }
            val authSubmit = submitError?.message?.contains("401", true) == true ||
                submitError?.message?.contains("auth", true) == true
            val tokenExpired = submitError?.message?.contains("token_expired", true) == true ||
                submitError?.message?.contains("session has expired", true) == true

            if (tokenExpired) {
                tokenCache.clearToken()
                updateDebug(
                    OperationStep.FAILED,
                    null,
                    OperationTimelineEntry(
                        OperationStep.SUBMIT,
                        submitStart,
                        System.currentTimeMillis(),
                        null, null, null,
                        "Business Engine returned token_expired; please retry after backend refresh"
                    )
                )
                return Result.failure(
                    Exception("Business Engine session expired. Please retry in a moment while we refresh access.")
                )
            }
            if (authSubmit) {
                tokenCache.clearToken()
                val retryTokenVendingHandler = createTokenVendingHandler(flowRequestId)
                val retryToken = retryTokenVendingHandler.vendAndLog(
                    label = "Submit Auth Retry",
                    clientRequestId = clientRequestId,
                    flowRequestId = flowRequestId,
                    jobId = null,
                    force = true
                )
                if (retryToken.isSuccess) {
                    currentVendToken = retryToken.getOrNull()!!
                    hasRefreshedAuth = true
                    submitResult = submitTranscriptionJob(sanitizedUrl, currentVendToken, clientRequestId)
                }
                if (submitResult.isFailure) {
                    val secondError = submitResult.exceptionOrNull()
                    debugLogManager.logWarning(
                        category = "TRANSCRIPTION",
                        operation = "submit_auth_failure",
                        message = secondError?.message ?: "Submit 401 after retry",
                        details = "URL=$sanitizedUrl; user=${userIdentification.userId}; Note: Only one auth retry attempted"
                    )
                }
            }
        }
        if (submitResult.isFailure && recoveredJobId.isNullOrBlank()) {
            updateDebug(
                OperationStep.FAILED,
                null,
                OperationTimelineEntry(
                    OperationStep.SUBMIT,
                    submitStart,
                    System.currentTimeMillis(),
                    null, null, null,
                    submitResult.exceptionOrNull()?.message,
                    expected = "202 Accepted with jobId",
                    received = submitResult.exceptionOrNull()?.message ?: "No response",
                    nextAction = "Surface submit failure; suggest retry after session refresh",
                    correlationId = flowRequestId
                )
            )
            return Result.failure(submitResult.exceptionOrNull() ?: Exception("Submit failed"))
        }
        val jobId = recoveredJobId ?: submitResult.getOrNull()!!.jobId
        updateDebug(
            OperationStep.SUBMIT,
            jobId,
            OperationTimelineEntry(
                step = OperationStep.SUBMIT,
                startTime = submitStart,
                endTime = System.currentTimeMillis(),
                duration = System.currentTimeMillis() - submitStart,
                request = RequestDebugDetails("POST", "$baseUrl/ttt/transcribe", "/ttt/transcribe", "Authorization: Bearer ...", """{"url":"$sanitizedUrl"}""", submitStart),
                response = ResponseDebugDetails(202, if (recoveredJobId != null) "Accepted (Recovered)" else "Accepted", "jobId=$jobId", System.currentTimeMillis(), System.currentTimeMillis() - submitStart),
                error = null,
                expected = "202 Accepted + jobId",
                received = "jobId=$jobId",
                nextAction = "Begin polling",
                correlationId = flowRequestId
            )
        )
        val submitDuration = System.currentTimeMillis() - submitStart
        if (submitDuration > 20000) {
            debugLogManager.logWarning(
                category = "TRANSCRIPTION",
                operation = "submit_duration_warning",
                message = "Submit took ${submitDuration}ms",
                details = "JobId=$jobId; URL=$sanitizedUrl"
            )
        }
        return Result.success(Pair(jobId, hasRefreshedAuth))
    }
}
