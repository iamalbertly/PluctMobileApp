package app.pluct.data.converter

import androidx.room.TypeConverter
import app.pluct.data.entity.ArtifactKind

class Converters {
    @TypeConverter
    fun fromArtifactKind(kind: ArtifactKind): String {
        return kind.name
    }

    @TypeConverter
    fun toArtifactKind(kind: String): ArtifactKind {
        return ArtifactKind.valueOf(kind)
    }
}

