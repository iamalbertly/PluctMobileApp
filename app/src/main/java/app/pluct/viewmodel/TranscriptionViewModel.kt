package app.pluct.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pluct.data.BusinessEngineClient
import app.pluct.data.EngineError
import app.pluct.data.Status
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

/**
 * ViewModel for handling transcription flow with credit awareness
 */
class TranscriptionViewModel(
    private val businessEngineClient: BusinessEngineClient = BusinessEngineClient()
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TranscriptionUiState())
    val uiState: StateFlow<TranscriptionUiState> = _uiState.asStateFlow()
    
    private val _progressState = MutableStateFlow(ProgressState())
    val progressState: StateFlow<ProgressState> = _progressState.asStateFlow()
    
    companion object {
        private const val TAG = "TranscriptionViewModel"
    }
    
    /**
     * Start transcription process with credit-aware flow
     */
    fun startTranscription(url: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Step 1: Health check
                val health = businessEngineClient.health()
                if (!health.isHealthy) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Service unavailable. Please try again later.",
                        showRetryBanner = true
                    )
                    return@launch
                }
                
                // Step 2: Check balance
                val balance = businessEngineClient.balance()
                if (balance.balance <= 0) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Insufficient credits. Please purchase credits to continue.",
                        showBuyCredits = true
                    )
                    return@launch
                }
                
                // Step 3: Vend token
                val vendResult = try {
                    businessEngineClient.vendToken()
                } catch (e: EngineError) {
                    if (e is EngineError.InsufficientCredits) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Insufficient credits. Please recharge your account.",
                            showBuyCredits = true
                        )
                        return@launch
                    } else if (e is EngineError.RateLimited) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Rate limited. Please wait a moment and try again.",
                            showRetryBanner = true
                        )
                        return@launch
                    } else {
                        handleEngineError(e)
                        return@launch
                    }
                }
                
                // Step 4: Start transcription
                val requestId = businessEngineClient.transcribe(url, vendResult.token)
                
                // Step 5: Poll status
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isTranscribing = true,
                    requestId = requestId
                )
                
                _progressState.value = _progressState.value.copy(
                    isVisible = true,
                    isMinimized = false,
                    phase = "STARTING",
                    percent = 0,
                    note = "Starting transcription..."
                )
                
                // Start polling
                businessEngineClient.pollStatus(requestId).collect { status ->
                    _progressState.value = _progressState.value.copy(
                        phase = status.phase,
                        percent = status.percent,
                        note = status.note
                    )
                    
                    when (status.phase) {
                        "COMPLETED" -> {
                            _uiState.value = _uiState.value.copy(
                                isTranscribing = false,
                                transcriptionResult = status.text ?: "Transcription completed"
                            )
                            _progressState.value = _progressState.value.copy(
                                isVisible = false
                            )
                        }
                        "FAILED" -> {
                            _uiState.value = _uiState.value.copy(
                                isTranscribing = false,
                                error = "Transcription failed: ${status.note}"
                            )
                            _progressState.value = _progressState.value.copy(
                                isVisible = false
                            )
                        }
                    }
                }
                
            } catch (e: EngineError) {
                handleEngineError(e)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "An unexpected error occurred. Please try again."
                )
            }
        }
    }
    
    private fun handleEngineError(error: EngineError) {
        val errorMessage = when (error) {
            is EngineError.Network -> "Network error. Please check your connection."
            is EngineError.Auth -> "Authentication failed. Please try again."
            is EngineError.InsufficientCredits -> "Insufficient credits. Please purchase credits."
            is EngineError.RateLimited -> "Rate limited. Please wait and try again."
            is EngineError.InvalidUrl -> "Invalid URL. Please check the video link."
            is EngineError.Upstream -> "Service error (${error.code}). Please try again."
            is EngineError.Unexpected -> "Unexpected error. Please try again."
        }
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = errorMessage,
            showRetryBanner = error is EngineError.Network || error is EngineError.Upstream,
            showBuyCredits = error is EngineError.InsufficientCredits
        )
    }
    
    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun dismissRetryBanner() {
        _uiState.value = _uiState.value.copy(showRetryBanner = false)
    }
    
    fun dismissBuyCredits() {
        _uiState.value = _uiState.value.copy(showBuyCredits = false)
    }
    
    fun minimizeProgress() {
        _progressState.value = _progressState.value.copy(isMinimized = true)
    }
    
    fun expandProgress() {
        _progressState.value = _progressState.value.copy(isMinimized = false)
    }
    
    fun hideProgress() {
        _progressState.value = _progressState.value.copy(isVisible = false)
    }
}

data class TranscriptionUiState(
    val isLoading: Boolean = false,
    val isTranscribing: Boolean = false,
    val error: String? = null,
    val showRetryBanner: Boolean = false,
    val showBuyCredits: Boolean = false,
    val requestId: String? = null,
    val transcriptionResult: String? = null
)

data class ProgressState(
    val isVisible: Boolean = false,
    val isMinimized: Boolean = false,
    val phase: String = "",
    val percent: Int = 0,
    val note: String = ""
)
