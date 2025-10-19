package app.pluct.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.repository.PluctRepository
import app.pluct.worker.WorkManagerUtils
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pluct-ViewModel-Processing-Engine - Video processing operations
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
class PluctProcessingEngine @Inject constructor(
    private val repository: PluctRepository,
    private val workManagerUtils: WorkManagerUtils
) {
    
    /**
     * Create a video with specified processing tier
     */
    suspend fun createVideoWithTier(url: String, processingTier: ProcessingTier, context: Context? = null) {
        try {
            android.util.Log.i("PluctProcessingEngine", "🎯 CREATING VIDEO WITH TIER: $url, tier=$processingTier")
            
            // Create video in repository
            val videoId = repository.createVideoWithTier(url, processingTier)
            android.util.Log.i("PluctProcessingEngine", "🎯 VIDEO CREATED: $videoId")
            
            // Enqueue background work if context is available
            if (context != null) {
                // Generate a mock JWT for now (in real implementation, this would come from UserManager)
                val mockJwt = "mock-jwt-${System.currentTimeMillis()}"
                workManagerUtils.enqueueTranscriptionWork(context, videoId, processingTier, mockJwt)
                android.util.Log.i("PluctProcessingEngine", "🎯 BACKGROUND WORK ENQUEUED: $videoId")
            } else {
                android.util.Log.w("PluctProcessingEngine", "⚠️ No context available for background work")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("PluctProcessingEngine", "❌ ERROR CREATING VIDEO: ${e.message}", e)
            throw e
        }
    }
}
