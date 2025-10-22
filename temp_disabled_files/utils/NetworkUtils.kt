package app.pluct.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

/**
 * Comprehensive network connectivity utilities
 */
object NetworkUtils {
    private const val TAG = "NetworkUtils"
    
    /**
     * Network connectivity states
     */
    enum class ConnectivityState {
        CONNECTED,
        CONNECTED_SLOW,
        CONNECTED_UNRELIABLE,
        DISCONNECTED,
        UNKNOWN
    }
    
    /**
     * Network quality levels
     */
    enum class NetworkQuality {
        EXCELLENT,    // Fast, reliable connection
        GOOD,         // Moderate speed, reliable
        POOR,         // Slow or unreliable
        UNRELIABLE,   // Intermittent connectivity
        NONE          // No connectivity
    }
    
    /**
     * Check basic internet connectivity
     */
    fun isInternetAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking internet connectivity: ${e.message}")
            false
        }
    }
    
    /**
     * Get detailed connectivity state
     */
    fun getConnectivityState(context: Context): ConnectivityState {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            if (capabilities == null) {
                return ConnectivityState.DISCONNECTED
            }
            
            if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                return ConnectivityState.DISCONNECTED
            }
            
            // Check if connection is validated
            if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                return ConnectivityState.CONNECTED_UNRELIABLE
            }
            
            // Check connection speed
            val downSpeed = capabilities.linkDownstreamBandwidthKbps
            val upSpeed = capabilities.linkUpstreamBandwidthKbps
            
            when {
                downSpeed >= 10000 -> ConnectivityState.CONNECTED // 10+ Mbps
                downSpeed >= 1000 -> ConnectivityState.CONNECTED_SLOW // 1-10 Mbps
                else -> ConnectivityState.CONNECTED_UNRELIABLE // < 1 Mbps
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connectivity state: ${e.message}")
            ConnectivityState.UNKNOWN
        }
    }
    
    /**
     * Test actual internet connectivity by making a test request
     */
    suspend fun testInternetConnectivity(timeoutMs: Long = 5000): NetworkQuality {
        return try {
            val url = URL("https://www.google.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = timeoutMs.toInt()
            connection.readTimeout = timeoutMs.toInt()
            connection.requestMethod = "HEAD"
            
            val startTime = System.currentTimeMillis()
            val responseCode = connection.responseCode
            val responseTime = System.currentTimeMillis() - startTime
            
            connection.disconnect()
            
            when {
                responseCode == 200 && responseTime < 1000 -> NetworkQuality.EXCELLENT
                responseCode == 200 && responseTime < 3000 -> NetworkQuality.GOOD
                responseCode == 200 && responseTime < 10000 -> NetworkQuality.POOR
                responseCode == 200 -> NetworkQuality.UNRELIABLE
                else -> NetworkQuality.NONE
            }
        } catch (e: Exception) {
            Log.w(TAG, "Internet connectivity test failed: ${e.message}")
            NetworkQuality.NONE
        }
    }
    
    /**
     * Test specific service connectivity (script.tokaudit.io)
     */
    suspend fun testServiceConnectivity(timeoutMs: Long = 3000): NetworkQuality {
        return try {
            val url = URL("https://www.script.tokaudit.io")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = timeoutMs.toInt()
            connection.readTimeout = timeoutMs.toInt()
            connection.requestMethod = "HEAD"
            
            val startTime = System.currentTimeMillis()
            val responseCode = connection.responseCode
            val responseTime = System.currentTimeMillis() - startTime
            
            connection.disconnect()
            
            when {
                responseCode == 200 && responseTime < 2000 -> NetworkQuality.EXCELLENT
                responseCode == 200 && responseTime < 5000 -> NetworkQuality.GOOD
                responseCode == 200 && responseTime < 15000 -> NetworkQuality.POOR
                responseCode == 200 -> NetworkQuality.UNRELIABLE
                else -> NetworkQuality.NONE
            }
        } catch (e: Exception) {
            Log.w(TAG, "Service connectivity test failed: ${e.message}")
            NetworkQuality.NONE
        }
    }
    
    /**
     * Get user-friendly connectivity message
     */
    fun getConnectivityMessage(state: ConnectivityState, quality: NetworkQuality? = null): String {
        return when {
            state == ConnectivityState.DISCONNECTED -> "No internet connection available"
            state == ConnectivityState.CONNECTED_UNRELIABLE -> "Internet connection is unstable"
            state == ConnectivityState.CONNECTED_SLOW -> "Internet connection is slow"
            quality == NetworkQuality.NONE -> "Cannot reach the transcript service"
            quality == NetworkQuality.UNRELIABLE -> "Transcript service is responding slowly"
            quality == NetworkQuality.POOR -> "Connection to transcript service is poor"
            quality == NetworkQuality.GOOD -> "Connection to transcript service is good"
            quality == NetworkQuality.EXCELLENT -> "Connection to transcript service is excellent"
            else -> "Internet connection available"
        }
    }
    
    /**
     * Get recommended timeout based on network quality
     */
    fun getRecommendedTimeout(quality: NetworkQuality): Long {
        return when (quality) {
            NetworkQuality.EXCELLENT -> 30000L // 30 seconds
            NetworkQuality.GOOD -> 60000L // 1 minute
            NetworkQuality.POOR -> 120000L // 2 minutes
            NetworkQuality.UNRELIABLE -> 180000L // 3 minutes
            NetworkQuality.NONE -> 10000L // 10 seconds (fail fast)
        }
    }
    
    /**
     * Check if network is suitable for WebView operations
     */
    fun isNetworkSuitableForWebView(context: Context): Boolean {
        val state = getConnectivityState(context)
        return state == ConnectivityState.CONNECTED || state == ConnectivityState.CONNECTED_SLOW
    }
}
