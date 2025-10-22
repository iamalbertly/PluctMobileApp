package app.pluct

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController
import app.pluct.ui.navigation.PluctNavigation
import app.pluct.ui.theme.PluctTheme
import app.pluct.ui.error.ErrorCenter
import app.pluct.ui.error.ErrorBannerHost
import app.pluct.core.error.ErrorEnvelope
import app.pluct.ui.config.ConfigBannerHost
import app.pluct.viewmodel.CaptureRequest
import app.pluct.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pluct-UI-Main-01Activity - Main activity with focused responsibilities
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Simplified and focused on core activity functionality
 */
@AndroidEntryPoint
class PluctUIMain01Activity : ComponentActivity() {
    
    @Inject lateinit var errorCenter: ErrorCenter
    
    private var homeViewModel: HomeViewModel? = null
    private var navController: NavController? = null
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "🚀 MainActivity onCreate")
        
        setContent {
            PluctTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    this@PluctUIMain01Activity.navController = navController
                    
                    // Error banner system - must be rendered at the top level
                    ErrorBannerHost(center = errorCenter)

                    // Config banner for degraded engine status
                    ConfigBannerHost(
                        isDegraded = false, // TODO: Check engine health status
                        message = "Engine status degraded - some features may be limited",
                        onDismiss = { /* TODO: Handle dismiss */ }
                    )

                    PluctNavigation(navController = navController)
                }
            }
        }
        
        // Handle intent data
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "📱 onNewIntent: ${intent?.action}")
        
        intent?.let { handleIntent(it) }
    }
    
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            "app.pluct.action.CAPTURE_INSIGHT" -> {
                val url = intent.getStringExtra("url") ?: ""
                val caption = intent.getStringExtra("caption")
                Log.d(TAG, "🎯 CAPTURE_INSIGHT intent: url=$url, caption=$caption")
                
                homeViewModel?.let { viewModel ->
                    lifecycleScope.launch {
                        viewModel.setCaptureRequest(CaptureRequest(url, caption))
                    }
                }
            }
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrEmpty()) {
                    Log.d(TAG, "📤 Shared text received: $sharedText")
                    homeViewModel?.updateVideoUrl(sharedText)
                }
            }
            else -> {
                // Handle debug deep links
                val data = intent.data
                if (data?.scheme == "pluct" && data.host == "debug") {
                    when (data.path) {
                        "/error" -> {
                            val code = data.getQueryParameter("code") ?: "TEST_ERROR"
                            val message = data.getQueryParameter("msg") ?: "Test error message"
                            Log.d(TAG, "🔴 Debug error triggered: $code - $message")
                            
                            lifecycleScope.launch {
                                errorCenter.emit(ErrorEnvelope(
                                    code = code,
                                    message = message,
                                    source = "debug_deep_link"
                                ))
                            }
                        }
                    }
                }
            }
        }
    }
    
    fun setHomeViewModel(viewModel: HomeViewModel) {
        this.homeViewModel = viewModel
        Log.d(TAG, "✅ HomeViewModel registered")
    }
    
    fun setNavController(navController: NavController) {
        this.navController = navController
    }
}
