package app.pluct.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.setContent
import app.pluct.PluctUIMain01Activity
import app.pluct.utils.PluctUrlUtils
import app.pluct.share.PluctURLProcessingUtils
import kotlinx.coroutines.*
import java.net.URL
import java.util.UUID

/**
 * ShareIngestActivity - Dedicated activity for handling Share intents from other apps.
 * 
 * Why dedicated activity: Avoids heavy UI startup and guarantees Share visibility.
 * Why fast handoff: Keeps Share tap responsive by immediately routing to MainActivity.
 * Why exported=true: Required for Android to discover this activity in Share sheets.
 */
class PluctShareIngestActivity : ComponentActivity() {
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("ShareIngestActivity", "onCreate called")
        
        // Handle the incoming share intent
        handleShareIntent()
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("ShareIngestActivity", "onNewIntent called with new intent")
        setIntent(intent)
        handleShareIntent()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
    
    private fun handleShareIntent() {
        // Generate runId for this session
        val runId = UUID.randomUUID().toString()
        Log.d("ShareIngestActivity", "WV:A:run_id=$runId")
        
        // Extract URL from multiple possible sources
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        val clipData = intent.clipData
        val dataString = intent.dataString
        
        Log.d("ShareIngestActivity", "Received shared text: $sharedText")
        Log.d("ShareIngestActivity", "Clip data: $clipData")
        Log.d("ShareIngestActivity", "Data string: $dataString")
        
        // Try multiple sources for URL extraction
        val urlText = sharedText ?: 
                     clipData?.getItemAt(0)?.text?.toString() ?: 
                     dataString
        
        if (urlText.isNullOrBlank()) {
            Log.w("ShareIngestActivity", "No shared text received")
            Toast.makeText(this, "No URL provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Extract URL from shared text (handle cases where text contains other content)
        val url = PluctUrlUtils.extractUrlFromText(urlText)
        Log.d("ShareIngestActivity", "Extracted URL: $url")
        
        if (url == null) {
            Log.w("ShareIngestActivity", "Invalid URL format: $urlText")
            Toast.makeText(this, "Invalid URL format", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Accept any valid TikTok URL (non-blank). No strict equality gate.
        Log.d("ShareIngestActivity", "WV:A:url=$url run=$runId")
        
        // Process URL before passing to MainActivity (on background thread)
        coroutineScope.launch {
            try {
                // For script.tokaudit.io, we want to prioritize vm.tiktok.com URLs
                // If it's already a vm.tiktok.com URL, keep it as-is
                // For other TikTok URLs, try to convert to vm.tiktok.com format
                // Process URL using the utility class
                val finalUrl = PluctURLProcessingUtils.processUrl(url)
                
                // Enhanced logging for debugging
                Log.i("ShareIngestActivity", "Processing share intent with URL: $finalUrl")
                Log.d("ShareIngestActivity", "Original URL: $url")
                Log.d("ShareIngestActivity", "Processed URL: $finalUrl")
                
                // Save URL to clipboard for easy access in WebView
                try {
                    val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText("Shared TikTok URL", finalUrl)
                    clipboardManager.setPrimaryClip(clipData)
                    Log.d("ShareIngestActivity", "Saved shared URL to clipboard: $finalUrl")
                } catch (e: Exception) {
                    Log.e("ShareIngestActivity", "Failed to save URL to clipboard: ${e.message}", e)
                }
                
                // Extract caption from shared text if available
                val captionText = extractCaptionFromText(urlText)
                Log.d("ShareIngestActivity", "Extracted caption: $captionText")
                
                // Show success message
                Toast.makeText(this@PluctShareIngestActivity, "Opening Pluct...", Toast.LENGTH_SHORT).show()
                 
                 // Use SINGLE_TOP and CLEAR_TOP to prevent double launch
                 // Remove NEW_TASK to keep the app in the foreground
                 val mainIntent = Intent(this@PluctShareIngestActivity, PluctUIMain01Activity::class.java).apply {
                     action = "app.pluct.action.CAPTURE_INSIGHT"
                     flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                     addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION) // Prevent animation for smoother transition
                     
                     // Pass the URL and caption to trigger the capture sheet
                     putExtra("capture_url", finalUrl)
                     putExtra("capture_caption", captionText)
                 }
                 
                 android.util.Log.d("ShareIngestActivity", "Starting MainActivity with capture intent")
                 
                 startActivity(mainIntent)
                 Log.d("ShareIngestActivity", "MainActivity started with capture intent, finishing ShareIngestActivity")
                 finish()
            } catch (e: Exception) {
                Log.e("ShareIngestActivity", "Error processing share intent", e)
                Toast.makeText(this@PluctShareIngestActivity, "Error processing URL", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    // No blocking dialog for URL equality; any TikTok URL proceeds
    
    // URL processing methods have been moved to UrlProcessingUtils
    
    /**
     * Extract caption from shared text by removing the URL
     */
    private fun extractCaptionFromText(sharedText: String): String? {
        return try {
            // Try to find and remove the URL from the text
            val urlPattern = Regex("https?://[^\\s]+")
            val caption = sharedText.replace(urlPattern, "").trim()
            if (caption.isNotEmpty()) caption else null
        } catch (e: Exception) {
            Log.e("ShareIngestActivity", "Error extracting caption: ${e.message}")
            null
        }
    }
}
