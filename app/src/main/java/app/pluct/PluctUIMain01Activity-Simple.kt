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
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController
import app.pluct.ui.navigation.PluctNavigationSimple
import app.pluct.ui.theme.PluctTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * PluctUIMain01Activity-Simple - Simplified main activity
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation][CoreResponsibility]
 * Minimal working version to get the app building
 */
@AndroidEntryPoint
class PluctUIMain01ActivitySimple : ComponentActivity() {

    private var navController: NavController? = null

    fun setNavController(controller: NavController) {
        this.navController = controller
        Log.d("PluctUIMain01ActivitySimple", "NavController set in MainActivity")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("PluctUIMain01ActivitySimple", "onCreate called")

        // Handle initial intent if app is launched via deep link or share
        handleIntent(intent)

        setContent {
            PluctTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    PluctNavigationSimple(navController = navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("PluctUIMain01ActivitySimple", "onNewIntent called with action: ${intent?.action}")
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            Log.d("PluctUIMain01ActivitySimple", "Handling intent: ${it.action}")
            when (it.action) {
                Intent.ACTION_SEND -> {
                    val sharedUrl = it.getStringExtra(Intent.EXTRA_TEXT)?.trim()
                    if (!sharedUrl.isNullOrEmpty()) {
                        Log.d("PluctUIMain01ActivitySimple", "Received ACTION_SEND with URL: $sharedUrl")
                        // TODO: Handle shared URL
                    } else {
                        Log.d("PluctUIMain01ActivitySimple", "No URL found in ACTION_SEND")
                    }
                }
                Intent.ACTION_VIEW -> {
                    it.data?.let { uri ->
                        Log.d("PluctUIMain01ActivitySimple", "Received ACTION_VIEW with URI: $uri")
                        if (uri.scheme == "pluct" && uri.host == "debug" && uri.pathSegments.contains("error")) {
                            Log.d("PluctUIMain01ActivitySimple", "Debug error deep link received")
                            // TODO: Handle debug error
                        }
                    }
                }
                else -> {
                    Log.d("PluctUIMain01ActivitySimple", "Unknown intent action: ${it.action}")
                }
            }
        }
    }
}
