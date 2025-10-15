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
        
        val workRequest = OneTimeWorkRequestBuilder<TTTranscribeWork>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .setInputData(workDataOf("url" to videoId))
            .build()
        
        workManager.enqueueUniqueWork(
            "process-${videoId.hashCode()}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
