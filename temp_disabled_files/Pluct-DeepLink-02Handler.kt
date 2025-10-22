package app.pluct.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import app.pluct.ui.error.ErrorCenter
import app.pluct.core.error.ErrorEnvelope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pluct-DeepLink-02Handler - Comprehensive deep link handler
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Implements deep linking for debug testing, error simulation, and app navigation
 */
@Singleton
class PluctDeepLinkHandler @Inject constructor(
    private val errorCenter: ErrorCenter
) {
    
    companion object {
        private const val TAG = "PluctDeepLinkHandler"
        
        // Deep link schemes and hosts
        const val SCHEME_PLUCT = "pluct"
        const val HOST_DEBUG = "debug"
        const val HOST_INGEST = "ingest"
        const val HOST_ERROR = "error"
        const val HOST_TEST = "test"
        
        // Debug error types
        const val ERROR_NETWORK = "network"
        const val ERROR_VALIDATION = "validation"
        const val ERROR_TIMEOUT = "timeout"
        const val ERROR_AUTH = "auth"
        const val ERROR_CREDITS = "credits"
    }
    
    /**
     * Handle incoming deep link
     */
    fun handleDeepLink(uri: Uri, context: Context): Boolean {
        Log.d(TAG, "🎯 Handling deep link: $uri")
        
        return when {
            uri.scheme == SCHEME_PLUCT && uri.host == HOST_DEBUG -> {
                handleDebugDeepLink(uri, context)
            }
            uri.scheme == SCHEME_PLUCT && uri.host == HOST_INGEST -> {
                handleIngestDeepLink(uri, context)
            }
            uri.scheme == SCHEME_PLUCT && uri.host == HOST_ERROR -> {
                handleErrorDeepLink(uri, context)
            }
            uri.scheme == SCHEME_PLUCT && uri.host == HOST_TEST -> {
                handleTestDeepLink(uri, context)
            }
            else -> {
                Log.w(TAG, "⚠️ Unknown deep link: $uri")
                false
            }
        }
    }
    
    /**
     * Handle debug deep links (pluct://debug/...)
     */
    private fun handleDebugDeepLink(uri: Uri, context: Context): Boolean {
        val pathSegments = uri.pathSegments
        Log.d(TAG, "🔧 Debug deep link: ${pathSegments.joinToString("/")}")
        
        when (pathSegments.firstOrNull()) {
            "error" -> {
                val errorType = pathSegments.getOrNull(1) ?: ERROR_NETWORK
                triggerDebugError(errorType)
                return true
            }
            "clear" -> {
                clearAllErrors()
                return true
            }
            "test" -> {
                runDebugTests()
                return true
            }
            else -> {
                Log.w(TAG, "⚠️ Unknown debug path: ${pathSegments.joinToString("/")}")
                return false
            }
        }
    }
    
    /**
     * Handle ingest deep links (pluct://ingest/...)
     */
    private fun handleIngestDeepLink(uri: Uri, context: Context): Boolean {
        val url = uri.pathSegments.joinToString("/")
        Log.d(TAG, "📥 Ingest deep link: $url")
        
        if (url.isNotBlank()) {
            // Create intent to process the URL
            val intent = Intent(context, Class.forName("app.pluct.PluctUIMain02Complete")).apply {
                action = "app.pluct.action.CAPTURE_INSIGHT"
                putExtra("capture_url", url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
            return true
        }
        
        return false
    }
    
    /**
     * Handle error deep links (pluct://error/...)
     */
    private fun handleErrorDeepLink(uri: Uri, context: Context): Boolean {
        val errorType = uri.pathSegments.firstOrNull() ?: ERROR_NETWORK
        Log.d(TAG, "🚨 Error deep link: $errorType")
        
        triggerDebugError(errorType)
        return true
    }
    
    /**
     * Handle test deep links (pluct://test/...)
     */
    private fun handleTestDeepLink(uri: Uri, context: Context): Boolean {
        val testType = uri.pathSegments.firstOrNull() ?: "all"
        Log.d(TAG, "🧪 Test deep link: $testType")
        
        when (testType) {
            "all" -> runAllTests()
            "api" -> runApiTests()
            "ui" -> runUITests()
            "error" -> runErrorTests()
            else -> {
                Log.w(TAG, "⚠️ Unknown test type: $testType")
                return false
            }
        }
        
        return true
    }
    
    /**
     * Trigger debug error based on type
     */
    private fun triggerDebugError(errorType: String) {
        Log.i(TAG, "🎯 Triggering debug error: $errorType")
        
        val error = when (errorType) {
            ERROR_NETWORK -> ErrorEnvelope(
                code = "DEBUG_NETWORK_ERROR",
                message = "Debug network error triggered",
                details = mapOf(
                    "type" to "network",
                    "trigger" to "debug_deep_link",
                    "timestamp" to System.currentTimeMillis()
                )
            )
            ERROR_VALIDATION -> ErrorEnvelope(
                code = "DEBUG_VALIDATION_ERROR",
                message = "Debug validation error triggered",
                details = mapOf(
                    "type" to "validation",
                    "trigger" to "debug_deep_link",
                    "timestamp" to System.currentTimeMillis()
                )
            )
            ERROR_TIMEOUT -> ErrorEnvelope(
                code = "DEBUG_TIMEOUT_ERROR",
                message = "Debug timeout error triggered",
                details = mapOf(
                    "type" to "timeout",
                    "trigger" to "debug_deep_link",
                    "timestamp" to System.currentTimeMillis()
                )
            )
            ERROR_AUTH -> ErrorEnvelope(
                code = "DEBUG_AUTH_ERROR",
                message = "Debug authentication error triggered",
                details = mapOf(
                    "type" to "auth",
                    "trigger" to "debug_deep_link",
                    "timestamp" to System.currentTimeMillis()
                )
            )
            ERROR_CREDITS -> ErrorEnvelope(
                code = "DEBUG_CREDITS_ERROR",
                message = "Debug credits error triggered",
                details = mapOf(
                    "type" to "credits",
                    "trigger" to "debug_deep_link",
                    "timestamp" to System.currentTimeMillis()
                )
            )
            else -> ErrorEnvelope(
                code = "DEBUG_UNKNOWN_ERROR",
                message = "Debug unknown error triggered",
                details = mapOf(
                    "type" to "unknown",
                    "trigger" to "debug_deep_link",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
        
        errorCenter.emitError(error)
    }
    
    /**
     * Clear all errors
     */
    private fun clearAllErrors() {
        Log.i(TAG, "🧹 Clearing all errors")
        errorCenter.clearAllErrors()
    }
    
    /**
     * Run debug tests
     */
    private fun runDebugTests() {
        Log.i(TAG, "🧪 Running debug tests")
        
        // Test error emission
        triggerDebugError(ERROR_NETWORK)
        
        // Test error clearing
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            clearAllErrors()
        }, 3000)
    }
    
    /**
     * Run all tests
     */
    private fun runAllTests() {
        Log.i(TAG, "🧪 Running all tests")
        runApiTests()
        runUITests()
        runErrorTests()
    }
    
    /**
     * Run API tests
     */
    private fun runApiTests() {
        Log.i(TAG, "🔗 Running API tests")
        // TODO: Implement API connectivity tests
    }
    
    /**
     * Run UI tests
     */
    private fun runUITests() {
        Log.i(TAG, "🎨 Running UI tests")
        // TODO: Implement UI interaction tests
    }
    
    /**
     * Run error tests
     */
    private fun runErrorTests() {
        Log.i(TAG, "🚨 Running error tests")
        
        // Test different error types
        listOf(ERROR_NETWORK, ERROR_VALIDATION, ERROR_TIMEOUT, ERROR_AUTH, ERROR_CREDITS)
            .forEachIndexed { index, errorType ->
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    triggerDebugError(errorType)
                }, (index * 1000).toLong())
            }
    }
    
    /**
     * Create debug deep link URI
     */
    fun createDebugUri(path: String): Uri {
        return Uri.Builder()
            .scheme(SCHEME_PLUCT)
            .authority(HOST_DEBUG)
            .appendPath(path)
            .build()
    }
    
    /**
     * Create ingest deep link URI
     */
    fun createIngestUri(url: String): Uri {
        return Uri.Builder()
            .scheme(SCHEME_PLUCT)
            .authority(HOST_INGEST)
            .appendPath(url)
            .build()
    }
}
