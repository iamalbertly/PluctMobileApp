package app.pluct.orchestrator

import app.pluct.data.BusinessEngineClient
import app.pluct.data.manager.UserManager
import app.pluct.orchestrator.OrchestratorResult
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Orchestrator-Token-Vendor - Token vending operations
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Singleton
class PluctOrchestratorTokenVendor @Inject constructor(
    private val businessEngineClient: BusinessEngineClient,
    private val userManager: UserManager
) {
    
    /**
     * Vend authentication token
     */
    suspend fun vendToken(): OrchestratorResult<String> {
        return try {
            val userJwt = userManager.getOrCreateUserJwt()
            val reqId = UUID.randomUUID().toString()
            val vendResult = businessEngineClient.vendShortToken(userJwt, reqId)
            
            OrchestratorResult.Success(vendResult.token)
        } catch (e: Exception) {
            OrchestratorResult.Failure("Token vending error: ${e.message}")
        }
    }
}
