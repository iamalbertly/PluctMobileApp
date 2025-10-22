package app.pluct.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ProcessingStatus { 
    PENDING, 
    TRANSCRIBING, 
    ANALYZING, 
    COMPLETED, 
    FAILED 
}

enum class ProcessingTier { 
    QUICK_SCAN, 
    AI_ANALYSIS 
}

@Entity(tableName = "video_items")
data class VideoItem(
    @PrimaryKey
    val id: String,
    val sourceUrl: String, // UNIQUE constraint will be added in DAO
    val title: String? = null,
    val description: String? = null,
    val author: String? = null,
    val thumbnailUrl: String? = null,
    val tagsCsv: String? = null,
    val isInvalid: Boolean = false,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    
    // NEW FIELDS for Choice Engine
    val status: ProcessingStatus = ProcessingStatus.PENDING,
    val processingTier: ProcessingTier, // Must be provided on creation
    val failureReason: String? = null, // To store error messages
    val isArchived: Boolean = false, // For archive functionality
    
    // ENHANCED METADATA FIELDS
    val creatorName: String? = null, // Full creator name
    val creatorUsername: String? = null, // @username
    val videoId: String? = null, // TikTok video ID
    val duration: Long? = null, // Video duration in seconds
    val viewCount: Long? = null, // View count
    val likeCount: Long? = null, // Like count
    val shareCount: Long? = null, // Share count
    val hashtags: String? = null, // Comma-separated hashtags
    val musicTitle: String? = null, // Music title
    val musicArtist: String? = null, // Music artist
    val lastUpdated: Long = System.currentTimeMillis() // Last metadata update
)

