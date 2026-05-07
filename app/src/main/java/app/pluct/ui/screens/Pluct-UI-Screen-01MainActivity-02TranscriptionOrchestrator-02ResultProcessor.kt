package app.pluct.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.util.Log
import app.pluct.core.debug.PluctCoreDebug01LogManager
import app.pluct.core.error.PluctCoreError03UserMessageFormatter
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.services.PluctCoreAPIDetailedError
import app.pluct.services.TranscriptionStatusResponse
import app.pluct.services.api.PluctCoreAPITranscriptionResult01Extractor

/**
 * Pluct-UI-Screen-01MainActivity-02TranscriptionOrchestrator-02ResultProcessor
 * Follows naming convention: [Project]-[UI]-[Screen]-[MainActivity]-[TranscriptionOrchestrator]-[ResultProcessor]
 * 6 scope layers: Project, UI, Screen, MainActivity, TranscriptionOrchestrator, ResultProcessor
 * 
 * Single source of truth for transcription result processing logic.
 * Extracted from TranscriptionOrchestrator to reduce file size and improve maintainability.
 * Handles success and failure result processing, transcript extraction, database updates, and credit calculations.
 */
object PluctUIScreen01MainActivityTranscriptionOrchestratorResultProcessor {
    
    /**
     * Result of processing
     */
    data class ProcessResult(
        val success: Boolean,
        val newBalance: Int,
        val newFreeUses: Int,
        val message: String?,
        val error: Throwable?
    )
    
    /**
     * Process successful transcription result
     */
    suspend fun processSuccess(
        statusResponse: TranscriptionStatusResponse,
        url: String,
        tier: ProcessingTier,
        currentBalance: Int,
        currentFreeUses: Int,
        currentVideoItem: VideoItem?,
        videoRepository: PluctVideoRepository,
        clipboardManager: ClipboardManager,
        debugLogManager: PluctCoreDebug01LogManager?
    ): ProcessResult {
        Log.d("ResultProcessor", "Transcription completed successfully")
        
        val extraction = PluctCoreAPITranscriptionResult01Extractor.extract(statusResponse)
        val transcriptText = extraction.transcript

        // CRITICAL: Validate transcript exists before saving
        if (transcriptText == null) {
            Log.e("ResultProcessor", "WARNING: Transcript is null or empty in response")
            debugLogManager?.logWarning(
                category = "TRANSCRIPTION",
                operation = "processVideo_missing_transcript",
                message = "Transcription completed but transcript field is empty",
                details = buildString {
                    appendLine("Response structure:")
                    appendLine("  transcript=${statusResponse.transcript != null}")
                    appendLine("  result.transcription=${statusResponse.result?.transcription != null}")
                    appendLine("  text=${statusResponse.text != null}")
                    appendLine("  status=${statusResponse.status}")
                    appendLine("  jobId=${statusResponse.jobId}")
                    appendLine("  transcriptSource=${extraction.source}")
                }
            )
            // UX FIX #5: Improved error message with troubleshooting tips
            val userMessage = buildString {
                appendLine("Transcription completed but no text was found.")
                appendLine()
                appendLine("Possible reasons:")
                appendLine("• The video may be silent or have no speech")
                appendLine("• Audio quality may be too low")
                appendLine("• Background music may be too loud")
                appendLine()
                appendLine("Try:")
                appendLine("• A different video with clear speech")
                appendLine("• A video with subtitles/captions enabled")
                appendLine("• Retrying after a few moments")
            }.trim()
            return ProcessResult(
                success = false,
                newBalance = currentBalance,
                newFreeUses = currentFreeUses,
                message = userMessage,
                error = Exception("Transcript extraction failed")
            )
        }
        
        // Get or create video item for saving transcript
        val existingVideo = videoRepository.getVideoByUrl(url) ?: VideoItem(
            id = System.currentTimeMillis().toString(),
            url = url,
            title = currentVideoItem?.title ?: "",
            thumbnailUrl = "",
            author = currentVideoItem?.author ?: "",
            duration = (statusResponse.duration ?: statusResponse.result?.duration ?: 0).toLong(),
            status = ProcessingStatus.COMPLETED,
            progress = 100,
            transcript = transcriptText,
            timestamp = System.currentTimeMillis(),
            tier = tier,
            jobId = statusResponse.jobId
        )
        
        // UX FIX #3: Extract confidence score from response (check both top-level and result.confidence)
        val confidenceScore = statusResponse.confidence ?: statusResponse.result?.confidence
        
        // Update video item with completed status and transcript
        val completedItem = existingVideo.copy(
            status = ProcessingStatus.COMPLETED,
            progress = 100,
            transcript = transcriptText,
            duration = (statusResponse.duration ?: statusResponse.result?.duration ?: 0).toLong(),
            jobId = statusResponse.jobId,
            title = currentVideoItem?.title ?: existingVideo.title,
            author = currentVideoItem?.author ?: existingVideo.author,
            confidence = confidenceScore  // UX FIX #3: Save confidence score
        )
        
        // Save to database
        val updateResult = videoRepository.insertVideo(completedItem)
        if (updateResult.isFailure) {
            val error = updateResult.exceptionOrNull()
            Log.e("ResultProcessor", "Failed to save transcript: ${error?.message}", error)
            debugLogManager?.logError(
                category = "DATABASE",
                operation = "insertVideo",
                message = "Failed to save completed transcript to database",
                exception = error,
                requestUrl = url
            )
        } else {
            Log.d("ResultProcessor", "Transcript saved successfully: ${transcriptText.length} chars")
        }
        
        // AUTO-COPY TO CLIPBOARD (Simplicity)
        try {
            val clipData = ClipData.newPlainText("Pluct Transcript", transcriptText)
            clipboardManager.setPrimaryClip(clipData)
            Log.d("ResultProcessor", "Transcript auto-copied to clipboard (${transcriptText.length} chars)")
        } catch (e: Exception) {
            Log.w("ResultProcessor", "Failed to auto-copy to clipboard: ${e.message}")
        }
        
        // TECH DEBT #3: Update credits based on tier (removed redundant else)
        val newBalance = when (tier) {
            ProcessingTier.EXTRACT_SCRIPT -> if (currentFreeUses > 0) currentBalance else currentBalance - 1
            ProcessingTier.GENERATE_INSIGHTS -> currentBalance - 2
        }
        val newFreeUses = if (tier == ProcessingTier.EXTRACT_SCRIPT && currentFreeUses > 0) currentFreeUses - 1 else currentFreeUses
        
        debugLogManager?.logInfo(
            category = "TRANSCRIPTION",
            operation = "processVideo_complete",
            message = "Transcription completed",
            details = buildString {
                appendLine("URL: $url")
                appendLine("Tier: $tier")
                appendLine("Transcript chars: ${transcriptText.length}")
                appendLine("New balance: $newBalance")
                appendLine("Free uses remaining: $newFreeUses")
            }
        )
        
        // UX FIX #2: Check for low balance warning when balance < 3
        var successMessage: String? = null
        if (newBalance < 3 && newBalance > 0) {
            successMessage = "Transcription complete. Warning: You only have $newBalance credits left."
        }
        
        return ProcessResult(
            success = true,
            newBalance = newBalance,
            newFreeUses = newFreeUses,
            message = successMessage,
            error = null
        )
    }
    
