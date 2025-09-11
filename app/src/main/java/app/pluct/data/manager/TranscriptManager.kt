package app.pluct.data.manager

import android.content.Context
import android.util.Log
import app.pluct.data.processor.UrlProcessor
import app.pluct.data.processor.UrlProcessingResult
import app.pluct.viewmodel.ValuePropositionGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages transcript operations including processing, saving, and value proposition generation
 */
class TranscriptManager(
    private val context: Context,
    private val urlProcessor: UrlProcessor = UrlProcessor(),
    private val valuePropositionGenerator: ValuePropositionGenerator = ValuePropositionGenerator
) {
    companion object {
        private const val TAG = "TranscriptManager"
        private const val TRANSCRIPTS_DIR = "transcripts"
    }
    
    /**
     * Process URL and prepare for transcript extraction
     */
    suspend fun processUrlForTranscript(url: String): TranscriptProcessingResult {
        return try {
            when (val result = urlProcessor.processAndValidateUrl(url)) {
                is UrlProcessingResult.Success -> {
                    TranscriptProcessingResult.ReadyForExtraction(
                        processedUrl = result.processedUrl,
                        normalizedUrl = result.normalizedUrl,
                        displayHost = urlProcessor.extractHostFromUrl(result.normalizedUrl)
                    )
                }
                is UrlProcessingResult.Invalid -> {
                    TranscriptProcessingResult.ValidationError(result.message, result.errorCode)
                }
                is UrlProcessingResult.Error -> {
                    TranscriptProcessingResult.ProcessingError(result.message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing URL for transcript: ${e.message}", e)
            TranscriptProcessingResult.ProcessingError("Failed to process URL: ${e.message}")
        }
    }
    
    /**
     * Handle successful transcript extraction
     */
    suspend fun handleTranscriptSuccess(
        transcript: String,
        url: String,
        shouldSave: Boolean = true
    ): TranscriptResult {
        return try {
            val valueProposition = generateValueProposition(transcript)
            
            val savedFile = if (shouldSave) {
                saveTranscript(transcript, url)
            } else null
            
            TranscriptResult.Success(
                transcript = transcript,
                valueProposition = valueProposition,
                savedFile = savedFile
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling transcript success: ${e.message}", e)
            TranscriptResult.Error("Failed to process transcript: ${e.message}")
        }
    }
    
    /**
     * Generate value proposition from transcript
     */
    private suspend fun generateValueProposition(transcript: String): String {
        return try {
            valuePropositionGenerator.generateValuePropositionFromTranscript(transcript)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating value proposition: ${e.message}", e)
            "Failed to generate value proposition"
        }
    }
    
    /**
     * Save transcript to file
     */
    private suspend fun saveTranscript(transcript: String, url: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                val transcriptsDir = File(context.filesDir, TRANSCRIPTS_DIR)
                if (!transcriptsDir.exists()) {
                    transcriptsDir.mkdirs()
                }
                
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val host = urlProcessor.extractHostFromUrl(url)
                val filename = "transcript_${host}_$timestamp.txt"
                
                val file = File(transcriptsDir, filename)
                
                val content = buildString {
                    appendLine("URL: $url")
                    appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                    appendLine("Host: $host")
                    appendLine("\n--- TRANSCRIPT ---\n")
                    appendLine(transcript)
                }
                
                file.writeText(content)
                Log.d(TAG, "Transcript saved to: ${file.absolutePath}")
                file
            } catch (e: Exception) {
                Log.e(TAG, "Error saving transcript: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * Get list of saved transcripts
     */
    suspend fun getSavedTranscripts(): List<TranscriptFile> {
        return withContext(Dispatchers.IO) {
            try {
                val transcriptsDir = File(context.filesDir, TRANSCRIPTS_DIR)
                if (!transcriptsDir.exists()) {
                    return@withContext emptyList()
                }
                
                transcriptsDir.listFiles { file ->
                    file.isFile && file.name.endsWith(".txt")
                }?.map { file ->
                    TranscriptFile(
                        file = file,
                        name = file.name,
                        lastModified = Date(file.lastModified()),
                        size = file.length()
                    )
                }?.sortedByDescending { it.lastModified } ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting saved transcripts: ${e.message}", e)
                emptyList()
            }
        }
    }
    
    /**
     * Delete a saved transcript
     */
    suspend fun deleteTranscript(file: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val deleted = file.delete()
                if (deleted) {
                    Log.d(TAG, "Transcript deleted: ${file.name}")
                } else {
                    Log.w(TAG, "Failed to delete transcript: ${file.name}")
                }
                deleted
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting transcript: ${e.message}", e)
                false
            }
        }
    }
}

/**
 * Result of transcript processing operation
 */
sealed class TranscriptProcessingResult {
    data class ReadyForExtraction(
        val processedUrl: String,
        val normalizedUrl: String,
        val displayHost: String
    ) : TranscriptProcessingResult()
    
    data class ValidationError(val message: String, val errorCode: String) : TranscriptProcessingResult()
    data class ProcessingError(val message: String) : TranscriptProcessingResult()
}

/**
 * Result of transcript handling operation
 */
sealed class TranscriptResult {
    data class Success(
        val transcript: String,
        val valueProposition: String,
        val savedFile: File?
    ) : TranscriptResult()
    
    data class Error(val message: String) : TranscriptResult()
}

/**
 * Represents a saved transcript file
 */
data class TranscriptFile(
    val file: File,
    val name: String,
    val lastModified: Date,
    val size: Long
)