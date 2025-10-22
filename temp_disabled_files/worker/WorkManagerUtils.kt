package app.pluct.worker

import android.content.Context
import androidx.work.*
import app.pluct.data.entity.ProcessingTier
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerUtils @Inject constructor() {
    
    fun enqueueTranscriptionWork(
        context: Context,
        videoId: String,
        processingTier: ProcessingTier,
        userJwt: String
    ) {
        android.util.Log.i("WorkManagerUtils", "🎯 ENQUEUING TRANSCRIPTION WORK: $videoId, tier=$processingTier")
        android.util.Log.i("WorkManagerUtils", "🎯 USING JWT: ${userJwt.take(20)}... (${if (userJwt.startsWith("mock-")) "MOCK JWT for Quick Scan" else "REAL JWT for AI Analysis"})")
        
        val workManager = WorkManager.getInstance(context)
        
        val workRequest = OneTimeWorkRequestBuilder<TTTranscribeWork>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .setInputData(workDataOf(
                "url" to videoId,
                "processingTier" to processingTier.name, // Pass processing tier
                "userJwt" to userJwt // Use provided JWT
            ))
            .build()
        
        workManager.enqueueUniqueWork(
            "process-${videoId.hashCode()}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        
        android.util.Log.i("WorkManagerUtils", "🎯 WORK ENQUEUED SUCCESSFULLY: $videoId")
    }
}
