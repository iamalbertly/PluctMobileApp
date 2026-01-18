package app.pluct.core.api

/**
 * Pluct-Core-API-00Constants - Single source of truth for API constants
 * Follows naming convention: [Project]-[Core]-[API]-[Constants]
 * 4 scope layers: Project, Core, API, Constants
 * Consolidates all BASE_URL and API endpoint constants
 */
object PluctCoreAPI00Constants {
    const val BASE_URL = "https://pluct-business-engine.romeo-lya2.workers.dev"
    
    // Polling intervals
    const val POLL_INTERVAL_MS_FAST = 1500L // Faster polling for first 10 attempts
    const val POLL_INTERVAL_MS_SLOW = 3000L // Slower polling after 10 attempts
    const val FAST_POLL_ATTEMPTS = 10 // Use fast polling for quick jobs
    const val MAX_POLL_ATTEMPTS = 30 // Maximum polling attempts for longer jobs
    
    // HTTP timeouts
    const val DEFAULT_TIMEOUT_MS = 60000L
}
