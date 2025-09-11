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
import app.pluct.MainActivity
import app.pluct.utils.UrlUtils
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
class ShareIngestActivity : ComponentActivity() {
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("ShareIngestActivity", "onCreate called")
        
        // Handle the incoming share intent
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
        val url = UrlUtils.extractUrlFromText(urlText)
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
                val finalUrl = UrlProcessingUtils.processUrl(url)
                
                // Save URL to clipboard for easy access in WebView
                try {
                    val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText("Shared TikTok URL", finalUrl)
                    clipboardManager.setPrimaryClip(clipData)
                    Log.d("ShareIngestActivity", "Saved shared URL to clipboard: $finalUrl")
                } catch (e: Exception) {
                    Log.e("ShareIngestActivity", "Failed to save URL to clipboard: ${e.message}", e)
                }
                
                // Start MainActivity with the final URL as a deep link parameter
                val encodedUrl = android.net.Uri.encode(finalUrl, "UTF-8")
                val deepLinkUri = "pluct://ingest?url=$encodedUrl&run_id=$runId"
                Log.d("ShareIngestActivity", "Starting MainActivity with deep link: $deepLinkUri")
                Log.d("ShareIngestActivity", "Original URL: $finalUrl, Encoded URL: $encodedUrl, Run ID: $runId")
                
                // Show success message
                Toast.makeText(this@ShareIngestActivity, "Saved link: ${UrlUtils.extractHostFromUrl(finalUrl)}", Toast.LENGTH_SHORT).show()
                 
                 // Use SINGLE_TOP and CLEAR_TOP to prevent double launch
                 val mainIntent = Intent(this@ShareIngestActivity, MainActivity::class.java).apply {
                     action = Intent.ACTION_VIEW
                     data = android.net.Uri.parse(deepLinkUri)
                     flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                     addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION) // Prevent animation for smoother transition
                     
                     // Persist both videoUrl and runId into intent extras
                     putExtra("tok_url", finalUrl)
                     putExtra("run_id", runId)
                 }
                 
                 android.util.Log.d("ShareIngestActivity", "Starting MainActivity with flags: ${mainIntent.flags}, deepLink: $deepLinkUri")
                 
                 // Set a flag in shared preferences to prevent duplicate launches
                 val prefs = getSharedPreferences("app.pluct.prefs", Context.MODE_PRIVATE)
                 prefs.edit().putLong("last_launch_timestamp", System.currentTimeMillis()).apply()
                 
                 startActivity(mainIntent)
                 Log.d("ShareIngestActivity", "MainActivity started with single top flag, finishing ShareIngestActivity")
                 finish()
            } catch (e: Exception) {
                Log.e("ShareIngestActivity", "Error processing share intent", e)
                Toast.makeText(this@ShareIngestActivity, "Error processing URL", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    // No blocking dialog for URL equality; any TikTok URL proceeds
    
    // URL processing methods have been moved to UrlProcessingUtils
}
