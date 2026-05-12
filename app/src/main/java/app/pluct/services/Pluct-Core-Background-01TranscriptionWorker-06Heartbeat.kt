package app.pluct.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Shared heartbeat coroutine for transcription progress notifications (worker vs foreground CaptureCard flow).
 */
internal fun CoroutineScope.launchPluctTranscriptionProgressHeartbeat(
    startProgress: Int,
    onPulse: suspend (progress: Int, label: String) -> Unit
): Job = launch {
    var progress = startProgress
    var index = 0
    while (isActive) {
        delay(PluctCoreTranscription02ProgressHeartbeat01Policy.TICK_MS)
        onPulse(
            progress,
            PluctCoreTranscription02ProgressHeartbeat01Policy.LABELS[
                index % PluctCoreTranscription02ProgressHeartbeat01Policy.LABELS.size
            ]
        )
        index++
        progress = PluctCoreTranscription02ProgressHeartbeat01Policy.nextProgress(progress)
    }
}
