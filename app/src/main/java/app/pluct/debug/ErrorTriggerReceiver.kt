package app.pluct.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.pluct.core.error.ErrorEnvelope
import app.pluct.core.log.PluctLogger

class ErrorTriggerReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "app.pluct.action.TRIGGER_ERROR" -> {
                val errorType = intent.getStringExtra("error_type") ?: "GENERAL"
                val error = when (errorType) {
                    "VALIDATION" -> ErrorEnvelope("VALIDATION_ERROR", "Test validation error triggered")
                    "NETWORK" -> ErrorEnvelope("NET_IO", "Test network error triggered")
                    "API" -> ErrorEnvelope("API_ERROR", "Test API error triggered")
                    "TIMEOUT" -> ErrorEnvelope("NET_TIMEOUT", "Test timeout error triggered")
                    else -> ErrorEnvelope("GENERAL_ERROR", "Test error triggered")
                }
                // Log the error for testing
                PluctLogger.logError(error)
            }
        }
    }
}
