package app.pluct.utils

import android.content.Context
import android.util.Log
import app.pluct.data.database.PluctDatabase
import app.pluct.data.entity.ArtifactKind
import app.pluct.data.entity.OutputArtifact
import app.pluct.data.entity.Transcript
import app.pluct.data.entity.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Utility class for verifying the app's functionality
 */
object VerificationUtils {
    private const val TAG = "VerificationUtils"

    /**
     * Verify the app's functionality by checking database access and components
     */
    suspend fun verifyAppFunctionality(context: Context): VerificationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting app verification")
            val startTime = System.currentTimeMillis()
            
            // Check database access
            val database = PluctDatabase.getDatabase(context)
            val videoDao = database.videoItemDao()
            val transcriptDao = database.transcriptDao()
            val artifactDao = database.outputArtifactDao()
            
            // Create test data
            val testVideoId = "test_" + UUID.randomUUID().toString()
            val testVideo = VideoItem(
                id = testVideoId,
                sourceUrl = "https://example.com/test",
                title = "Test Video",
                author = "Test Author",
                description = "Test Description",
                createdAt = System.currentTimeMillis()
            )
            
            // Insert test video
            videoDao.insertVideo(testVideo)
            Log.d(TAG, "Test video inserted: $testVideoId")
            
            // Insert test transcript
            val testTranscript = Transcript(
                id = "transcript_" + UUID.randomUUID().toString(),
                videoId = testVideoId,
                text = "This is a test transcript",
                language = "en",
                createdAt = System.currentTimeMillis()
            )
            transcriptDao.insertTranscript(testTranscript)
            Log.d(TAG, "Test transcript inserted")
            
            // Insert test artifact
            val testArtifactId = "artifact_" + UUID.randomUUID().toString()
            val testArtifact = OutputArtifact(
                id = testArtifactId,
                videoId = testVideoId,
                kind = ArtifactKind.TRANSCRIPT,
                mime = "text/plain",
                filename = "test.txt",
                content = "Test artifact content",
                createdAt = System.currentTimeMillis()
            )
            artifactDao.insertArtifact(testArtifact)
            Log.d(TAG, "Test artifact inserted: $testArtifactId")
            
            // Verify data can be retrieved
            val retrievedVideo = videoDao.getById(testVideoId)
            val retrievedTranscript = transcriptDao.getByVideoId(testVideoId)
            val retrievedArtifact = artifactDao.getByVideoIdAndKind(testVideoId, ArtifactKind.TRANSCRIPT)
            
            // Clean up test data
            videoDao.deleteVideo(testVideo)
            Log.d(TAG, "Test data cleaned up")
            
            // Check results
            val success = retrievedVideo != null && 
                          retrievedTranscript != null && 
                          retrievedArtifact != null
            
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "Verification completed in ${duration}ms, success: $success")
            
            if (success) {
                VerificationResult.Success(duration)
            } else {
                VerificationResult.Failure("Data verification failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Verification failed: ${e.message}", e)
            VerificationResult.Failure("Error during verification: ${e.message}")
        }
    }
}

/**
 * Result of the verification process
 */
sealed class VerificationResult {
    data class Success(val durationMs: Long) : VerificationResult()
    data class Failure(val message: String) : VerificationResult()
}
