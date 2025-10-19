package app.pluct.orchestrator

import app.pluct.data.BusinessEngineClient
import app.pluct.orchestrator.OrchestratorResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Orchestrator-Health-Checker - Health check operations
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Singleton
class PluctOrchestratorHealthChecker @Inject constructor(
    private val businessEngineClient: BusinessEngineClient
) {
    
    /**
     * Perform comprehensive health check
     */
    suspend fun performHealthCheck(): OrchestratorResult<Unit> {
        return try {
            val healthResult = businessEngineClient.health()
            
            if (healthResult.isHealthy) {
                OrchestratorResult.Success(Unit)
            } else {
                OrchestratorResult.Failure("Health check failed: ${healthResult.isHealthy}")
            }
        } catch (e: Exception) {
            OrchestratorResult.Failure("Health check error: ${e.message}")
        }
    }
}
