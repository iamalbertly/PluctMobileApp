package app.pluct.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import app.pluct.core.network.PluctNetworkConnectivityChecker
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.entity.QueueReason
import app.pluct.data.repository.PluctVideoRepository
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
    
    // UX IMPROVEMENT #3: Track if network loss was detected to enable proactive queueing
    @Volatile
    private var networkLossDetected = false
    
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
            networkLossDetected = true
            Log.w(TAG, "Network lost during transcription")
            // Note: Actual queueing will be handled in worker's coroutine scope via checkAndQueueOnNetworkLoss()
        }
        
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val isAvailable = hasInternet && isValidated
            _isNetworkAvailable.value = isAvailable
            if (!isAvailable) {
                networkLossDetected = true
            }
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

    fun markNetworkLossDetected() {
        networkLossDetected = true
        _isNetworkAvailable.value = false
        Log.w(TAG, "Network loss marked by worker")
    }
    
    /**
     * UX IMPROVEMENT #3: Check if network loss was detected and queue video proactively
     * Must be called from coroutine scope
     * Technical Debt #2: Properly handles network loss queueing in coroutine context
     */
    suspend fun checkAndQueueOnNetworkLoss(): Boolean {
        val currentlyUnavailable = !_isNetworkAvailable.value ||
            !PluctNetworkConnectivityChecker.isNetworkAvailable(context)

        if (!currentlyUnavailable || !networkLossDetected) {
            return false
        }

        Log.w(TAG, "Network loss detected, queueing video: $url")

        val existingVideo = videoRepository.getVideoByUrl(url)
        if (existingVideo == null) {
            val queueResult = queueManager.queueVideo(
                url = url,
                tier = ProcessingTier.EXTRACT_SCRIPT,
                reason = QueueReason.NO_INTERNET
            )
            if (queueResult.isSuccess) {
                Log.d(TAG, "Video queued successfully due to network loss")
                networkLossDetected = false
                return true
            }
            Log.e(TAG, "Failed to queue missing video on network loss: ${queueResult.exceptionOrNull()?.message}")
            return false
        }

        if (existingVideo.status == ProcessingStatus.COMPLETED) {
            Log.d(TAG, "Video completed before network loss queue was needed")
            networkLossDetected = false
            return true
        }

        if (existingVideo.status == ProcessingStatus.QUEUED) {
            Log.d(TAG, "Video already queued safely")
            networkLossDetected = false
            return true
        }

        if (existingVideo.status == ProcessingStatus.PROCESSING || existingVideo.status == ProcessingStatus.FAILED) {
            val queuedVideo = existingVideo.copy(
                status = ProcessingStatus.QUEUED,
                queueReason = QueueReason.NO_INTERNET,
                queuedAt = System.currentTimeMillis(),
                failureReason = "Network lost. Waiting to retry."
            )
            val updateResult = videoRepository.updateVideo(queuedVideo)
            if (updateResult.isSuccess) {
                Log.d(TAG, "Video queued successfully due to network loss")
                networkLossDetected = false
                return true
            }
            Log.e(TAG, "Failed to update video queue state on network loss: ${updateResult.exceptionOrNull()?.message}")
        }

        return false
    }
    
    /**
     * Check if network is currently available
     */
    fun isNetworkCurrentlyAvailable(): Boolean {
        return _isNetworkAvailable.value
    }
    
    /**
     * Handle network restored - retry queued videos
     * Must be called from coroutine scope
     * UX FIX #1: Wired to queue processor - triggers immediate processing of queued videos
     */
    suspend fun handleNetworkRestored() {
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
            
            // UX FIX #1: Trigger queue processor to process videos immediately
            PluctQueueProcessorWorker.triggerImmediateProcessing(context)
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

