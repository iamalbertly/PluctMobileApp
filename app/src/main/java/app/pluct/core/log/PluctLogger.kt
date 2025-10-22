package app.pluct.core.log

import android.util.Log

object PluctLogger {
    fun logError(error: app.pluct.core.error.ErrorEnvelope) {
        Log.e("PLUCT_ERR", "{\"code\":\"${error.code}\",\"message\":\"${error.message}\",\"details\":${error.details}}")
    }
    
    fun logBusinessEngineCall(operation: String, success: Boolean, responseTime: Long, details: Map<String, Any> = emptyMap()) {
        Log.i("BUSINESS_ENGINE", "{\"operation\":\"$operation\",\"success\":$success,\"responseTime\":$responseTime,\"details\":$details}")
    }
}
