package app.pluct.core.error

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-Core-Error-02Center - Global error management center
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@Singleton
class ErrorCenter @Inject constructor() {
    private val _errors = MutableSharedFlow<ErrorEnvelope>()
    val errors: SharedFlow<ErrorEnvelope> = _errors.asSharedFlow()
    
    fun emitError(error: ErrorEnvelope) {
        _errors.tryEmit(error)
    }
    
    fun emitError(code: String, message: String, details: Map<String, Any> = emptyMap()) {
        emitError(ErrorEnvelope(code, message, details))
    }
}
