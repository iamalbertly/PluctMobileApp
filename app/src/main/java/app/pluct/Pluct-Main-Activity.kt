package app.pluct

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController
import app.pluct.ui.navigation.PluctNavigation
import app.pluct.ui.theme.PluctTheme
import app.pluct.utils.DebugLogger
import app.pluct.utils.VerificationResult
import app.pluct.utils.VerificationUtils
import app.pluct.data.manager.UserManager
import app.pluct.notification.PluctNotificationHelper
import app.pluct.config.AppConfig
import app.pluct.utils.BusinessEngineHealthChecker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MainActivity - Single activity design for stability on low-end hardware.
 * 
 * Why single activity: Reduces memory footprint, faster app startup,
 * and fewer lifecycle complications compared to multiple activities.
 * 
 * Why @AndroidEntryPoint: Enables Hilt dependency injection in this activity.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    
    @Inject
    lateinit var userManager: UserManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize app configuration
        AppConfig.initialize(this)
        
        // Initialize file logger
        DebugLogger.init(applicationContext)
        DebugLogger.clear()
        DebugLogger.log("MainActivity onCreate - intent: ${intent?.data}")
        
        // Initialize notification channel
        PluctNotificationHelper.createNotificationChannel(this)
        
        // Handle first-time user onboarding
        handleFirstTimeUser()
        
        // Run verification as a bonus step
        runVerification()
        // Run Business Engine preflight health check to ensure connectivity and emit stage logs
        runBusinessEnginePreflight()
        
        setContent {
            PluctTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Handle initial deep link after NavHost is set up
                    LaunchedEffect(Unit) {
                        // Store the NavController reference
                        setNavController(navController)
                        
                        // Handle the initial intent
                        intent?.let { initialIntent ->
                            DebugLogger.log("Handling deep link: ${initialIntent.data}")
                            handleSharedUrl(initialIntent)
                            navController.handleDeepLink(initialIntent)
                        }
                    }
                    
                    // Handle new intents when they arrive
                    LaunchedEffect(intent) {
                        intent?.let { currentIntent ->
                            DebugLogger.log("LaunchedEffect handling intent: ${currentIntent.data}")
                            handleSharedUrl(currentIntent)
                            navController.handleDeepLink(currentIntent)
                        }
                    }
                    
                    PluctNavigation(navController = navController)
                }
            }
        }
    }
    
    /**
     * Handle first-time user onboarding
     */
    private fun handleFirstTimeUser() {
        lifecycleScope.launch {
            try {
                if (userManager.isFirstTimeUser()) {
                    DebugLogger.log("First-time user detected, initializing onboarding")
                    
                    // Get or create user ID and register with business engine
                    val userId = userManager.getOrCreateUserId()
                    DebugLogger.log("User ID: $userId")
                    
                    // Mark user as no longer first-time
                    userManager.markUserAsReturning()
                    
                    DebugLogger.log("First-time user onboarding completed")
                } else {
                    DebugLogger.log("Returning user detected")
                }
            } catch (e: Exception) {
                DebugLogger.log("Error in first-time user handling: ${e.message}")
            }
        }
    }
    
    /**
     * Run verification as a bonus step
     */
    private fun runVerification() {
        lifecycleScope.launch {
            try {
                DebugLogger.log("Running app verification")
                when (val result = VerificationUtils.verifyAppFunctionality(applicationContext)) {
                    is VerificationResult.Success -> DebugLogger.log("Verification OK in ${result.durationMs}ms")
                    is VerificationResult.Failure -> DebugLogger.log("Verification FAIL: ${result.message}")
                }
            } catch (e: Exception) {
                DebugLogger.log("Verification exception: ${e.message}")
            }
        }
    }

    private fun runBusinessEnginePreflight() {
        lifecycleScope.launch {
            try {
                DebugLogger.log("Running Business Engine preflight health check")
                BusinessEngineHealthChecker.performFullHealthCheck()
            } catch (e: Exception) {
                DebugLogger.log("Business Engine preflight exception: ${e.message}")
            }
        }
    }
    
    // Store a reference to the NavController for use in onNewIntent
    private var navControllerRef: NavController? = null
    
    // Function to set the NavController reference from Compose
    fun setNavController(navController: NavController) {
        navControllerRef = navController
        android.util.Log.d("MainActivity", "NavController reference set")
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        
        android.util.Log.d(TAG, "onNewIntent called with intent: ${intent?.action}, data: ${intent?.data}")
        
        // Set the new intent
        setIntent(intent)
        
        intent?.let { newIntent ->
            android.util.Log.d("MainActivity", "Handling incoming intent: ${newIntent.action}, data: ${newIntent.data}")

            // Handle CAPTURE_INSIGHT action for the Choice Engine
            if (newIntent.action == "app.pluct.action.CAPTURE_INSIGHT") {
                android.util.Log.d("MainActivity", "Handling CAPTURE_INSIGHT intent")
                // The capture request will be handled by the HomeViewModel
                // No navigation needed here, just pass the data to the ViewModel
                return
            }

            // Handle shared URLs
            handleSharedUrl(newIntent)

            navControllerRef?.let { navController ->
                android.util.Log.d("MainActivity", "Handling deep link with NavController (no URL equality gate)")
                navController.handleDeepLink(newIntent)
            } ?: run {
                android.util.Log.w("MainActivity", "NavController not available yet, will handle deep link when UI is ready")
                // Will be handled by LaunchedEffect(intent)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        android.util.Log.d(TAG, "MainActivity onResume - ensuring app stays in foreground")
        
        // Ensure the app stays in foreground when resumed
        if (intent?.action == "app.pluct.action.CAPTURE_INSIGHT" || 
            intent?.getStringExtra(Intent.EXTRA_TEXT) != null) {
            android.util.Log.d(TAG, "MainActivity onResume - handling pending intent to keep app in foreground")
        }
    }
    
    /**
     * Handle shared URLs from Android Share Intent
     */
    private fun handleSharedUrl(intent: Intent) {
        val sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        if (!sharedUrl.isNullOrEmpty()) {
            DebugLogger.log("Shared URL received: $sharedUrl")
            // Store the shared URL for the ViewModel to pick up
            // This will be handled by the HomeScreen when it observes the intent
        }
    }
}