    /**
     * Process failed transcription result
     */
    suspend fun processFailure(
        error: Throwable,
        url: String,
        tier: ProcessingTier,
        currentBalance: Int,
        currentFreeUses: Int,
        currentVideoItem: VideoItem?,
        videoRepository: PluctVideoRepository,
        debugLogManager: PluctCoreDebug01LogManager?
    ): ProcessResult {
        Log.e("ResultProcessor", "Transcription failed: ${error.message}")
        
        // Log transcription failure to debug system with full details
        debugLogManager?.logError(
            category = "TRANSCRIPTION",
            operation = "processTikTokVideo",
            message = "Transcription failed for URL: $url",
            exception = error,
            requestUrl = url,
            requestPayload = buildString {
                appendLine("REQUEST CONTEXT:")
                appendLine("  Video URL: $url")
                appendLine("  Processing Tier: $tier")
                appendLine("  Current Balance: $currentBalance")
                appendLine("  Free Uses Remaining: $currentFreeUses")
                appendLine()
                appendLine("FLOW DETAILS:")
                appendLine("  Expected Flow: Metadata → Vend Token → Submit → Poll → Complete")
                appendLine("  Cost: ${if (tier == ProcessingTier.EXTRACT_SCRIPT) "1 credit" else "2 credits"}")
            },
            responseBody = buildString {
                appendLine("ERROR DETAILS:")
                appendLine("  Error Type: ${error::class.simpleName}")
                appendLine("  Error Message: ${error.message}")
                if (error is PluctCoreAPIDetailedError) {
                    appendLine()
                    appendLine("DETAILED API ERROR:")
                    appendLine(error.getDetailedMessage())
                }
                appendLine()
                appendLine("STACK TRACE:")
                appendLine(error.stackTraceToString())
            }
        )
        
        // Update video item with failure status
        currentVideoItem?.let { video ->
            videoRepository.updateVideo(video.copy(
                status = ProcessingStatus.FAILED,
                failureReason = "Transcription failed: ${error.message}"
            ))
        }
        
        val isAuth = (error.message?.contains("auth", ignoreCase = true) == true) || 
                     (error.message?.contains("401") == true) ||
                     (error.message?.contains("token_expired", ignoreCase = true) == true) ||
                     (error.message?.contains("session has expired", ignoreCase = true) == true)
        
        val friendly = when {
            isAuth -> "Authentication expired. We'll refresh and retry automatically."
            else -> null
        }

        val userMessage = PluctCoreError03UserMessageFormatter.formatUserMessage(
            error = error,
            technicalMessage = error.message,
            errorCode = "TRANSCRIPTION_FAILED",
            context = "complete transcription flow"
        )
        
        return ProcessResult(
            success = false,
            newBalance = currentBalance,
            newFreeUses = currentFreeUses,
            message = friendly ?: userMessage.message,
            error = error
        )
    }
}
