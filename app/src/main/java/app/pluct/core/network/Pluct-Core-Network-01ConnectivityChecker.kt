package app.pluct.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build

/**
 * Pluct-Core-Network-01ConnectivityChecker
 * Singleton object for checking network connectivity status
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Sequence][Responsibility]
 */
object PluctNetworkConnectivityChecker {
    
    /**
     * Check if network is available and validated
     * Returns true if device has internet connectivity that is validated
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                   capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            // Fallback for older Android versions
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo?.isConnected == true && networkInfo.isAvailable
        }
    }
    
    /**
     * Check if network is connected (basic connectivity, no validation)
     * Returns true if device has any network connection
     */
    fun isNetworkConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo?.isConnected == true
        }
    }
    
    /**
     * Get network connection type
     * Returns WiFi, Mobile, or None
     */
    fun getNetworkType(context: Context): NetworkType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkType.None
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return NetworkType.None
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.None
            
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WiFi
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.Mobile
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.Ethernet
                else -> NetworkType.None
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return NetworkType.None
            return when (networkInfo.type) {
                ConnectivityManager.TYPE_WIFI -> NetworkType.WiFi
                ConnectivityManager.TYPE_MOBILE -> NetworkType.Mobile
                ConnectivityManager.TYPE_ETHERNET -> NetworkType.Ethernet
                else -> NetworkType.None
            }
        }
    }
}

/**
 * Network connection type enum
 */
enum class NetworkType {
    WiFi,
    Mobile,
    Ethernet,
    None
}



