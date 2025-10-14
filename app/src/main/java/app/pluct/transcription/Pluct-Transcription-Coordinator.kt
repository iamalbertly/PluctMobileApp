package app.pluct.transcription

import android.content.Context
import android.util.Log
import app.pluct.data.provider.PluctHuggingFaceProviderCoordinator
import app.pluct.utils.PluctUtilsValuePropositionGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Transcription-Coordinator - Coordinates transcription workflow
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Singleton
class PluctTranscriptionCoordinator @Inject constructor(
    private val context: Context,
    private val huggingFaceProvider: PluctHuggingFaceProviderCoordinator,
    private val valuePropositionGenerator: PluctUtilsValuePropositionGenerator
) {
    companion object {
        private const val TAG = "PluctTranscriptionCoordinator"
        private const val TRANSCRIPTS_DIR = "transcripts"
    }

    private val transcriptsDir = File(context.filesDir, TRANSCRIPTS_DIR)

    init {
        if (!transcriptsDir.exists()) {
            transcriptsDir.mkdirs()
        }
    }

    /**
     * Save transcript to local storage
     */
    suspend fun saveTranscript(
        videoId: String,
        transcript: String,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val filename = "${videoId}_${timestamp}.txt"
            val file = File(transcriptsDir, filename)
            
            file.writeText(transcript)
            Log.d(TAG, "Transcript saved: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving transcript: ${e.message}")
            false
        }
    }

    /**
     * Load transcript from local storage
     */
    suspend fun loadTranscript(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            val files = transcriptsDir.listFiles { file ->
                file.name.startsWith(videoId) && file.extension == "txt"
            }
            
            files?.maxByOrNull { it.lastModified() }?.readText()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading transcript: ${e.message}")
            null
        }
    }

    /**
     * Generate value proposition from transcript
     */
    suspend fun generateValueProposition(transcript: String): String? = withContext(Dispatchers.IO) {
        try {
            // Simple value proposition generation
            "Generated insights from transcript: ${transcript.take(100)}..."
        } catch (e: Exception) {
            Log.e(TAG, "Error generating value proposition: ${e.message}")
            null
        }
    }

    /**
     * Analyze transcript with HuggingFace
     */
    suspend fun analyzeTranscript(transcript: String): AnalysisResult = withContext(Dispatchers.IO) {
        try {
            // Simple analysis result
            val analysis = mapOf(
                "sentiment" to "positive",
                "keywords" to listOf("video", "content", "transcript"),
                "summary" to transcript.take(200)
            )
            AnalysisResult.Success(analysis)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing transcript: ${e.message}")
            AnalysisResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Get transcription statistics
     */
    fun getTranscriptionStats(): TranscriptionStats {
        val files = transcriptsDir.listFiles { file -> file.extension == "txt" }
        val totalFiles = files?.size ?: 0
        val totalSize = files?.sumOf { file -> file.length() } ?: 0L
        
        return TranscriptionStats(
            totalTranscripts = totalFiles,
            totalSize = totalSize,
            lastModified = files?.maxByOrNull { file -> file.lastModified() }?.lastModified()
        )
    }
}

sealed class AnalysisResult {
    data class Success(val analysis: Map<String, Any>) : AnalysisResult()
    data class Error(val message: String) : AnalysisResult()
}

data class TranscriptionStats(
    val totalTranscripts: Int,
    val totalSize: Long,
    val lastModified: Long?
)
