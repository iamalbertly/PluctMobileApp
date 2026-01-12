package app.pluct.core.timing

/**
 * Pluct-Core-Timing-01Constants
 * Follows naming convention: [Project]-[Core]-[Timing]-[Sequence][Constants]
 * 5 scope layers: Project, Core, Timing, Sequence, Constants
 * 
 * Technical Debt #1: Single source of truth for all timing constants
 * Extracted from hardcoded delays throughout the codebase for better maintainability
 */
object PluctCoreTiming01Constants {
    
    // UI Animation and Interaction Delays
    const val UI_ANIMATION_DELAY_MS = 500L
    const val UI_STATE_PROPAGATION_DELAY_MS = 800L
    const val UI_BRIEF_DELAY_MS = 1000L
    const val UI_PERMISSION_CHECK_DELAY_MS = 1000L
    
    // Progress Monitoring Intervals
    const val PROGRESS_MICRO_UPDATE_INTERVAL_MS = 500L
    const val PROGRESS_POLL_INTERVAL_BASE_MS = 5000L
    const val PROGRESS_POLL_INTERVAL_FAST_MS = 2000L
    const val PROGRESS_POLL_INTERVAL_SLOW_MS = 10000L
    
    // Network and Connectivity Checks
    const val NETWORK_CHECK_INTERVAL_MS = 2000L
    const val NETWORK_RETRY_DELAY_MS = 10000L
    
    // Transcription Timeouts
    const val TRANSCRIPTION_INITIAL_TIMEOUT_MS = 20000L
    const val TRANSCRIPTION_EXTENDED_TIMEOUT_MS = 30000L
    const val TRANSCRIPTION_STEP_CHECK_INTERVAL_MS = 5000L
    const val TRANSCRIPTION_STEP_TIMEOUT_MS = 10000L
    
    // API Request Delays
    const val API_RETRY_DELAY_MS = 1000L
    const val API_TOKEN_REFRESH_DELAY_MS = 1000L
    
    // Health Monitoring
    const val HEALTH_CHECK_INTERVAL_MS = 60000L // 1 minute
    
    // Background Processing
    const val BACKGROUND_POLL_INTERVAL_MS = 5000L
    const val BACKGROUND_RETRY_DELAY_MS = 10000L
    
    // ADB Detection (for debugging)
    const val ADB_DETECTION_DELAY_MS = 30000L
}
