package app.pluct.ui.utils.scripts

import org.json.JSONObject

/**
 * Pluct-WebView-Script-Builder - Builds automation scripts for WebView
 */
object PluctWebViewScriptBuilder {
    
    /**
     * Legacy builder (kept for compatibility); prefer buildAutomationScriptFromAsset.
     */
    fun buildAutomationScript(videoUrl: String, runId: String): String {
        val escapedUrl = JSONObject.quote(videoUrl)
        
        return """
            (function() {
                const RUN_ID = '$runId';
                const TARGET_URL = $escapedUrl;
                let automationState = 'INIT';
                let isCompleted = false;
                let retryCount = 0;
                const MAX_RETRIES = 3;
                let checkTimer = null;
                let errorReported = false;
                
                function clearCheckTimer() {
                    if (checkTimer) {
                        clearTimeout(checkTimer);
                        checkTimer = null;
                    }
                }
                
                function clearAllTimers() {
                    clearCheckTimer();
                    for (let i = 1; i < 10000; i++) {
                        clearTimeout(i);
                    }
                }
                
                // Console proxy
                (function(){
                    try {
                        const _log = console.log.bind(console);
                        const _error = console.error.bind(console);
                        console.log = function(){
                            try { if (window.AndroidBridge && window.AndroidBridge.onLogMessage) window.AndroidBridge.onLogMessage('L:' + Array.from(arguments).join(' ')); } catch(_){}
                            return _log.apply(null, arguments);
                        };
                        console.error = function(){
                            try { if (window.AndroidBridge && window.AndroidBridge.onLogMessage) window.AndroidBridge.onLogMessage('E:' + Array.from(arguments).join(' ')); } catch(_){}
                            return _error.apply(null, arguments);
                        };
                    } catch(_){}
                })();

                function log(message) {
                    try {
                        console.log('WV:J:' + message + ' run=' + RUN_ID + ' state=' + automationState);
                        if (window.AndroidBridge && window.AndroidBridge.onLogMessage) window.AndroidBridge.onLogMessage('I:' + message);
                    } catch (e) {
                        console.error('Log error:', e);
                    }
                }
                
                function setState(newState) {
                    automationState = newState;
                    log('state_change_to=' + newState);
                }
                
                function completeAutomation() {
                    if (isCompleted) return;
                    isCompleted = true;
                    setState('COMPLETED');
                    log('automation_completed');
                    clearAllTimers();
                }
                
                function failAutomation(error) {
                    if (isCompleted || errorReported) return;
                    isCompleted = true;
                    errorReported = true;
                    setState('FAILED');
                    log('automation_failed error=' + error);
                    clearAllTimers();
                    
                    try {
                        window.AndroidBridge.onError(error);
                    } catch (e) {
                        log('bridge_error: ' + e.message);
                    }
                }
                
                // Early validation
                if (!TARGET_URL || TARGET_URL.trim() === '') {
                    log('fatal_blank_url');
                    failAutomation('blank_url');
                    return;
                }
                
                log('url=' + TARGET_URL);
                
                // Check if we're on the right domain
                function checkDomain() {
                    if (location.href === 'about:blank') {
                        log('about_blank_wait');
                        setTimeout(checkDomain, 250);
                        return;
                    }
                    
                    const isTokAudit = location.hostname.endsWith('tokaudit.io');
                    const isGetTranscribe = location.hostname.includes('gettranscribe.ai');
                    
                    if (!(isTokAudit || isGetTranscribe)) {
                        log('wrong-domain host=' + location.hostname);
                        setTimeout(checkDomain, 500);
                        return;
                    }
                    
                    if (document.readyState !== 'complete') {
                        log('wait_for_ready');
                        window.addEventListener('load', checkDomain, { once: true });
                        return;
                    }
                    
                    log('page_ready');
                    setState('READY');
                    
                    if (isTokAudit) {
                        PluctTokAuditAutomation.startTokAuditFlow();
                    } else {
                        PluctGetTranscribeAutomation.startGetTranscribeFlow();
                    }
                }
                
                // Start the automation
                checkDomain();
            })();
        """.trimIndent()
    }

    /**
     * Preferred: template the canonical asset content with RUN_ID and TARGET_URL.
     */
    fun buildAutomationScriptFromAsset(assetContent: String, videoUrl: String, runId: String): String {
        val safe = assetContent
            .replace("{{RUN_ID}}", runId)
            .replace("{{TARGET_URL}}", videoUrl)
        return safe
    }
}
