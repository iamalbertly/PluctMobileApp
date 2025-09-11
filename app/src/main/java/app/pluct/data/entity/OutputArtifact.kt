package app.pluct.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ArtifactKind {
    JSONL,
    CHUNKS,
    PROMPT,
    SUMMARY,
    METADATA,
    TRANSCRIPT,
    VALUE_PROPOSITION
}

@Entity(
    tableName = "output_artifacts",
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
data class OutputArtifact(
    @PrimaryKey
    val id: String,
    val videoId: String,
    val kind: ArtifactKind,
    val mime: String,
    val filename: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
