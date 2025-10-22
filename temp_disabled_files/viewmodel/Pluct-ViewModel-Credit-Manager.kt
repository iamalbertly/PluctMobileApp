package app.pluct.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pluct.data.manager.UserManager
import app.pluct.data.PluctBusinessEngineUnifiedClientNew
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pluct-ViewModel-Credit-Manager - Credit balance management
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
class PluctCreditManager @Inject constructor(
    private val userManager: UserManager,
    private val businessEngineClient: PluctBusinessEngineUnifiedClientNew
) {
    
    /**
     * Load user credit balance
     */
    suspend fun loadCreditBalance(): Int {
        return try {
            android.util.Log.i("PluctCreditManager", "🎯 LOADING CREDIT BALANCE")
            val userJwt = userManager.getOrCreateUserJwt()
            val balance = businessEngineClient.getCreditBalance(userJwt)
            android.util.Log.i("PluctCreditManager", "🎯 CREDIT BALANCE LOADED: ${balance.balance}")
            balance.balance
        } catch (e: Throwable) {
            android.util.Log.e("PluctCreditManager", "❌ ERROR LOADING CREDIT BALANCE: ${e.message}", e)
            0 // Return 0 as fallback
        }
    }
    
    /**
     * Refresh credit balance
     */
    suspend fun refreshCreditBalance(): Int {
        return loadCreditBalance()
    }
}
