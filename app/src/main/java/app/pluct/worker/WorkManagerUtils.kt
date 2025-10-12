package app.pluct.worker

import android.content.Context
import androidx.work.*
import app.pluct.data.entity.ProcessingTier
import java.util.concurrent.TimeUnit

object WorkManagerUtils {
    
    fun enqueueTranscriptionWork(
        context: Context,
        videoId: String,
        processingTier: ProcessingTier
    ) {
        val workManager = WorkManager.getInstance(context)
        
        val inputData = Data.Builder()
            .putString(TranscriptionWorker.KEY_VIDEO_ID, videoId)
            .putString(TranscriptionWorker.KEY_PROCESSING_TIER, processingTier.name)
            .build()
        
        val workRequest = OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10000L, // 10 seconds
                TimeUnit.MILLISECONDS
            )
            .build()
        
        workManager.enqueue(workRequest)
    }
}
