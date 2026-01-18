package app.pluct.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoItem(
    @PrimaryKey
    val id: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String,
    val author: String,
    val duration: Long,
    val status: ProcessingStatus,
    val progress: Int,
    val transcript: String?,
    val timestamp: Long,
    val description: String? = null,
    val sourceUrl: String = url,
    val tier: ProcessingTier = ProcessingTier.EXTRACT_SCRIPT,
    val createdAt: Long = timestamp,
    val failureReason: String? = null,
    val errorDetails: String? = null,  // JSON serialized PluctCoreAPIDetailedError
    val queueReason: QueueReason? = null,
    val queuedAt: Long? = null,
    val jobId: String? = null  // Transcription job ID for status resumption
)

