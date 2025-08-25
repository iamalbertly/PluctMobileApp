package app.pluct.utils

/**
 * Application-wide constants
 */
object Constants {
    /**
     * URL Constants
     */
    object Urls {
        /**
         * Known working TikTok URL for testing and fallback
         */
        const val WORKING_TIKTOK_URL = "https://vm.tiktok.com/ZMAF56hjK/"
        
        /**
         * Script TokAudit service URL
         */
        const val SCRIPT_TOKAUDIT_URL = "https://www.script.tokaudit.io/"
    }
    
    /**
     * Timeout Constants
     */
    object Timeouts {
        /**
         * Maximum wait time for transcript generation (ms)
         */
        const val MAX_TRANSCRIPT_WAIT_TIME = 60000L // 60 seconds
        
        /**
         * Service availability timeout (ms)
         */
        const val SERVICE_AVAILABILITY_TIMEOUT = 20000L // 20 seconds
        
        /**
         * Interval for checking transcript status (ms)
         */
        const val TRANSCRIPT_CHECK_INTERVAL = 1000L // 1 second
    }
}
