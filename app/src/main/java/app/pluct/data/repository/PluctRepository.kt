package app.pluct.data.repository

import app.pluct.data.dao.OutputArtifactDao
import app.pluct.data.dao.TranscriptDao
import app.pluct.data.dao.VideoItemDao
import app.pluct.data.entity.ArtifactKind
import app.pluct.data.entity.OutputArtifact
import app.pluct.data.entity.Transcript
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
    
    suspend fun upsertVideo(url: String): String {
        // Fetch metadata for the video
        val metadata = metadataService.fetchVideoMetadata(url)
        
        val video = VideoItem(
            id = UUID.randomUUID().toString(),
            sourceUrl = url,
            title = metadata?.title,
            description = metadata?.description,
            author = metadata?.author,
            thumbnailUrl = metadata?.thumbnailUrl
        )
        return videoItemDao.upsertVideo(video)
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
                errorMessage = errorMessage
            )
            videoItemDao.upsertVideo(invalidVideo)
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
    
    // Transcript operations
    suspend fun saveTranscript(videoId: String, text: String, language: String? = null) {
        val transcript = Transcript(
            id = UUID.randomUUID().toString(),
            videoId = videoId,
            text = text,
            language = language
        )
        transcriptDao.saveTranscript(transcript)
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
}

