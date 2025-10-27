package app.pluct

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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            PluctTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PluctMainContent(apiService, userIdentification)
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
    userIdentification: PluctCoreUserIdentification
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State management
    var creditBalance by remember { mutableStateOf(0) }
    var freeUsesRemaining by remember { mutableStateOf(3) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    
    // User preferences
    val userPreferences = remember { PluctUserPreferences(context) }
    
    // Load initial data
    LaunchedEffect(Unit) {
        loadInitialData(apiService, userIdentification) { balance, freeUses ->
            creditBalance = balance
            freeUsesRemaining = freeUses
        }
    }
    
    // Handle video processing
    val onTierSubmit: (String, ProcessingTier) -> Unit = { url: String, tier: ProcessingTier ->
        scope.launch {
            processVideo(apiService, url, tier, creditBalance, freeUsesRemaining) { success, newBalance, newFreeUses ->
                if (success) {
                    creditBalance = newBalance
                    freeUsesRemaining = newFreeUses
                    // Add video to list
                    val newVideo = VideoItem(
                        id = System.currentTimeMillis().toString(),
                        url = url,
                        title = "Processing...",
                        thumbnailUrl = "",
                        author = "",
                        duration = 0L,
                        status = ProcessingStatus.PROCESSING,
                        progress = 0,
                        transcript = "",
                        timestamp = System.currentTimeMillis(),
                        tier = tier,
                        createdAt = System.currentTimeMillis()
                    )
                    videos = videos + newVideo
                }
            }
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
        onRetryVideo = { /* TODO: Implement retry logic */ },
        onDeleteVideo = { /* TODO: Implement delete logic */ }
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
        val balanceResult = apiService.getCreditBalance()
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
        
        // Start transcription
        val transcriptionResult = apiService.startTranscription(url)
        transcriptionResult.fold(
            onSuccess = { transcription ->
                Log.d("MainActivity", "Transcription started: ${transcription.jobId}")
                
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
                Log.e("MainActivity", "Failed to start transcription: ${error.message}")
                onResult(false, currentBalance, currentFreeUses)
            }
        )
    } catch (e: Exception) {
        Log.e("MainActivity", "Error processing video: ${e.message}")
        onResult(false, currentBalance, currentFreeUses)
    }
}