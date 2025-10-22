package app.pluct.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import app.pluct.data.manager.UserManager
import app.pluct.data.repository.PluctRepository
import app.pluct.api.PluctCoreApiService
import app.pluct.api.VendTokenRequest
import app.pluct.transcription.PluctTranscriptionProcessor
import app.pluct.transcription.PluctTranscriptionCoordinator
import app.pluct.worker.TTTranscribeWork
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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
                // Step 1: Get user JWT for authentication
                val userJwt = userManager.getOrCreateUserJwt()
                Log.d("IngestViewModel", "Using user JWT: ${userJwt.take(20)}...")
                
                // Step 2: Start transcription work with JWT
                val workRequest = OneTimeWorkRequestBuilder<TTTranscribeWork>()
                    .setInputData(workDataOf("url" to url, "userJwt" to userJwt))
                    .setConstraints(Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                    .build()
                
                WorkManager.getInstance(context).enqueue(workRequest)
                _uiState.value = _uiState.value.copy(state = IngestState.PROCESSING, message = "Processing video...")
                
                // Step 3: Monitor work progress
                WorkManager.getInstance(context)
                    .getWorkInfoByIdLiveData(workRequest.id)
                    .observeForever { workInfo ->
                        when (workInfo?.state) {
                            WorkInfo.State.RUNNING -> {
                                _uiState.value = _uiState.value.copy(state = IngestState.PROCESSING, message = "Transcribing...")
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                val transcript = workInfo.outputData.getString("transcript") ?: ""
                                _uiState.value = _uiState.value.copy(state = IngestState.SUCCESS, message = "Transcription completed", transcript = transcript)
                            }
                            WorkInfo.State.FAILED -> {
                                _uiState.value = _uiState.value.copy(state = IngestState.ERROR, message = "Transcription failed")
                            }
                            else -> {}
                        }
                    }

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
