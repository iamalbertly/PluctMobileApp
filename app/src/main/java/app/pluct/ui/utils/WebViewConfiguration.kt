package app.pluct.ui.utils

import android.net.Uri
import android.util.Log
import android.webkit.*
import android.net.http.SslError
import app.pluct.R

/**
 * Consolidated WebView configuration with SSL fixes and performance optimizations
 */
object WebViewConfiguration {
    private const val TAG = "WebViewConfiguration"
    
    /**
     * Configure WebView with SSL fixes and performance optimizations
     */
    fun configureWebView(webView: WebView, runId: String) {
        try {
            Log.d(TAG, "WV:A:webview_configured run=$runId")
            RunRingBuffer.addLog(runId, "INFO", "webview_configured", "WV")
            
            // Essential settings
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                setGeolocationEnabled(false)
                setRenderPriority(WebSettings.RenderPriority.HIGH)
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
            }
            
            // Configure cookies
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(webView, true)
            }
            
            // Set WebViewClient with SSL fixes
            webView.webViewClient = createWebViewClient(runId)
            
            // Set WebChromeClient for console logging
            webView.webChromeClient = createWebChromeClient()
            
            Log.d(TAG, "WV:A:webview_ssl_configured run=$runId")
            RunRingBuffer.addLog(runId, "INFO", "webview_ssl_configured", "WV")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring WebView: ${e.message}", e)
            RunRingBuffer.addLog(runId, "ERROR", "webview_config_error: ${e.message}", "WV")
        }
    }
    
    /**
     * Create WebViewClient with SSL fixes and script injection
     */
    private fun createWebViewClient(runId: String): WebViewClient {
        return object : WebViewClient() {
            private var alreadyInjected = false
            private fun dumpHtml(view: WebView?, label: String) {
                try {
                    view?.evaluateJavascript("(function(){try{return (document.body&&document.body.innerHTML)?document.body.innerHTML.slice(0,4000):'';}catch(e){return 'html_err:'+e.message}})();") { html ->
                        val safe = (html ?: "").replace('\n', ' ')
                        Log.d(TAG, "WV:A:html_dump label=" + label + " len=" + safe.length)
                        Log.d("WVConsole", "WVConsole:html_dump(" + label + ")=" + safe)
                    }
                } catch (_: Exception) {
                }
            }
            
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                alreadyInjected = false
                val host = try { Uri.parse(url ?: "").host.orEmpty().lowercase() } catch (_: Exception) { "" }
                Log.d(TAG, "WV:A:page_started url=$url host=$host run=$runId")
                RunRingBuffer.addLog(runId, "INFO", "page_started url=$url host=$host", "WV")
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val finishedHost = try { Uri.parse(url ?: "").host.orEmpty().lowercase() } catch (_: Exception) { "" }
                Log.d(TAG, "WV:A:page_finished url=$url host=$finishedHost run=$runId")
                RunRingBuffer.addLog(runId, "INFO", "page_finished url=$url host=$finishedHost", "WV")
                dumpHtml(view, "onPageFinished")
                
                // Inject automation script for TokAudit
                val isTokAudit = finishedHost.endsWith("tokaudit.io")
                val isGetTranscribe = finishedHost.contains("gettranscribe.ai")
                if ((isTokAudit || isGetTranscribe) &&
                    (view?.url != "about:blank") &&
                    !alreadyInjected) {
                    
                    view?.postDelayed({
                        try {
                            val videoUrl = view?.getTag(R.id.tag_video_url) as? String ?: ""
                            val scriptRunId = view?.getTag(R.id.tag_run_id) as? String ?: runId
                            
                            if (alreadyInjected) {
                                Log.d(TAG, "WV:A:skip_inject reason=delayed_already_injected url=$url run=$runId")
                                RunRingBuffer.addLog(runId, "INFO", "skip_inject reason=delayed_already_injected url=$url", "WV")
                                return@postDelayed
                            }

                            if (videoUrl.isNotEmpty()) {
                                view?.let { WebViewScripts.injectAutomationScript(it, videoUrl, scriptRunId) }
                                alreadyInjected = true
                                Log.d(TAG, "WV:A:inject_auto host=$finishedHost run=$scriptRunId")
                                RunRingBuffer.addLog(scriptRunId, "INFO", "inject_auto host=$finishedHost", "WV")
                            } else {
                                Log.e(TAG, "WV:A:fatal_blank_url run=$scriptRunId")
                                RunRingBuffer.addLog(scriptRunId, "ERROR", "fatal_blank_url", "WV")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error injecting script: ${e.message}", e)
                            RunRingBuffer.addLog(runId, "ERROR", "script_injection_error: ${e.message}", "WV")
                        }
                    }, 750) // 600-900ms delay
                } else {
                    val reason = when {
                        finishedHost.isEmpty() -> "no_host"
                        !(isTokAudit || isGetTranscribe) -> "unsupported_host=$finishedHost"
                        view?.url == "about:blank" -> "about_blank"
                        alreadyInjected -> "already_injected"
                        else -> "unknown"
                    }
                    Log.d(TAG, "WV:A:skip_inject reason=$reason url=$url run=$runId")
                    RunRingBuffer.addLog(runId, "INFO", "skip_inject reason=$reason url=$url", "WV")
                }
            }
            
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                val host = Uri.parse(error?.url ?: "").host.orEmpty().lowercase()
                val ok = host == "tokaudit.io" || host.endsWith(".tokaudit.io") ||
                    host == "gettranscribe.ai" || host.endsWith(".gettranscribe.ai")
                
                if (ok) {
                    handler?.proceed()
                    Log.d(TAG, "WV:A:ssl_proceed host=$host run=$runId")
                    RunRingBuffer.addLog(runId, "INFO", "ssl_proceed host=$host", "WV")
                } else {
                    handler?.cancel()
                    Log.d(TAG, "WV:A:ssl_cancel host=$host run=$runId")
                    RunRingBuffer.addLog(runId, "ERROR", "ssl_cancel host=$host", "WV")
                }
            }
            
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                Log.e(TAG, "WV:A:webview_error code=${error?.errorCode} desc=${error?.description} run=$runId")
                RunRingBuffer.addLog(runId, "ERROR", "webview_error code=${error?.errorCode}", "WV")
                dumpHtml(view, "onReceivedError")
            }
            
            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)
                Log.e(TAG, "WV:A:http_error code=${errorResponse?.statusCode} url=${request?.url} run=$runId")
                RunRingBuffer.addLog(runId, "ERROR", "http_error code=${errorResponse?.statusCode}", "WV")
                dumpHtml(view, "onReceivedHttpError")
            }
            
            override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                super.onRenderProcessGone(view, detail)
                Log.e(TAG, "WV:A:render_gone didCrash=${detail?.didCrash()} run=$runId")
                RunRingBuffer.addLog(runId, "ERROR", "render_gone didCrash=${detail?.didCrash()}", "WV")
                return true
            }
        }
    }
    
    /**
     * Create WebChromeClient for console logging
     */
    private fun createWebChromeClient(): WebChromeClient {
        return object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let { message ->
                    val level = message.messageLevel()
                    val raw = message.message()
                    val tag = when (level) {
                        ConsoleMessage.MessageLevel.ERROR -> "WVConsoleErr"
                        ConsoleMessage.MessageLevel.WARNING -> "WVConsoleWarn"
                        ConsoleMessage.MessageLevel.LOG -> "WVConsoleLog"
                        ConsoleMessage.MessageLevel.TIP -> "WVConsoleTip"
                        ConsoleMessage.MessageLevel.DEBUG -> "WVConsoleDbg"
                        else -> "WVConsole"
                    }
                    val logMessage = tag + ":" + raw
                    when (level) {
                        ConsoleMessage.MessageLevel.ERROR -> Log.e(tag, logMessage)
                        ConsoleMessage.MessageLevel.WARNING -> Log.w(tag, logMessage)
                        ConsoleMessage.MessageLevel.DEBUG -> Log.d(tag, logMessage)
                        else -> Log.i(tag, logMessage)
                    }
                }
                return true
            }
        }
    }
}