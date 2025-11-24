package app.pluct

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.AndroidEntryPoint
import app.pluct.services.PluctCoreAPIUnifiedService
import app.pluct.services.PluctCoreUserIdentification
import javax.inject.Inject
import app.pluct.ui.theme.PluctTheme
import app.pluct.ui.screens.PluctHomeScreen
import app.pluct.data.entity.VideoItem
import app.pluct.data.entity.ProcessingStatus
import app.pluct.data.entity.ProcessingTier
import app.pluct.data.preferences.PluctUserPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Pluct-Main-01Activity - Simplified main activity for core UI testing
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@AndroidEntryPoint
class PluctUIScreen01MainActivity : ComponentActivity() {
    
    @Inject lateinit var apiService: PluctCoreAPIUnifiedService
    @Inject lateinit var userIdentification: PluctCoreUserIdentification
    @Inject lateinit var videoRepository: app.pluct.data.repository.PluctVideoRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle TikTok intent
        handleTikTokIntent(intent)
        
        setContent {
            PluctTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PluctMainContent(apiService, userIdentification, videoRepository)
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleTikTokIntent(it) }
    }
    
    private fun handleTikTokIntent(intent: Intent) {
        Log.d("MainActivity", "Handling intent: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                Log.d("MainActivity", "Received shared text: $sharedText")
                
                if (!sharedText.isNullOrEmpty() && sharedText.contains("tiktok.com")) {
                    Log.d("MainActivity", "TikTok URL detected: $sharedText")
                    // Store the URL for the UI to pick up
                    PluctUserPreferences.setPrefilledUrl(this, sharedText)
                }
            }
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                Log.d("MainActivity", "Received URI: $uri")
                
                if (uri?.scheme == "pluct") {
                    when (uri.host) {
                        "ingest" -> {
                            val url = uri.getQueryParameter("url")
                            Log.d("MainActivity", "Deep link URL: $url")
                            url?.let { PluctUserPreferences.setPrefilledUrl(this, it) }
                        }
                        "debug" -> {
                            Log.d("MainActivity", "Debug deep link received")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Main content composable - extracted for better separation of concerns
 */
@Composable
fun PluctMainContent(
    apiService: PluctCoreAPIUnifiedService,
    userIdentification: PluctCoreUserIdentification,
    videoRepository: app.pluct.data.repository.PluctVideoRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State management
    var creditBalance by remember { mutableStateOf(0) }
    var freeUsesRemaining by remember { mutableStateOf(3) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var prefilledUrl by remember { mutableStateOf<String?>(null) }
    
    // Load videos from database using Flow
    val videos by videoRepository.getAllVideos().collectAsState(initial = emptyList())
    
    // Load initial data and check for prefilled URL
    LaunchedEffect(Unit) {
        // Check for prefilled URL from intent
        val url = PluctUserPreferences.getAndClearPrefilledUrl(context)
        if (url != null) {
            Log.d("MainActivity", "Found prefilled URL: $url")
            prefilledUrl = url
        }
        
        loadInitialData(apiService, userIdentification) { balance, freeUses ->
            creditBalance = balance
            freeUsesRemaining = freeUses
        }
    }
    
    // Refresh credit balance function
    val refreshCreditBalance: () -> Unit = {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                loadInitialData(apiService, userIdentification) { balance, freeUses ->
                    creditBalance = balance
                    freeUsesRemaining = freeUses
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to refresh balance: ${e.message}", e)
                errorMessage = "Failed to refresh balance"
            } finally {
                isLoading = false
            }
        }
    }
    
    // Handle video processing
    val onTierSubmit: (String, ProcessingTier) -> Unit = { url: String, tier: ProcessingTier ->
        scope.launch {
            processVideo(apiService, url, tier, creditBalance, freeUsesRemaining, videoRepository) { success, newBalance, newFreeUses ->
                if (success) {
                    creditBalance = newBalance
                    freeUsesRemaining = newFreeUses
                }
            }
        }
    }
    
    // Handle video retry
    val onRetryVideo: (VideoItem) -> Unit = { video ->
        scope.launch {
            processVideo(apiService, video.url, video.tier, creditBalance, freeUsesRemaining, videoRepository) { success, newBalance, newFreeUses ->
                if (success) {
                    creditBalance = newBalance
                    freeUsesRemaining = newFreeUses
                }
            }
        }
    }
    
    // Handle video deletion
    val onDeleteVideo: (VideoItem) -> Unit = { video ->
        scope.launch {
            videoRepository.deleteVideo(video)
            Log.d("MainActivity", "Video deleted: ${video.id}")
        }
    }
    
    // Main UI
    PluctHomeScreen(
        creditBalance = creditBalance,
        freeUsesRemaining = freeUsesRemaining,
        videos = videos,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onTierSubmit = onTierSubmit,
        onRetryVideo = onRetryVideo,
        onDeleteVideo = onDeleteVideo,
        prefilledUrl = prefilledUrl,
        apiService = apiService,
        onRefreshCreditBalance = refreshCreditBalance
    )
}

/**
 * Load initial data from API
 */
private suspend fun loadInitialData(
    apiService: PluctCoreAPIUnifiedService,
    userIdentification: PluctCoreUserIdentification,
    onDataLoaded: (Int, Int) -> Unit
) {
    try {
        Log.d("MainActivity", "Loading initial data...")
        
        // Get credit balance
        val balanceResult = apiService.checkUserBalance()
        balanceResult.fold(
            onSuccess = { balance ->
                Log.d("MainActivity", "REAL credit balance loaded: ${balance.balance} for user: ${userIdentification.userId}")
                onDataLoaded(balance.balance, 3) // Default free uses
            },
            onFailure = { error ->
                Log.e("MainActivity", "Failed to load credit balance: ${error.message}")
                onDataLoaded(0, 3) // Fallback values
            }
        )
    } catch (e: Exception) {
        Log.e("MainActivity", "Error loading initial data: ${e.message}")
        onDataLoaded(0, 3) // Fallback values
    }
}

/**
 * Process video with the selected tier
 */
private suspend fun processVideo(
    apiService: PluctCoreAPIUnifiedService,
    url: String,
    tier: ProcessingTier,
    currentBalance: Int,
    currentFreeUses: Int,
    videoRepository: app.pluct.data.repository.PluctVideoRepository,
    onResult: (Boolean, Int, Int) -> Unit
) {
    try {
        Log.d("MainActivity", "Processing video: $url with tier: $tier")
        
        // Check if user has enough credits/free uses
        val hasEnoughCredits = when (tier) {
            ProcessingTier.EXTRACT_SCRIPT -> currentFreeUses > 0 || currentBalance >= 1
            ProcessingTier.GENERATE_INSIGHTS -> currentBalance >= 2
            else -> false // Other tiers not supported yet
        }
        
        if (!hasEnoughCredits) {
            Log.w("MainActivity", "Insufficient credits for tier: $tier")
            onResult(false, currentBalance, currentFreeUses)
            return
        }
        
        // Create video item and save to database before processing
        val videoId = System.currentTimeMillis().toString()
        val newVideo = VideoItem(
            id = videoId,
            url = url,
            title = "Processing...",
            thumbnailUrl = "",
            author = "",
            duration = 0L,
            status = ProcessingStatus.PROCESSING,
            progress = 0,
            transcript = null,
            timestamp = System.currentTimeMillis(),
            tier = tier,
            createdAt = System.currentTimeMillis()
        )
        videoRepository.insertVideo(newVideo)
        Log.d("MainActivity", "Video saved to database with id: $videoId")
        
        // Vend service token first
        val vendResult = apiService.vendToken()
        if (vendResult.isFailure) {
            val error = vendResult.exceptionOrNull()
            val errorMsg = error?.message ?: "Unknown error"
            Log.e("MainActivity", "Failed to vend token: $errorMsg")
            // Create detailed error string for debugging
            val errorDetails = """
                Service: BusinessEngine
                Operation: Vend Token
                Error: $errorMsg
                Timestamp: ${System.currentTimeMillis()}
                Stack: ${error?.stackTraceToString()?.take(500) ?: "N/A"}
            """.trimIndent()
            // Update video with failure
            videoRepository.updateVideo(newVideo.copy(
                status = ProcessingStatus.FAILED,
                failureReason = "Failed to vend token: $errorMsg",
                errorDetails = errorDetails
            ))
            onResult(false, currentBalance, currentFreeUses)
            return
        }
        val serviceToken = vendResult.getOrNull()?.token ?: run {
            Log.e("MainActivity", "Vend token returned null token")
            val errorDetails = """
                Service: BusinessEngine
                Operation: Vend Token
                Error: Token vending succeeded but returned null token
                Status Code: 200
                Expected: VendTokenResponse with non-null token
                Actual: VendTokenResponse with null token field
                Timestamp: ${System.currentTimeMillis()}
            """.trimIndent()
            videoRepository.updateVideo(newVideo.copy(
                status = ProcessingStatus.FAILED,
                failureReason = "Vend token returned null",
                errorDetails = errorDetails
            ))
            onResult(false, currentBalance, currentFreeUses)
            return
        }
        
        // Start transcription with proper token
        val transcriptionResult = apiService.submitTranscriptionJob(url, serviceToken)
        transcriptionResult.fold(
            onSuccess = { transcription ->
                Log.d("MainActivity", "Transcription started: ${transcription.jobId}")
                
                // Update video with job details
                videoRepository.updateVideo(newVideo.copy(
                    status = ProcessingStatus.PROCESSING,
                    transcript = "Job ID: ${transcription.jobId}"
                ))
                
                // Update credits based on tier
                val newBalance = when (tier) {
                    ProcessingTier.EXTRACT_SCRIPT -> if (currentFreeUses > 0) currentBalance else currentBalance - 1
                    ProcessingTier.GENERATE_INSIGHTS -> currentBalance - 2
                    else -> currentBalance // Other tiers not supported yet
                }
                val newFreeUses = if (tier == ProcessingTier.EXTRACT_SCRIPT && currentFreeUses > 0) currentFreeUses - 1 else currentFreeUses
                
                onResult(true, newBalance, newFreeUses)
            },
            onFailure = { error ->
                val errorMsg = error.message ?: "Unknown error"
                Log.e("MainActivity", "Failed to start transcription: $errorMsg")
                // Create detailed error string
                val errorDetails = "Service: TTTranscribe\nOperation: Submit Transcription Job\nError: $errorMsg\nTimestamp: ${System.currentTimeMillis()}\nStack: ${error.stackTraceToString().take(500)}"
                // Update video with failure
                videoRepository.updateVideo(newVideo.copy(
                    status = ProcessingStatus.FAILED,
                    failureReason = "Transcription failed: $errorMsg",
                    errorDetails = errorDetails
                ))
                onResult(false, currentBalance, currentFreeUses)
            }
        )
    } catch (e: Exception) {
        Log.e("MainActivity", "Error processing video: ${e.message}")
        onResult(false, currentBalance, currentFreeUses)
    }
}