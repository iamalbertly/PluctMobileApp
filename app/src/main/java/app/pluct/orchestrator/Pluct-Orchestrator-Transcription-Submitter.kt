package app.pluct.orchestrator

import app.pluct.data.PluctBusinessEngineUnifiedClientNew
import app.pluct.orchestrator.OrchestratorResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Orchestrator-Transcription-Submitter - Transcription submission operations
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */
@Singleton
class PluctOrchestratorTranscriptionSubmitter @Inject constructor(
    private val businessEngineClient: PluctBusinessEngineUnifiedClientNew
) {
    
    /**
     * Submit transcription request
     */
    suspend fun submitTranscription(url: String, token: String): OrchestratorResult<String> {
        return try {
            val result = businessEngineClient.transcribe(url, token)
            
            OrchestratorResult.Success(result)
        } catch (e: Exception) {
            OrchestratorResult.Failure("Transcription submission error: ${e.message}")
        }
    }
}
