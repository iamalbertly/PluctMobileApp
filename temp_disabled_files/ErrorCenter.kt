package app.pluct.ui.error

import app.pluct.core.error.ErrorEnvelope
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@Singleton
class ErrorCenter @Inject constructor() {
    private val _events = MutableSharedFlow<ErrorEnvelope>(extraBufferCapacity = 32)
    val events: SharedFlow<ErrorEnvelope> = _events
    fun emit(err: ErrorEnvelope) { 
        android.util.Log.d("ErrorCenter", "Emitting error: ${err.code} - ${err.message}")
        _events.tryEmit(err) 
    }
}
