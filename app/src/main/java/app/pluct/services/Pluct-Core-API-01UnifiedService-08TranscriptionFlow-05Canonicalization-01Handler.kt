package app.pluct.services

/**
 * Pluct-Core-API-01UnifiedService-08TranscriptionFlow-05Canonicalization-01Handler
 * Follows naming convention: [Project]-[Core]-[API]-[UnifiedService]-[TranscriptionFlow]-[Canonicalization]-[Handler]
 * 7 scope layers: Project, Core, API, UnifiedService, TranscriptionFlow, Canonicalization, Handler
 * 
 * Single source of truth for URL canonicalization logic.
 * Extracted from TranscriptionFlow01Handler to reduce file size and improve maintainability.
 */
object PluctCoreAPI01UnifiedService08TranscriptionFlow05Canonicalization01Handler {
    
    /**
     * Canonicalize TikTok URL and update debug timeline
     * @param sanitizedUrl The URL to canonicalize (mutable - will be updated if resolution succeeds)
     * @param validator URL validator with resolveTikTokRedirect method
     * @param flowRequestId Flow request ID for debug correlation
     * @param updateDebug Function to update debug timeline
     * @return Pair of (resolvedUrl, resolutionResult) where resolvedUrl is the canonicalized URL or original if failed
     */
    suspend fun canonicalizeUrl(
        sanitizedUrl: String,
        validator: PluctCoreValidationInputSanitizer,
        flowRequestId: String,
        updateDebug: (OperationStep, String?, Int?, Int?, OperationTimelineEntry?) -> Unit
    ): Pair<String, app.pluct.services.UrlResolutionResult> {
        val canonicalizeStart = System.currentTimeMillis()
        updateDebug(
            OperationStep.CANONICALIZE,
            null, null, null,
            OperationTimelineEntry(
                step = OperationStep.CANONICALIZE,
                startTime = canonicalizeStart,
                endTime = null,
                duration = null,
                request = RequestDebugDetails("RESOLVE", sanitizedUrl, "resolve", "None", """{"originalUrl":"$sanitizedUrl"}""", canonicalizeStart),
                response = null,
                error = null,
                expected = "Resolve vm.tiktok.com shortlink to canonical @user/video/{id}",
                received = null,
                nextAction = "Use resolved URL for metadata/submit"
            )
        )
        val resolutionResult = validator.resolveTikTokRedirect(sanitizedUrl)
        val resolvedUrl = resolutionResult.resolvedUrl
        val resolutionEnd = System.currentTimeMillis()
        
        if (!resolvedUrl.isNullOrBlank()) {
            updateDebug(
                OperationStep.CANONICALIZE,
                null, null, null,
                OperationTimelineEntry(
                    step = OperationStep.CANONICALIZE,
                    startTime = canonicalizeStart,
                    endTime = resolutionEnd,
                    duration = resolutionEnd - canonicalizeStart,
                    request = RequestDebugDetails("RESOLVE", resolutionResult.originalUrl, "resolve", "None", """{"redirectChain":${resolutionResult.redirectChain}}""", canonicalizeStart),
                    response = ResponseDebugDetails(200, "Canonicalized", "Resolved to $resolvedUrl", resolutionEnd, resolutionEnd - canonicalizeStart, flowRequestId),
                    error = null,
                    expected = "Canonical TikTok URL ready",
                    received = resolvedUrl,
                    nextAction = "Proceed to metadata",
                    correlationId = flowRequestId
                )
            )
            return Pair(resolvedUrl, resolutionResult)
        } else {
            updateDebug(
                OperationStep.CANONICALIZE,
                null, null, null,
                OperationTimelineEntry(
                    step = OperationStep.CANONICALIZE,
                    startTime = canonicalizeStart,
                    endTime = resolutionEnd,
                    duration = resolutionEnd - canonicalizeStart,
                    request = null,
                    response = null,
                    error = resolutionResult.error,
                    expected = "Attempt redirect resolution",
                    received = resolutionResult.redirectChain.joinToString(" -> ").ifBlank { "No redirects" },
                    nextAction = "Proceed with original URL; resolution_missed=true",
                    correlationId = flowRequestId
                )
            )
            return Pair(sanitizedUrl, resolutionResult)
        }
    }
}
