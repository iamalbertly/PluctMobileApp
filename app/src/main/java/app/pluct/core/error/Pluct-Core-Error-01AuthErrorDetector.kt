package app.pluct.core.error

import app.pluct.services.PluctCoreAPIDetailedError

/**
 * Pluct-Core-Error-01AuthErrorDetector - Single source of truth for authentication error detection
 * Follows naming convention: [Project]-[Core]-[Error]-[AuthErrorDetector]
 * 4 scope layers: Project, Core, Error, AuthErrorDetector
 * Consolidates all 401/403/auth error detection logic
 */
object PluctCoreError01AuthErrorDetector {
    
    /**
     * Check if error is an authentication error (401 or 403)
     */
    fun isAuthError(error: Throwable?): Boolean {
        if (error == null) return false
        
        if (error is PluctCoreAPIDetailedError) {
            val status = error.technicalDetails.responseStatusCode
            if (status == 401 || status == 403) return true
            if (error.userMessage.contains("exp", ignoreCase = true)) return true
        }
        
        val msg = error.message ?: return false
        return msg.contains("401", ignoreCase = true) ||
            msg.contains("unauthorized", ignoreCase = true) ||
            msg.contains("exp", ignoreCase = true) ||
            msg.contains("authentication", ignoreCase = true) ||
            msg.contains("exp claim timestamp check failed", ignoreCase = true)
    }
    
    /**
     * Check if error is specifically a 401 Unauthorized error
     */
    fun is401Unauthorized(error: Throwable?): Boolean {
        if (error == null) return false
        
        if (error is PluctCoreAPIDetailedError) {
            return error.technicalDetails.responseStatusCode == 401
        }
        
        val errorMessage = error.message?.lowercase() ?: ""
        return errorMessage.contains("401") || 
               errorMessage.contains("unauthorized", ignoreCase = true) ||
               errorMessage.contains("exp claim timestamp check failed", ignoreCase = true)
    }
}
