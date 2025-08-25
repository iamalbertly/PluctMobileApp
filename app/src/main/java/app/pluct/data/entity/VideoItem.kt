package app.pluct.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val createdAt: Long = System.currentTimeMillis()
)

