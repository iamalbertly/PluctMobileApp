package app.pluct.ui.utils

import android.content.Context
import android.util.Log
import app.pluct.utils.NetworkUtils
import app.pluct.utils.Constants

/**
 * Handles network connectivity checks and quality assessment for the ingest process
 */
object NetworkHandler {
    
    /**
     * Enhanced network connectivity check with quality assessment
     */
    fun checkNetworkConnectivity(context: Context): NetworkCheckResult {
        val connectivityState = NetworkUtils.getConnectivityState(context)
        
        return when (connectivityState) {
            NetworkUtils.ConnectivityState.DISCONNECTED -> {
                Log.w("PluctIngest", "No internet connection available")
                NetworkCheckResult(
                    isConnected = false,
                    showNetworkStatus = true,
                    networkQuality = NetworkUtils.NetworkQuality.NONE
                )
            }
            NetworkUtils.ConnectivityState.CONNECTED_UNRELIABLE -> {
                Log.w("PluctIngest", "Internet connection is unreliable")
                NetworkCheckResult(
                    isConnected = false,
                    showNetworkStatus = true,
                    networkQuality = NetworkUtils.NetworkQuality.UNRELIABLE
                )
            }
            NetworkUtils.ConnectivityState.CONNECTED_SLOW -> {
                Log.i("PluctIngest", "Internet connection is slow but available")
                NetworkCheckResult(
                    isConnected = true,
                    showNetworkStatus = true,
                    networkQuality = NetworkUtils.NetworkQuality.POOR
                )
            }
            NetworkUtils.ConnectivityState.CONNECTED -> {
                Log.d("PluctIngest", "Internet connection is good")
                NetworkCheckResult(
                    isConnected = true,
                    showNetworkStatus = false,
                    networkQuality = NetworkUtils.NetworkQuality.GOOD
                )
            }
            NetworkUtils.ConnectivityState.UNKNOWN -> {
                Log.w("PluctIngest", "Internet connection status unknown")
                NetworkCheckResult(
                    isConnected = false,
                    showNetworkStatus = true,
                    networkQuality = NetworkUtils.NetworkQuality.UNRELIABLE
                )
            }
        }
    }
    
    /**
     * Get adaptive timeout based on network quality
     */
    fun getAdaptiveTimeout(networkQuality: NetworkUtils.NetworkQuality): Long {
        return when (networkQuality) {
            NetworkUtils.NetworkQuality.EXCELLENT -> 150000L // 2.5 minutes (increased for transcript processing)
            NetworkUtils.NetworkQuality.GOOD -> 180000L // 3 minutes (increased for transcript processing)
            NetworkUtils.NetworkQuality.POOR -> 240000L // 4 minutes
            NetworkUtils.NetworkQuality.UNRELIABLE -> 360000L // 6 minutes
            NetworkUtils.NetworkQuality.NONE -> 30000L // 30 seconds (fail fast)
        }
    }
}

data class NetworkCheckResult(
    val isConnected: Boolean,
    val showNetworkStatus: Boolean,
    val networkQuality: NetworkUtils.NetworkQuality
)
