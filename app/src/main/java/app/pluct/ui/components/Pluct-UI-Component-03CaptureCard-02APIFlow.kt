package app.pluct.ui.components

import android.util.Log
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.PluctCoreAPIDetailedError
import app.pluct.services.PluctCoreAPIUnifiedServiceErrorCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pluct-UI-Component-03CaptureCard-02APIFlow - API flow handler
 * Single source of truth for API flow operations.
 */
object PluctUIComponent03CaptureCardAPIFlow {

    /**
     * Handle complete Business Engine API flow with comprehensive logging.
     */
    fun handleCompleteAPIFlow(
        normalizedUrl: String,
        apiService: PluctCoreAPIUnifiedService,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val tag = "CaptureCard"
        Log.d(tag, "Starting complete Business Engine API flow for URL: $normalizedUrl")

        val cachedError = PluctCoreAPIUnifiedServiceErrorCache.getCachedError(normalizedUrl)
        if (cachedError != null) {
            Log.d(tag, "Found cached error details for URL: $normalizedUrl")
            CoroutineScope(Dispatchers.Main).launch {
                onError(cachedError.getDetailedMessage())
            }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val startTime = System.currentTimeMillis()
                Log.d(tag, "Calling processTikTokVideo API...")
                Log.d(tag, "URL: $normalizedUrl")

                val result = apiService.processTikTokVideo(normalizedUrl)
                val duration = System.currentTimeMillis() - startTime
                Log.d(tag, "Duration: ${duration}ms")

                if (result.isSuccess) {
                    val status = result.getOrNull()!!
                    Log.d(
                        tag,
                        "Flow successful. status=${status.status}, transcriptChars=${status.transcript?.length ?: 0}, confidence=${status.confidence}, language=${status.language}, duration=${status.duration}"
                    )
                    withContext(Dispatchers.Main) {
                        onSuccess("Transcription completed successfully!")
                    }
                } else {
                    val error = result.exceptionOrNull()
                    val errorMessage = when (error) {
                        is PluctCoreAPIDetailedError -> {
                            Log.e(tag, "Complete API flow failed with detailed error", error)
                            PluctCoreAPIUnifiedServiceErrorCache.cacheError(normalizedUrl, error)
                            error.getDetailedMessage()
                        }
                        else -> {
                            val msg = error?.message ?: "Unknown error"
                            Log.e(tag, "Complete API flow failed: $msg", error)
                            msg
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onError(errorMessage)
                    }
                }
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is PluctCoreAPIDetailedError -> {
                        Log.e(tag, "Exception with detailed error", e)
                        PluctCoreAPIUnifiedServiceErrorCache.cacheError(normalizedUrl, e)
                        e.getDetailedMessage()
                    }
                    else -> {
                        val msg = e.message ?: "Unknown error occurred"
                        Log.e(tag, "Exception during complete API flow: $msg", e)
                        msg
                    }
                }
                withContext(Dispatchers.Main) {
                    onError(errorMessage)
                }
            }
        }
    }
}
