package app.pluct.data.dao

import androidx.room.*
import app.pluct.data.entity.Transcript
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptDao {
    @Query("SELECT * FROM transcripts WHERE videoId = :videoId LIMIT 1")
    suspend fun getByVideoId(videoId: String): Transcript?

    @Query("SELECT * FROM transcripts WHERE videoId = :videoId")
    fun getTranscriptFlow(videoId: String): Flow<Transcript?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscript(transcript: Transcript): Long

    @Update
    suspend fun updateTranscript(transcript: Transcript)

    @Transaction
    suspend fun saveTranscript(transcript: Transcript): Long {
        return insertTranscript(transcript)
    }
}

