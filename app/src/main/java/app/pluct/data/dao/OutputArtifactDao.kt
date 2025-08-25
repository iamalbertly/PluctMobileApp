package app.pluct.data.dao

import androidx.room.*
import app.pluct.data.entity.OutputArtifact
import app.pluct.data.entity.ArtifactKind
import kotlinx.coroutines.flow.Flow

@Dao
interface OutputArtifactDao {
    @Query("SELECT * FROM output_artifacts WHERE videoId = :videoId AND kind = :kind LIMIT 1")
    suspend fun getByVideoIdAndKind(videoId: String, kind: ArtifactKind): OutputArtifact?

    @Query("SELECT * FROM output_artifacts WHERE videoId = :videoId")
    fun getArtifactsFlow(videoId: String): Flow<List<OutputArtifact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtifact(artifact: OutputArtifact)

    @Update
    suspend fun updateArtifact(artifact: OutputArtifact)

    @Transaction
    suspend fun saveArtifact(artifact: OutputArtifact) {
        insertArtifact(artifact)
    }
}

