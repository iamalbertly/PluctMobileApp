package app.pluct.orchestrator

import app.pluct.data.PluctBusinessEngineUnifiedClientNew
import app.pluct.data.manager.UserManager
import app.pluct.orchestrator.OrchestratorResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Orchestrator-Credit-Manager - Credit management operations
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Singleton
class PluctOrchestratorCreditManager @Inject constructor(
    private val businessEngineClient: PluctBusinessEngineUnifiedClientNew,
    private val userManager: UserManager
) {
    
    /**
     * Perform credit balance check
     */
    suspend fun performCreditCheck(): OrchestratorResult<Unit> {
        return try {
            val userJwt = userManager.getOrCreateUserJwt()
            val balanceResult = businessEngineClient.balance(userJwt)
            
            if (balanceResult.balance > 0) {
                OrchestratorResult.Success(Unit)
            } else {
                OrchestratorResult.Failure("Insufficient credits: ${balanceResult.balance}")
            }
        } catch (e: Exception) {
            OrchestratorResult.Failure("Credit check error: ${e.message}")
        }
    }
}
