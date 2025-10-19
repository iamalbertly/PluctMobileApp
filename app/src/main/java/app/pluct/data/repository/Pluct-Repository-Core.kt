package app.pluct.data.repository

import app.pluct.data.dao.OutputArtifactDao
import app.pluct.data.dao.TranscriptDao
import app.pluct.data.dao.VideoItemDao
import app.pluct.data.entity.ArtifactKind
import app.pluct.data.entity.OutputArtifact
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.Transcript
import android.util.Log
import app.pluct.data.entity.VideoItem
import app.pluct.data.service.VideoMetadataService
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluctRepository @Inject constructor(
    private val videoItemDao: VideoItemDao,
    private val transcriptDao: TranscriptDao,
    private val outputArtifactDao: OutputArtifactDao,
    private val metadataService: VideoMetadataService
) {
    // Video operations
    fun streamAll(): Flow<List<VideoItem>> = videoItemDao.streamAll()
    
    suspend fun findByUrl(url: String): VideoItem? = videoItemDao.findByUrl(url)
    
    suspend fun updateVideo(video: VideoItem) = videoItemDao.updateVideo(video)
    
    suspend fun upsertVideo(url: String): String {
        // Fetch metadata for the video
        val metadata = metadataService.fetchVideoMetadata(url)
        
        val video = VideoItem(
            id = UUID.randomUUID().toString(),
            sourceUrl = url,
            title = metadata?.title,
            description = metadata?.description,
            author = metadata?.author,
            thumbnailUrl = metadata?.thumbnailUrl,
            processingTier = ProcessingTier.QUICK_SCAN // Default to free tier for backward compatibility
        )
        return videoItemDao.upsertVideo(video)
    }
    
    suspend fun createVideoWithTier(url: String, processingTier: ProcessingTier): String {
        // Fetch enhanced metadata for the video
        val metadata = metadataService.fetchVideoMetadata(url)
        
        val video = VideoItem(
            id = UUID.randomUUID().toString(),
            sourceUrl = url,
            title = metadata?.title ?: "TikTok Video",
            description = metadata?.description ?: "Shared from TikTok",
            author = metadata?.author ?: "TikTok Creator",
            thumbnailUrl = metadata?.thumbnailUrl,
            processingTier = processingTier,
            status = ProcessingStatus.PENDING
        )
        val videoId = videoItemDao.upsertVideo(video)
        android.util.Log.i("PluctRepository", "Video created with ID: $videoId, title: ${video.title}, author: ${video.author}")
        return videoId
    }
    
    suspend fun markUrlAsInvalid(videoId: String, url: String, errorMessage: String) {
        // Update the video item to mark it as invalid
        val video = videoItemDao.getById(videoId)
        if (video != null) {
            val updatedVideo = video.copy(
                isInvalid = true,
                errorMessage = errorMessage
            )
            videoItemDao.updateVideo(updatedVideo)
        } else {
            // Create a new video entry marked as invalid
            val invalidVideo = VideoItem(
                id = videoId,
                sourceUrl = url,
                isInvalid = true,
                errorMessage = errorMessage,
                processingTier = ProcessingTier.QUICK_SCAN // Default for invalid videos
            )
            videoItemDao.upsertVideo(invalidVideo)
        }
    }
    
    suspend fun markUrlAsValid(videoId: String) {
        // Update the video item to mark it as valid
        val video = videoItemDao.getById(videoId)
        if (video != null) {
            val updatedVideo = video.copy(
                isInvalid = false,
                errorMessage = null
            )
            videoItemDao.updateVideo(updatedVideo)
        }
    }
    
    suspend fun getVideoWithTranscript(videoId: String): Pair<VideoItem?, Transcript?> {
        val video = videoItemDao.getById(videoId)
        val transcript = transcriptDao.getByVideoId(videoId)
        return Pair(video, transcript)
    }
    
    suspend fun deleteVideo(videoId: String) {
        // Delete the video and all associated data
        videoItemDao.deleteVideoById(videoId)
        // Note: Room will handle cascading deletes if foreign keys are set up properly
    }
    
    // NEW METHODS for Choice Engine processing status management
    suspend fun updateVideoStatus(videoId: String, status: ProcessingStatus, failureReason: String? = null) {
        val video = videoItemDao.getById(videoId)
        if (video != null) {
            val updatedVideo = video.copy(
                status = status,
                failureReason = failureReason
            )
            videoItemDao.updateVideo(updatedVideo)
        }
    }
    
    suspend fun getVideoById(videoId: String): VideoItem? {
        return videoItemDao.getById(videoId)
    }
    
    suspend fun getAllVideos(): List<VideoItem> {
        return videoItemDao.getAll()
    }
    
    suspend fun saveTranscriptForVideo(videoId: String, transcript: String) {
        // TODO: Implement transcript storage
        Log.d("PluctRepository", "Saving transcript for video $videoId: ${transcript.length} characters")
    }
    
    suspend fun saveArtifact(videoId: String, artifactType: String, content: String) {
        // TODO: Implement artifact storage
        Log.d("PluctRepository", "Saving artifact $artifactType for video $videoId: ${content.length} characters")
    }
    
    // Transcript operations
    suspend fun saveTranscriptWithLanguage(videoId: String, text: String, language: String? = null) {
        val transcript = Transcript(
            id = UUID.randomUUID().toString(),
            videoId = videoId,
            text = text,
            language = language
        )
        transcriptDao.saveTranscript(transcript)
    }
    
    // Save transcript by userId (for new simplified flow)
    suspend fun saveTranscript(userId: String, transcript: String) {
        // For now, we'll create a simple video entry and save the transcript
        // In a real implementation, you might want to link this to a specific video
        val videoId = upsertVideo("transcript_$userId") // Create a placeholder video
        saveTranscriptWithLanguage(videoId, transcript)
    }
    
    fun getTranscriptFlow(videoId: String): Flow<Transcript?> = transcriptDao.getTranscriptFlow(videoId)
    
    // Artifact operations
    suspend fun saveArtifact(videoId: String, kind: ArtifactKind, content: String, filename: String, mime: String) {
        val artifact = OutputArtifact(
            id = UUID.randomUUID().toString(),
            videoId = videoId,
            kind = kind,
            content = content,
            filename = filename,
            mime = mime
        )
        outputArtifactDao.saveArtifact(artifact)
    }
    
    fun getArtifactsFlow(videoId: String): Flow<List<OutputArtifact>> = outputArtifactDao.getArtifactsFlow(videoId)
    
    // Search operations
    suspend fun searchTranscripts(query: String): List<Transcript> {
        // Simple implementation - in a real app, you might use FTS or external search
        return emptyList() // TODO: Implement search functionality
    }
    
    // Transcript operations for JavaScript bridge
    suspend fun saveTranscript(transcriptData: Map<String, Any>): Long {
        val runId = transcriptData["runId"] as String
        val sourceUrl = transcriptData["sourceUrl"] as String
        val text = transcriptData["text"] as String
        val createdAt = transcriptData["createdAt"] as Long
        val tags = transcriptData["tags"] as List<String>
        
        // Create or find video item
        val videoId = upsertVideo(sourceUrl)
        
        // Create transcript
        val transcript = Transcript(
            id = UUID.randomUUID().toString(),
            videoId = videoId,
            text = text,
            createdAt = createdAt
        )
        
        // Save transcript and return row ID
        return transcriptDao.saveTranscript(transcript)
    }
}

