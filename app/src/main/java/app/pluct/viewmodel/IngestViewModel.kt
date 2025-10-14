package app.pluct.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pluct.data.manager.UserManager
import app.pluct.data.repository.PluctRepository
import app.pluct.api.PluctCoreApiService
import app.pluct.api.VendTokenRequest
import app.pluct.transcription.PluctTranscriptionProcessor
import app.pluct.transcription.PluctTranscriptionCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IngestViewModel @Inject constructor(
    private val apiService: PluctCoreApiService,
    private val transcriptionProcessor: PluctTranscriptionProcessor,
    private val transcriptionCoordinator: PluctTranscriptionCoordinator,
    private val userManager: UserManager,
    private val repository: PluctRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(IngestUiState(state = IngestState.IDLE))
    val uiState = _uiState.asStateFlow()

    private val sharedUrl: String = savedStateHandle.get<String>("url") ?: ""

    init {
        if (sharedUrl.isNotBlank()) {
            startTranscriptionFlow(sharedUrl)
        }
    }

    fun startTranscriptionFlow(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(state = IngestState.LOADING, message = "Preparing...")
            val userId = userManager.getCurrentUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(state = IngestState.ERROR, message = "User not initialized.")
                return@launch
            }

            try {
                // Step 1: Vend the token from your Business Engine
                _uiState.value = _uiState.value.copy(state = IngestState.LOADING, message = "Requesting access token...")
                val tokenResponse = apiService.vendToken(VendTokenRequest(userId = userId))

                if (!tokenResponse.isSuccessful) {
                    if (tokenResponse.code() == 403) {
                         // THIS IS THE MONETIZATION HOOK!
                        _uiState.value = _uiState.value.copy(state = IngestState.ERROR, message = "You're out of credits! Go to settings to buy more.")
                    } else {
                        _uiState.value = _uiState.value.copy(state = IngestState.ERROR, message = "Could not get token: ${tokenResponse.message()}")
                    }
                    return@launch
                }
                val jwt = "Bearer ${tokenResponse.body()!!.token}"

                    // Step 2: Use TTTranscribe service for transcription
                    _uiState.value = _uiState.value.copy(state = IngestState.PROCESSING, message = "Transcribing video with TTTranscribe...")
                    
                    var transcript = ""
                    var transcriptionSuccess = false
                    
                    // Process URL for transcription
                    val processingResult = transcriptionProcessor.processUrlForTranscript(url)
                    when (processingResult) {
                        is app.pluct.transcription.TranscriptProcessingResult.ReadyForExtraction -> {
                            _uiState.value = _uiState.value.copy(message = "Extracting transcript...")
                            
                            // Extract transcript
                            val extractionResult = transcriptionProcessor.extractTranscript(processingResult.processedUrl)
                            when (extractionResult) {
                                is app.pluct.transcription.TranscriptExtractionResult.Success -> {
                                    transcript = extractionResult.transcript
                                    transcriptionSuccess = true
                                }
                                is app.pluct.transcription.TranscriptExtractionResult.Error -> {
                                    _uiState.value = _uiState.value.copy(state = IngestState.ERROR, message = "TTTranscribe failed: ${extractionResult.message}")
                                }
                            }
                        }
                        is app.pluct.transcription.TranscriptProcessingResult.Error -> {
                            _uiState.value = _uiState.value.copy(state = IngestState.ERROR, message = "URL processing failed: ${processingResult.message}")
                        }
                    }
                    
                    if (!transcriptionSuccess) {
                        return@launch
                    }

                // Step 3: Save and show the result
                _uiState.value = _uiState.value.copy(state = IngestState.LOADING, message = "Saving...")
                repository.saveTranscript(userId, transcript)
                _uiState.value = _uiState.value.copy(
                    state = IngestState.SUCCESS, 
                    transcript = transcript, 
                    message = "Transcription completed successfully!"
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(state = IngestState.ERROR, message = "An error occurred: ${e.message}")
            }
        }
    }
    
    fun setProviderUsed(provider: String) {
        _uiState.value = _uiState.value.copy(providerUsed = provider)
    }
    
    fun markWebActivityLaunched() {
        _uiState.value = _uiState.value.copy(isWebActivityLaunched = true)
    }
    
    fun resetWebActivityLaunch() {
        _uiState.value = _uiState.value.copy(isWebActivityLaunched = false)
    }
    
    fun saveTranscript(transcript: String, setStateToReady: Boolean = false) {
        _uiState.value = _uiState.value.copy(transcript = transcript)
        if (setStateToReady) {
            _uiState.value = _uiState.value.copy(state = IngestState.READY)
        }
    }
    
    fun generateValueProposition(transcript: String) {
        _uiState.value = _uiState.value.copy(valueProposition = "Key insights: ${transcript.take(100)}...")
    }
    
    fun tryAnotherProvider(provider: String? = null) {
        _uiState.value = _uiState.value.copy(state = IngestState.READY)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, state = IngestState.IDLE)
    }
    
    fun saveUrlForLaterProcessing(url: String) {
        // Implementation for saving URL for later processing
    }
    
    fun handleWebTranscriptResult(resultCode: Int, data: android.content.Intent?) {
        // Implementation for handling web transcript result
    }
    
    fun showTranscriptSuccess(transcript: String) {
        _uiState.value = _uiState.value.copy(
            state = IngestState.TRANSCRIPT_SUCCESS,
            transcript = transcript
        )
    }
}
