package app.pluct.viewmodel

import android.util.Log
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.VideoItem
import app.pluct.data.repository.PluctRepository
import app.pluct.orchestrator.OrchestratorResult
import app.pluct.services.PluctAPIRetry
import app.pluct.services.TikTokMetadata
import app.pluct.services.PluctMetadataService
import app.pluct.services.PluctStatusMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Home-Business-Logic - Business logic for HomeViewModel
 * Single source of truth for home business logic
 * Adheres to 300-line limit with smart separation of concerns
 */

@Singleton
class PluctHomeBusinessLogic @Inject constructor(
    private val repository: PluctRepository,
    private val metadataService: PluctMetadataService,
    private val statusMonitor: PluctStatusMonitor,
    private val apiRetry: PluctAPIRetry
) {
    
    fun isValidTikTokUrl(url: String): Boolean {
        return url.contains("tiktok.com") || url.contains("vm.tiktok.com")
    }
    
    suspend fun fetchAndStoreMetadata(url: String, videoId: String) {
        val result = apiRetry.executeMetadataCall("fetchMetadata") {
            metadataService.fetchMetadata(url) as Any
        }
        
        result.fold(
            onSuccess = { metadata ->
                if (metadata != null) {
                    val tikTokMetadata = metadata as TikTokMetadata
                    Log.d("PluctHomeBusinessLogic", "✅ Metadata fetched: ${tikTokMetadata.title}")
                    repository.updateVideoMetadata(videoId, tikTokMetadata.title, tikTokMetadata.description, tikTokMetadata.author)
                } else {
                    Log.w("PluctHomeBusinessLogic", "⚠️ No metadata returned for: $url")
                }
            },
            onFailure = { error ->
                Log.e("PluctHomeBusinessLogic", "❌ Metadata fetch failed after retries", error)
            }
        )
    }
    
    fun startStatusMonitoring(videoId: String) {
        statusMonitor.startMonitoring(videoId)
    }
    
    fun validateUrl(url: String): String? {
        return when {
            url.isBlank() -> "URL cannot be empty"
            !isValidTikTokUrl(url) -> "Invalid TikTok URL format"
            else -> null
        }
    }
    
    fun normalizeUrl(url: String): String {
        return when {
            url.contains("vm.tiktok.com") -> url
            url.contains("tiktok.com") -> {
                val videoId = url.substringAfterLast("/")
                "https://vm.tiktok.com/$videoId"
            }
            else -> url
        }
    }
    
    suspend fun createVideoItem(url: String, tier: ProcessingTier): String {
        return repository.createVideoWithTier(url, tier)
    }
    
    suspend fun updateVideoMetadata(videoId: String, title: String, description: String, author: String) {
        repository.updateVideoMetadata(videoId, title, description, author)
    }
}
