package app.pluct

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.pluct.core.error.ErrorEnvelope
import app.pluct.data.entity.VideoItem
import app.pluct.ui.error.PluctErrorBanner
import app.pluct.ui.screens.PluctHomeScreen
import app.pluct.ui.screens.PluctCaptureSheet
import app.pluct.ui.theme.PluctTheme
import app.pluct.viewmodel.PluctHomeViewModel

/**
 * Pluct-Main-01Activity - Full-featured main activity with proper state management
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            PluctTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PluctMainContent()
                }
            }
        }
    }
}

@Composable
fun PluctMainContent(
    viewModel: PluctHomeViewModel = viewModel()
) {
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val currentError by viewModel.currentError.collectAsStateWithLifecycle()
    
    var showCaptureSheet by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        PluctHomeScreen(
            videos = videos,
            onCaptureClick = { showCaptureSheet = true },
            onVideoClick = { /* Handle video click */ }
        )
        
        PluctCaptureSheet(
            isVisible = showCaptureSheet,
            onDismiss = { showCaptureSheet = false },
            onUrlSubmit = { url ->
                android.util.Log.d("MainActivity", "onUrlSubmit called with url: $url")
                showCaptureSheet = false
                android.util.Log.d("MainActivity", "Calling viewModel.addVideo")
                viewModel.addVideo(url)
                android.util.Log.d("MainActivity", "viewModel.addVideo called")
            }
        )
        
        // Error banner
        PluctErrorBanner(
            error = currentError,
            onDismiss = { viewModel.dismissError() }
        )
    }
}
