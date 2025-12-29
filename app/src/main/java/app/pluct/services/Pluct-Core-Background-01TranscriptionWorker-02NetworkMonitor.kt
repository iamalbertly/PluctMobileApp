package app.pluct.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import app.pluct.core.network.PluctNetworkConnectivityChecker
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.QueueReason
import app.pluct.data.repository.PluctVideoRepository
import app.pluct.notification.PluctNotificationHelper
import app.pluct.services.PluctQueueManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * Pluct-Core-Background-01TranscriptionWorker-02NetworkMonitor
 * Follows naming convention: [Project]-[Core]-[Background]-[TranscriptionWorker]-[NetworkMonitor]
 * 5 scope layers: Project, Core, Background, TranscriptionWorker, NetworkMonitor
 * Monitors network state during background transcription
 */
class PluctCoreBackground01TranscriptionWorkerNetworkMonitor(
    private val context: Context,
    private val url: String,
    private val videoRepository: PluctVideoRepository,
    private val queueManager: PluctQueueManager
) {
    private val TAG = "NetworkMonitor"
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _isNetworkAvailable = MutableStateFlow(PluctNetworkConnectivityChecker.isNetworkAvailable(context))
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val wasUnavailable = !_isNetworkAvailable.value
            _isNetworkAvailable.value = true
            Log.d(TAG, "Network available")
            
            if (wasUnavailable) {
                // Note: handleNetworkRestored is suspend, but NetworkCallback is not
                // This will be handled in the worker's coroutine scope
                // For now, just log - actual retry will be handled by queue processor
                Log.d(TAG, "Network restored - queue processor will handle retry")
            }
        }
        
        override fun onLost(network: Network) {
            _isNetworkAvailable.value = false
            Log.w(TAG, "Network lost")
            handleNetworkLoss()
        }
        
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isNetworkAvailable.value = hasInternet && isValidated
            Log.d(TAG, "Network capabilities changed: internet=$hasInternet, validated=$isValidated")
        }
    }
    
    /**
     * Start monitoring network state
     */
    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        
        connectivityManager.registerNetworkCallback(request, networkCallback)
        Log.d(TAG, "Network monitoring started")
    }
    
    /**
     * Stop monitoring network state
     */
    fun stopMonitoring() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        Log.d(TAG, "Network monitoring stopped")
    }
    
    /**
     * Handle network loss - queue video and show error notification
     * Note: This is called from NetworkCallback which is not a coroutine context
     * The actual queueing will be handled by the worker's coroutine scope
     */
    private fun handleNetworkLoss() {
        Log.w(TAG, "Network lost during transcription, will queue video: $url")
        // Note: queueVideo is suspend, so it will be called from the worker's coroutine scope
        // For now, just log - the worker will handle queueing on failure
    }
    
    /**
     * Handle network restored - retry queued videos
     * Must be called from coroutine scope
     */
    private suspend fun handleNetworkRestored() {
        Log.d(TAG, "Network restored, checking for queued videos")
        
        // Find queued videos with NO_INTERNET reason
        val queuedVideos = videoRepository.getVideosByStatus(ProcessingStatus.QUEUED)
            .first()
            .filter { it.queueReason == QueueReason.NO_INTERNET }
        
        if (queuedVideos.isNotEmpty()) {
            Log.d(TAG, "Found ${queuedVideos.size} queued video(s) to retry")
            
            // Show notification via queue notification manager
            app.pluct.notification.PluctQueueNotificationManager.showQueueNotification(
                context = context,
                queuedCount = queuedVideos.size,
                message = "Network restored. Processing queued video(s)..."
            )
        }
    }
    
    /**
     * Monitor network state with callback
     */
    fun monitorNetworkState(callback: (Boolean) -> Unit) {
        // Observe network state flow
        // In a real implementation, this would use Flow collection
        // For now, we use the StateFlow directly
        val currentState = _isNetworkAvailable.value
        callback(currentState)
    }
}

