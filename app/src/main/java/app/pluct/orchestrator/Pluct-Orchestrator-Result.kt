package app.pluct.orchestrator

/**
 * Pluct-Orchestrator-Result - Standardized result types for Business Engine operations
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 * Provides unified error handling and recovery mechanisms
 */
sealed class OrchestratorResult<out T> {
    data class Success<T>(val data: T): OrchestratorResult<T>()
    data class Failure(
        val reason: String, 
        val logId: String? = null,
        val errorCode: String? = null,
        val retryable: Boolean = true
    ): OrchestratorResult<Nothing>()
}

/**
 * Extension functions for result handling
 */
fun <T> OrchestratorResult<T>.isSuccess(): Boolean = this is OrchestratorResult.Success
fun <T> OrchestratorResult<T>.isFailure(): Boolean = this is OrchestratorResult.Failure

fun <T> OrchestratorResult<T>.getOrNull(): T? = when (this) {
    is OrchestratorResult.Success -> data
    is OrchestratorResult.Failure -> null
}

fun <T> OrchestratorResult<T>.getOrThrow(): T = when (this) {
    is OrchestratorResult.Success -> data
    is OrchestratorResult.Failure -> throw RuntimeException("Operation failed: ${reason}")
}

fun <T> OrchestratorResult<T>.fold(
    onSuccess: (T) -> Unit,
    onFailure: (String, String?, String?, Boolean) -> Unit
) = when (this) {
    is OrchestratorResult.Success -> onSuccess(data)
    is OrchestratorResult.Failure -> onFailure(reason, logId, errorCode, retryable)
}
