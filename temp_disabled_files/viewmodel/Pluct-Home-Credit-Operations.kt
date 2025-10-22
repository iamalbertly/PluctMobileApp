package app.pluct.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pluct.viewmodel.PluctCreditManager
import app.pluct.error.PluctErrorHandler
import app.pluct.services.PluctAPIRetry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

/**
 * Pluct-Home-Credit-Operations - Credit operations for HomeViewModel
 * Single source of truth for credit operations
 * Adheres to 300-line limit with smart separation of concerns
 */

class PluctHomeCreditOperations @Inject constructor(
    private val creditManager: PluctCreditManager,
    private val errorHandler: PluctErrorHandler,
    private val apiRetry: PluctAPIRetry
) : ViewModel() {
    
    private val _creditBalance = MutableStateFlow(0)
    val creditBalance: StateFlow<Int> = _creditBalance.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun refreshCreditBalance() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val result = apiRetry.executeBusinessEngineCall("getCreditBalance") {
                    creditManager.loadCreditBalance()
                }
                
                result.fold(
                    onSuccess = { balance ->
                        _creditBalance.value = balance as Int
                        Log.d("PluctHomeCreditOperations", "✅ Credit balance refreshed: $balance")
                    },
                    onFailure = { error ->
                        _error.value = "Failed to load credit balance: ${error.message}"
                        Log.e("PluctHomeCreditOperations", "❌ Failed to refresh credit balance", error)
                    }
                )
            } catch (e: Exception) {
                _error.value = "Failed to load credit balance: ${e.message}"
                Log.e("PluctHomeCreditOperations", "❌ Credit balance refresh failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // TODO: Implement deduct and add credit methods when Business Engine API supports them
    
    fun clearError() {
        _error.value = null
    }
}
