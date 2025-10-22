package app.pluct.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transcripts",
    foreignKeys = [
        ForeignKey(
            entity = VideoItem::class,
            parentColumns = ["id"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("videoId")]
)
data class Transcript(
    @PrimaryKey
    val id: String,
    val videoId: String,
    val text: String,
    val language: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
