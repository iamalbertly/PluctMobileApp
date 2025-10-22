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
     * Timeout Constants - Intelligent timeout management
     */
    object Timeouts {
        /**
         * Network connectivity test timeout (ms)
         */
        const val NETWORK_TEST_TIMEOUT = 5000L // 5 seconds
        
        /**
         * Service availability test timeout (ms)
         */
        const val SERVICE_TEST_TIMEOUT = 3000L // 3 seconds
        
        /**
         * Initial page load timeout (ms) - increased for slow connections
         */
        const val INITIAL_PAGE_LOAD_TIMEOUT = 45000L // 45 seconds
        
        /**
         * WebView script injection delay (ms) - increased for stability
         */
        const val SCRIPT_INJECTION_DELAY = 20000L // 20 seconds
        
        /**
         * Maximum wait time for transcript generation (ms) - adaptive based on network
         */
        const val MAX_TRANSCRIPT_WAIT_TIME = 180000L // 3 minutes (increased from 60s)
        
        /**
         * Service availability timeout (ms) - increased for better detection
         */
        const val SERVICE_AVAILABILITY_TIMEOUT = 30000L // 30 seconds (increased from 20s)
        
        /**
         * Interval for checking transcript status (ms)
         */
        const val TRANSCRIPT_CHECK_INTERVAL = 2000L // 2 seconds (increased from 1s)
        
        /**
         * Network idle timeout for WebView (ms)
         */
        const val NETWORK_IDLE_TIMEOUT = 5000L // 5 seconds
        
        /**
         * Retry delay for failed connections (ms)
         */
        const val RETRY_DELAY = 3000L // 3 seconds
        
        /**
         * Maximum retry attempts for network operations
         */
        const val MAX_RETRY_ATTEMPTS = 3
    }
    
    /**
     * WebView Configuration Constants
     */
    object WebView {
        /**
         * User agent string for better compatibility
         */
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        
        /**
         * Maximum memory cache size (bytes)
         */
        const val MAX_CACHE_SIZE = 50 * 1024 * 1024L // 50MB
        
        /**
         * JavaScript interface names for compatibility
         */
        val JAVASCRIPT_INTERFACE_NAMES = arrayOf("Android", "AndroidBridge")
    }
    
    /**
     * Error Messages
     */
    object ErrorMessages {
        const val NO_INTERNET = "No internet connection available. Please check your network settings and try again."
        const val SLOW_CONNECTION = "Your internet connection appears to be slow. This may take longer than usual."
        const val SERVICE_UNAVAILABLE = "The transcript service is currently unavailable. Please try again later."
        const val TIMEOUT_ERROR = "The operation timed out. This may be due to a slow connection or service issues."
        const val WEBVIEW_ERROR = "A browser error occurred. Please try again or use manual entry."
        const val NETWORK_ERROR = "Network error occurred. Please check your connection and try again."
        const val UNKNOWN_ERROR = "An unexpected error occurred. Please try again."
        
        // TokAudit specific error messages
        const val INVALID_DATA = "No valid TikTok data found for this link. The video may be private, deleted, or the link may be invalid."
        const val INVALID_URL = "Invalid URL format. Please check the link and try again."
        const val NO_SUBTITLES = "Subtitles are not available for this video. The video may not have captions enabled."
        const val PROCESSING_TIMEOUT = "The video processing is taking too long. This may indicate an issue with the video or service."
        const val GENERIC_ERROR = "An error occurred while processing the video. Please try again or use a different video."
        const val RED_ERROR_TEXT = "An error was detected in the transcript service. Please try again or use a different video."
        const val ERROR_CONTAINER = "An error was detected in the transcript service. Please try again or use a different video."
        const val TRANSCRIPT_NOT_FOUND = "Transcript not found for this video. The video may not have captions or may be private."
        const val PROVIDER_ERROR = "The transcription provider encountered an error. Please try again or use a different provider."
    }
}
