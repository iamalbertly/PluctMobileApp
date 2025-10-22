package app.pluct.ui.error

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorCenter @Inject constructor() {
    private val _errors = MutableSharedFlow<app.pluct.core.error.ErrorEnvelope>()
    val errors: SharedFlow<app.pluct.core.error.ErrorEnvelope> = _errors.asSharedFlow()

    fun emitError(error: app.pluct.core.error.ErrorEnvelope) {
        _errors.tryEmit(error)
    }
}
