package app.pluct.scraper

import android.util.Log
import android.webkit.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * WebView-based scraper for TikTok videos (Quick Scan tier)
 * This provides a fragile but free alternative to the TTTranscribe API
 */
class PluctWebViewScraper {
    companion object {
        private const val TAG = "WebViewScraper"
    }
    
    /**
     * Scrape transcript from TikTok video using WebView
     * @param videoUrl The TikTok video URL to scrape
     * @param webView The WebView instance to use for scraping
     * @return ScrapingResult with transcript or error
     */
    suspend fun scrapeTranscript(videoUrl: String, webView: WebView): ScrapingResult {
        return suspendCancellableCoroutine { continuation ->
            try {
                Log.i(TAG, "Starting WebView scraping for: $videoUrl")
                
                // Set up WebView settings
                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                }
                
                // Set up WebViewClient to handle page loading
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d(TAG, "Page finished loading: $url")
                        
                        // Execute JavaScript to extract transcript
                        view?.evaluateJavascript(extractTranscriptScript()) { result ->
                            Log.d(TAG, "JavaScript execution result: $result")
                            
                            try {
                                val transcript = parseJavaScriptResult(result)
                                if (transcript.isNotEmpty()) {
                                    Log.i(TAG, "Successfully scraped transcript: ${transcript.length} characters")
                                    continuation.resume(ScrapingResult.Success(transcript))
                                } else {
                                    Log.w(TAG, "No transcript found in page")
                                    continuation.resume(ScrapingResult.Error("No transcript found"))
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing JavaScript result", e)
                                continuation.resume(ScrapingResult.Error("Failed to parse result: ${e.message}"))
                            }
                        }
                    }
                    
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        Log.e(TAG, "WebView error: ${error?.description}")
                        continuation.resume(ScrapingResult.Error("WebView error: ${error?.description}"))
                    }
                }
                
                // Load the TikTok URL
                webView.loadUrl(videoUrl)
                
                // Set up timeout
                webView.postDelayed({
                    if (continuation.isActive) {
                        Log.w(TAG, "Scraping timeout reached")
                        continuation.resume(ScrapingResult.Error("Scraping timeout"))
                    }
                }, 30000) // 30 second timeout
                
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up WebView scraping", e)
                continuation.resume(ScrapingResult.Error("Setup error: ${e.message}"))
            }
        }
    }
    
    /**
     * JavaScript code to extract transcript from TikTok page
     */
    private fun extractTranscriptScript(): String {
        return """
            (function() {
                try {
                    // Try to find transcript in various possible locations
                    var transcript = '';
                    
                    // Method 1: Look for transcript in video metadata
                    var videoElement = document.querySelector('video');
                    if (videoElement && videoElement.textTracks) {
                        for (var i = 0; i < videoElement.textTracks.length; i++) {
                            var track = videoElement.textTracks[i];
                            if (track.kind === 'captions' || track.kind === 'subtitles') {
                                for (var j = 0; j < track.cues.length; j++) {
                                    transcript += track.cues[j].text + ' ';
                                }
                            }
                        }
                    }
                    
                    // Method 2: Look for transcript in page data
                    if (!transcript) {
                        var scripts = document.querySelectorAll('script');
                        for (var i = 0; i < scripts.length; i++) {
                            var scriptContent = scripts[i].textContent;
                            if (scriptContent && scriptContent.includes('transcript')) {
                                try {
                                    var data = JSON.parse(scriptContent);
                                    if (data.transcript) {
                                        transcript = data.transcript;
                                        break;
                                    }
                                } catch (e) {
                                    // Try to extract transcript using regex
                                    var match = scriptContent.match(/"transcript":\s*"([^"]+)"/);
                                    if (match) {
                                        transcript = match[1];
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    
                    // Method 3: Look for captions in video container
                    if (!transcript) {
                        var captionElements = document.querySelectorAll('[data-e2e="video-desc"], .video-desc, .caption');
                        for (var i = 0; i < captionElements.length; i++) {
                            transcript += captionElements[i].textContent + ' ';
                        }
                    }
                    
                    // Method 4: Look for any text content that might be transcript
                    if (!transcript) {
                        var textElements = document.querySelectorAll('p, span, div');
                        for (var i = 0; i < textElements.length; i++) {
                            var text = textElements[i].textContent;
                            if (text && text.length > 50 && text.length < 1000) {
                                transcript += text + ' ';
                            }
                        }
                    }
                    
                    return transcript.trim();
                } catch (e) {
                    return 'Error: ' + e.message;
                }
            })();
        """.trimIndent()
    }
    
    /**
     * Parse JavaScript execution result
     */
    private fun parseJavaScriptResult(result: String): String {
        return try {
            // Remove quotes and escape characters
            result.removeSurrounding("\"").replace("\\\"", "\"").replace("\\n", "\n").trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JavaScript result", e)
            ""
        }
    }
}

/**
 * Result of WebView scraping
 */
sealed class ScrapingResult {
    data class Success(val transcript: String) : ScrapingResult()
    data class Error(val message: String) : ScrapingResult()
}
