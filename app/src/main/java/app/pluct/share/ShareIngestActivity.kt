package app.pluct.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import app.pluct.MainActivity
import kotlinx.coroutines.*
import java.net.URL

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
        
        // Handle the incoming share intent
        handleShareIntent()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
    
    private fun handleShareIntent() {
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
        val url = extractUrlFromText(urlText)
        Log.d("ShareIngestActivity", "Extracted URL: $url")
        
        if (url == null) {
            Log.w("ShareIngestActivity", "Invalid URL format: $urlText")
            Toast.makeText(this, "Invalid URL format", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Process URL before passing to MainActivity (on background thread)
        coroutineScope.launch {
            try {
                // For script.tokaudit.io, we want to prioritize vm.tiktok.com URLs
                // If it's already a vm.tiktok.com URL, keep it as-is
                // For other TikTok URLs, try to convert to vm.tiktok.com format
                // For non-TikTok URLs, canonicalize as usual
                val finalUrl = when {
                    url.contains("vm.tiktok.com") -> {
                        Log.d("ShareIngestActivity", "Keeping vm.tiktok.com URL as-is for script.tokaudit.io: $url")
                        url
                    }
                    url.contains("tiktok.com/@") -> {
                        // Try to convert to vm.tiktok.com format
                        try {
                            val match = Regex("tiktok\\.com/@[^/]+/video/(\\d+)").find(url)
                            if (match != null) {
                                val videoId = match.groupValues[1]
                                val vmUrl = "https://vm.tiktok.com/ZMA${videoId.substring(0, 6)}/"
                                Log.d("ShareIngestActivity", "Converted to vm.tiktok.com format: $vmUrl")
                                vmUrl
                            } else {
                                // If conversion fails, use canonicalized URL
                                val canonicalUrl = withContext(Dispatchers.IO) {
                                    canonicalizeUrl(url)
                                }
                                Log.d("ShareIngestActivity", "Could not convert to vm.tiktok.com, canonicalized: $canonicalUrl")
                                canonicalUrl
                            }
                        } catch (e: Exception) {
                            Log.e("ShareIngestActivity", "Error converting to vm.tiktok.com format", e)
                            // Fall back to canonicalization
                            val canonicalUrl = withContext(Dispatchers.IO) {
                                canonicalizeUrl(url)
                            }
                            Log.d("ShareIngestActivity", "Canonicalized URL after error: $canonicalUrl")
                            canonicalUrl
                        }
                    }
                    else -> {
                        val canonicalUrl = withContext(Dispatchers.IO) {
                            canonicalizeUrl(url)
                        }
                        Log.d("ShareIngestActivity", "Canonicalized non-TikTok URL: $canonicalUrl")
                        canonicalUrl
                    }
                }
                
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
                val deepLinkUri = "pluct://ingest?url=$encodedUrl"
                Log.d("ShareIngestActivity", "Starting MainActivity with deep link: $deepLinkUri")
                Log.d("ShareIngestActivity", "Original URL: $finalUrl, Encoded URL: $encodedUrl")
                
                // Show success message
                Toast.makeText(this@ShareIngestActivity, "Saved link: ${extractHostFromUrl(finalUrl)}", Toast.LENGTH_SHORT).show()
                 
                 // Use SINGLE_TOP and CLEAR_TOP to prevent double launch
                 val mainIntent = Intent(this@ShareIngestActivity, MainActivity::class.java).apply {
                     action = Intent.ACTION_VIEW
                     data = android.net.Uri.parse(deepLinkUri)
                     flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                     addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION) // Prevent animation for smoother transition
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
    
    private fun extractUrlFromText(text: String): String? {
        // Enhanced URL extraction - look for URLs in the text
        val urlPattern = Regex("https?://[^\\s]+")
        val match = urlPattern.find(text)
        
        return match?.value?.let { url ->
            try {
                // Validate URL format
                URL(url)
                
                // Check if it's a supported platform
                when {
                    url.contains("tiktok.com") || url.contains("vm.tiktok.com") -> {
                        Log.d("ShareIngestActivity", "Supported TikTok URL detected: $url")
                        url
                    }
                    url.contains("youtube.com") || url.contains("youtu.be") -> {
                        Log.d("ShareIngestActivity", "Supported YouTube URL detected: $url")
                        url
                    }
                    url.contains("instagram.com") -> {
                        Log.d("ShareIngestActivity", "Supported Instagram URL detected: $url")
                        url
                    }
                    url.contains("twitter.com") || url.contains("x.com") -> {
                        Log.d("ShareIngestActivity", "Supported Twitter/X URL detected: $url")
                        url
                    }
                    else -> {
                        Log.w("ShareIngestActivity", "Unsupported URL format detected: $url")
                        Log.w("ShareIngestActivity", "This URL format needs developer attention for future support")
                        // Still return the URL for now, but log it for future development
                        url
                    }
                }
            } catch (e: Exception) {
                Log.e("ShareIngestActivity", "Invalid URL format: $url", e)
                null
            }
        }
    }
    
    private fun canonicalizeUrl(url: String): String {
        return try {
            Log.d("ShareIngestActivity", "Starting URL canonicalization for: $url")
            
            // Try HEAD request first
            val connection = URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
            connection.setInstanceFollowRedirects(true)
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            
            val responseCode = connection.responseCode
            val finalUrl = connection.url.toString()
            
            Log.d("ShareIngestActivity", "HEAD response: $responseCode, final URL: $finalUrl")
            
            if (responseCode in 200..299) {
                Log.d("ShareIngestActivity", "Successfully canonicalized URL to: $finalUrl")
                finalUrl
            } else {
                // Fallback to GET if HEAD fails
                Log.d("ShareIngestActivity", "HEAD failed, trying GET")
                val getConnection = URL(url).openConnection() as java.net.HttpURLConnection
                getConnection.requestMethod = "GET"
                getConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                getConnection.setInstanceFollowRedirects(true)
                getConnection.connectTimeout = 3000
                getConnection.readTimeout = 3000
                
                val getResponseCode = getConnection.responseCode
                val getFinalUrl = getConnection.url.toString()
                
                Log.d("ShareIngestActivity", "GET response: $getResponseCode, final URL: $getFinalUrl")
                
                if (getResponseCode in 200..299) {
                    Log.d("ShareIngestActivity", "Successfully canonicalized URL to: $getFinalUrl")
                    getFinalUrl
                } else {
                    Log.w("ShareIngestActivity", "Both HEAD and GET failed, keeping original URL")
                    url // Keep original if both fail
                }
            }
        } catch (e: Exception) {
            Log.w("ShareIngestActivity", "URL canonicalization failed: ${e.message ?: "Unknown error"}", e)
            url // Keep original URL on error
        }
    }
    
    private fun extractHostFromUrl(url: String): String {
        return try {
            val uri = URL(url)
            uri.host
        } catch (e: Exception) {
            url
        }
    }
}
