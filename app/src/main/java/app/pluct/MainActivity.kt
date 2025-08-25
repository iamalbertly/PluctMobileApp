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
import app.pluct.utils.VerificationResult
import app.pluct.utils.VerificationUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Run verification as a bonus step
        runVerification()
        
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
                            Log.d(TAG, "Handling initial deep link: ${initialIntent.data}")
                            navController.handleDeepLink(initialIntent)
                        }
                    }
                    
                    PluctNavigation(navController = navController)
                }
            }
        }
    }
    
    /**
     * Run verification as a bonus step
     */
    private fun runVerification() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Starting app verification")
                when (val result = VerificationUtils.verifyAppFunctionality(applicationContext)) {
                    is VerificationResult.Success -> {
                        Log.d(TAG, "App verification successful in ${result.durationMs}ms")
                        Toast.makeText(
                            this@MainActivity,
                            "App verification successful in ${result.durationMs}ms",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is VerificationResult.Failure -> {
                        Log.e(TAG, "App verification failed: ${result.message}")
                        Toast.makeText(
                            this@MainActivity,
                            "App verification failed: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during verification: ${e.message}", e)
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
        
        // Check for duplicate launches
        val prefs = getSharedPreferences("app.pluct.prefs", Context.MODE_PRIVATE)
        val lastLaunchTime = prefs.getLong("last_launch_timestamp", 0)
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastLaunchTime < 1000) { // Within 1 second
            Log.w(TAG, "Detected duplicate launch within 1 second, ignoring")
            return
        }
        
        // Update the timestamp
        prefs.edit().putLong("last_launch_timestamp", currentTime).apply()
        
        // Set the new intent
        setIntent(intent)
        
        // Handle subsequent deep links
        intent?.let { newIntent ->
            android.util.Log.d("MainActivity", "Received new intent: ${newIntent.action}, data: ${newIntent.data}")
            
            // Use the stored NavController reference
            navControllerRef?.let { navController ->
                android.util.Log.d("MainActivity", "Handling deep link with NavController")
                navController.handleDeepLink(newIntent)
            } ?: run {
                android.util.Log.w("MainActivity", "NavController not available yet, will handle deep link when UI is ready")
                // The deep link will be handled when the UI is ready via LaunchedEffect
            }
        }
    }
}
