package app.pluct.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pluct.data.repository.PluctRepository
import app.pluct.data.manager.PluctTranscriptionManagerCoordinator
import app.pluct.utils.ValuePropositionGenerator
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Pluct-Transcript-Processor - Handles transcript processing logic
 */
class PluctTranscriptProcessor(
    private val repository: PluctRepository,
    private val transcriptionManager: PluctTranscriptionManagerCoordinator,
    private val stateManager: PluctIngestStateManager
) : ViewModel() {
    companion object {
        private const val TAG = "PluctTranscriptProcessor"
    }
    
    fun saveTranscript(text: String, language: String? = null, setStateToReady: Boolean = false) {
        val videoId = stateManager.uiState.value.videoId ?: return
        
        viewModelScope.launch {
            try {
                Log.i(TAG, "Saving transcript for video: $videoId")
                repository.saveTranscript(videoId, text, language)
                Log.i(TAG, "Transcript saved successfully")
                
                if (setStateToReady) {
                    stateManager.updateState { it.copy(state = IngestState.READY, error = null) }
                } else {
                    stateManager.updateState { it.copy(error = null) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving transcript: ${e.message}", e)
                stateManager.updateState { it.copy(error = e.message ?: "Failed to save transcript") }
            }
        }
    }
    
    fun generateValueProposition() {
        val videoId = stateManager.uiState.value.videoId ?: return
        val transcript = stateManager.uiState.value.transcript ?: return
        
        Log.i(TAG, "Generating value proposition for video: $videoId")
        viewModelScope.launch {
            try {
                val valueProposition = ValuePropositionGenerator.generateValuePropositionFromTranscript(transcript)
                
                repository.saveArtifact(
                    videoId = videoId,
                    kind = app.pluct.data.entity.ArtifactKind.VALUE_PROPOSITION,
                    content = valueProposition,
                    filename = "value_proposition.txt",
                    mime = "text/plain"
                )
                
                Log.i(TAG, "Value proposition generated and saved")
                stateManager.updateState { it.copy(state = IngestState.READY, summary = valueProposition) }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating value proposition: ${e.message}", e)
                stateManager.updateState { it.copy(error = "Failed to generate value proposition: ${e.message}") }
            }
        }
    }
}
