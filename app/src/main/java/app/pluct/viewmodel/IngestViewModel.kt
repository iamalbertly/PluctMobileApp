package app.pluct.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pluct.data.manager.UserManager
import app.pluct.data.repository.PluctRepository
import app.pluct.data.service.ApiService
import app.pluct.data.service.TranscribeRequest
import app.pluct.data.service.VendTokenRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IngestViewModel @Inject constructor(
    private val apiService: ApiService,
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

                // Step 2: Use the token to call the TTTranscribe service
                _uiState.value = _uiState.value.copy(state = IngestState.LOADING, message = "Transcribing video...")
                val transcribeResponse = apiService.transcribe(jwt, TranscribeRequest(url = url))

                if (!transcribeResponse.isSuccessful) {
                    _uiState.value = _uiState.value.copy(state = IngestState.ERROR, message = "Transcription failed: ${transcribeResponse.message()}")
                    return@launch
                }
                val transcript = transcribeResponse.body()!!.transcript

                // Step 3: Save and show the result
                _uiState.value = _uiState.value.copy(state = IngestState.LOADING, message = "Saving...")
                repository.saveTranscript(userId, transcript) // Implement this in your repo
                _uiState.value = _uiState.value.copy(state = IngestState.SUCCESS, transcript = transcript, message = "Success!")

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(state = IngestState.ERROR, message = "An error occurred: ${e.message}")
            }
        }
    }
}
