package app.pluct.ui.utils

import android.util.Log
import android.webkit.WebView
import org.json.JSONObject

/**
 * WebView script injection utilities with proper tokaudit flow
 */
object WebViewScripts {
    private const val TAG = "WebViewScripts"

    /**
     * Inject automation script into WebView
     */
    fun injectAutomationScript(
        webView: WebView, 
        videoUrl: String, 
        runId: String
    ) {
        try {
            Log.d(TAG, "WV:A:injecting_script url=$videoUrl run=$runId")
            RunRingBuffer.addLog(runId, "INFO", "injecting_script url=$videoUrl", "WV")
            
            val script = buildAutomationScript(videoUrl, runId)
            Log.d(TAG, "WV:A:script_built length=${script.length} run=$runId")
            
            webView.evaluateJavascript(script) { result ->
                Log.d(TAG, "WV:A:script_injected result=$result run=$runId")
                RunRingBuffer.addLog(runId, "INFO", "script_injected result=$result", "WV")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting automation script: ${e.message}", e)
            RunRingBuffer.addLog(runId, "ERROR", "script_injection_error: ${e.message}", "WV")
        }
    }
    
    /**
     * Build proper tokaudit automation script
     */
    private fun buildAutomationScript(videoUrl: String, runId: String): String {
        val escapedUrl = JSONObject.quote(videoUrl)
        
        return """
            (function() {
                const RUN_ID = '$runId';
                const TARGET_URL = $escapedUrl;
                let automationState = 'INIT';
                let isCompleted = false;
                let retryCount = 0;
                const MAX_RETRIES = 3;
                
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
                }
                
                function failAutomation(error) {
                    if (isCompleted) return;
                    isCompleted = true;
                    setState('FAILED');
                    log('automation_failed error=' + error);
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
                        startTokAuditFlow();
                    } else {
                        startGetTranscribeFlow();
                    }
                }
                
                // Global input focus keeper to prevent controlled input reset
                let inputFocusKeeper = null;
                
                function startInputFocusKeeper(inputElement, targetValue) {
                    if (inputFocusKeeper) {
                        clearInterval(inputFocusKeeper);
                    }
                    
                    inputFocusKeeper = setInterval(() => {
                        try {
                            // Only maintain if we're still in the input phase
                            if (currentState === 'INPUT_FILLING' || currentState === 'INPUT_FILLED' || currentState === 'START_CLICKED') {
                                if (inputElement && inputElement.value !== targetValue) {
                                    log('tokaudit_input_value_reset_detected, restoring');
                                    inputElement.value = targetValue;
                                    inputElement.dispatchEvent(new Event('input', { 
                                        bubbles: true, 
                                        inputType: 'insertText',
                                        data: targetValue
                                    }));
                                    
                                    // Keep it focused
                                    if (document.activeElement !== inputElement) {
                                        inputElement.focus();
                                    }
                                }
                            } else {
                                // Stop maintaining when we move to other phases
                                clearInterval(inputFocusKeeper);
                                inputFocusKeeper = null;
                            }
                        } catch (e) {
                            log('input_focus_keeper_error: ' + e.message);
                        }
                    }, 100); // Check every 100ms
                }
                
                function startTokAuditFlow() {
                    try {
                        setState('TOKAUDIT_START');
                        log('tokaudit_flow_started');
                        
                        // Step 1: Find and fill input field
                        const input = findTokAuditInput();
                        if (!input) {
                            log('tokaudit_input_not_found');
                            failAutomation('input_not_found');
                            return;
                        }
                        
                        log('tokaudit_input_found');
                        setState('INPUT_FILLING');
                        
                        // Fill input with URL
                        if (!fillTokAuditInput(input)) {
                            log('tokaudit_input_fill_failed');
                            failAutomation('input_fill_failed');
                            return;
                        }
                        
                        log('tokaudit_input_filled');
                        setState('INPUT_FILLED');
                        
                        // Step 2: Click START button
                        setTimeout(() => {
                            if (!clickStartButton()) {
                                log('tokaudit_start_button_not_found');
                                failAutomation('start_button_not_found');
                                return;
                            }
                            
                            log('tokaudit_start_clicked');
                            setState('START_CLICKED');
                            
                            // Step 3: Monitor for results
                            monitorTokAuditResults();
                        }, 1000);
                        
                    } catch (e) {
                        log('tokaudit_flow_error: ' + e.message);
                        failAutomation('flow_error');
                    }
                }
                
                function findTokAuditInput() {
                    const selectors = [
                        'textarea[placeholder*="Video" i]',
                        'input[type="url"]',
                        'input[placeholder*="url" i]',
                        'input[placeholder*="link" i]',
                        'input[placeholder*="tiktok" i]',
                        'textarea',
                        'input[type="text"]'
                    ];
                    
                    for (const selector of selectors) {
                        const element = document.querySelector(selector);
                        if (element && element.offsetParent !== null) {
                            return element;
                        }
                    }
                    return null;
                }
                
                function fillTokAuditInput(input) {
                    try {
                        log('tokaudit_input_filling_started');
                        
                        // Use native setter with proper event dispatching
                        function nativeSet(element, value) {
                            try {
                                // Get the native value setter
                                const proto = Object.getPrototypeOf(element);
                                const desc = Object.getOwnPropertyDescriptor(proto, 'value');
                                
                                if (desc && desc.set) {
                                    // Use the native setter
                                    desc.set.call(element, value);
                                    log('tokaudit_native_setter_used');
                                } else {
                                    // Fallback to direct assignment
                                    element.value = value;
                                    log('tokaudit_direct_assignment_used');
                                }
                                
                                // Dispatch events that frameworks listen for
                                element.dispatchEvent(new Event('input', { bubbles: true }));
                                element.dispatchEvent(new Event('change', { bubbles: true }));
                                
                                log('tokaudit_events_dispatched');
                                return true;
                            } catch (e) {
                                log('tokaudit_native_set_error: ' + e.message);
                                return false;
                            }
                        }
                        
                        // Focus the input first
                        input.focus();
                        input.click();
                        
                        // Set the value using native setter
                        if (nativeSet(input, TARGET_URL)) {
                            log('tokaudit_input_value_set_success');
                            
                            // Verify the value was set
                            setTimeout(() => {
                                if (input.value === TARGET_URL) {
                                    log('tokaudit_input_value_verified');
                                } else {
                                    log('tokaudit_input_value_not_set value=' + input.value);
                                }
                            }, 100);
                            
                            return true;
                        } else {
                            log('tokaudit_input_value_set_failed');
                            return false;
                        }
                        
                    } catch (e) {
                        log('tokaudit_input_fill_error: ' + e.message);
                        return false;
                    }
                }
                
                function clickStartButton() {
                    // Prefer submit buttons over generic buttons
                    const buttonSelectors = [
                        'button[type="submit"]',
                        'button[aria-label*="submit" i]',
                        'button[aria-label*="search" i]',
                        'button[aria-label*="start" i]',
                        'button[aria-label*="analyze" i]'
                    ];
                    
                    // Try specific selectors first
                    for (const selector of buttonSelectors) {
                        try {
                            const button = document.querySelector(selector);
                            if (button && button.offsetParent !== null) {
                                button.scrollIntoView({ block: 'center' });
                                button.focus();
                                button.click();
                                log('tokaudit_submit_button_clicked selector=' + selector);
                                return true;
                            }
                        } catch (e) {
                            log('tokaudit_button_selector_error selector=' + selector + ' error=' + e.message);
                        }
                    }
                    
                    // Fallback: find button by text content
                    const buttons = Array.from(document.querySelectorAll('button'));
                    const startButton = buttons.find(btn => 
                        /(start|analyze|check|go|submit|get transcript)/i.test(btn.textContent || '')
                    );
                    
                    if (startButton && startButton.offsetParent !== null) {
                        startButton.scrollIntoView({ block: 'center' });
                        startButton.focus();
                        startButton.click();
                        log('tokaudit_text_button_clicked');
                        return true;
                    }
                    
                    // Final fallback: try Enter key on input
                    log('tokaudit_no_submit_button_found_trying_enter');
                    const input = findTokAuditInput();
                    if (input) {
                        const evOpts = { key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true };
                        input.dispatchEvent(new KeyboardEvent('keydown', evOpts));
                        input.dispatchEvent(new KeyboardEvent('keyup', evOpts));
                        log('tokaudit_enter_key_fired');
                        return true;
                    }
                    
                    return false;
                }
                
                function monitorTokAuditResults() {
                    setState('MONITORING');
                    log('tokaudit_monitoring_started');
                    
                    let checkCount = 0;
                    const maxChecks = 120; // 120 seconds (2 minutes) for processing
                    let processingAnimationSeen = false;
                    let processingAnimationCleared = false;
                    let transcriptFound = false;
                    let errorFound = false;
                    
                    function checkForResults() {
                        if (transcriptFound || errorFound || isCompleted) return;
                        checkCount++;
                        
                        log('tokaudit_check_attempt=' + checkCount);
                        
                        const bodyText = document.body.textContent || '';
                        
                        // Step 1: Check for "Getting Video Info..." animation
                        if (!processingAnimationSeen) {
                            if (/Getting Video Info/i.test(bodyText) || /Processing/i.test(bodyText) || /Loading/i.test(bodyText)) {
                                processingAnimationSeen = true;
                                log('tokaudit_processing_animation_detected');
                                setState('PROCESSING_ANIMATION');
                            }
                        }
                        
                        // Step 2: Wait for processing animation to clear
                        if (processingAnimationSeen && !processingAnimationCleared) {
                            if (!(/Getting Video Info/i.test(bodyText) || /Processing/i.test(bodyText) || /Loading/i.test(bodyText))) {
                                processingAnimationCleared = true;
                                log('tokaudit_processing_animation_cleared');
                                setState('PROCESSING_COMPLETE');
                            } else {
                                log('tokaudit_still_processing');
                                setTimeout(checkForResults, 1000);
                                return;
                            }
                        }
                        
                        // Step 3: Only check for results after processing animation is cleared
                        if (!processingAnimationCleared) {
                            setTimeout(checkForResults, 1000);
                            return;
                        }
                        
                        // Step 4: Check for error messages
                        if (/No valid TikTok data found for this link/i.test(bodyText)) {
                            log('tokaudit_error_invalid_data');
                            errorFound = true;
                            failAutomation('invalid_data');
                            return;
                        }
                        
                        if (/Invalid URL/i.test(bodyText)) {
                            log('tokaudit_error_invalid_url');
                            errorFound = true;
                            failAutomation('invalid_url');
                            return;
                        }
                        
                        if (/Subtitles Not Available/i.test(bodyText)) {
                            log('tokaudit_error_no_subtitles');
                            errorFound = true;
                            failAutomation('no_subtitles');
                            return;
                        }
                        
                        // Step 5: Check for COPY button (indicates transcript is ready)
                        const copyButton = findCopyButton();
                        if (copyButton && copyButton.offsetParent !== null) {
                            log('tokaudit_copy_button_found');
                            setState('COPY_BUTTON_FOUND');
                            
                            // Find the transcript text near the copy button
                            const transcriptText = findTranscriptNearCopyButton(copyButton);
                            if (transcriptText && isValidTranscript(transcriptText)) {
                                log('tokaudit_transcript_found len=' + transcriptText.length);
                                transcriptFound = true;
                                setState('TRANSCRIPT_FOUND');
                                
                                // Click the COPY button
                                if (clickCopyButton()) {
                                    log('tokaudit_copy_button_clicked_successfully');
                                    // Copy transcript to bridge
                                    copyTranscript(transcriptText);
                                    return;
                                } else {
                                    log('tokaudit_copy_button_click_failed');
                                    // Still try to copy the transcript
                                    copyTranscript(transcriptText);
                                    return;
                                }
                            } else {
                                log('tokaudit_no_valid_transcript_near_copy_button');
                            }
                        }
                        
                        // Step 6: Check for transcript content in various elements
                        const transcriptText = findTranscriptInPage();
                        if (transcriptText && isValidTranscript(transcriptText)) {
                            log('tokaudit_transcript_found_in_page len=' + transcriptText.length);
                            transcriptFound = true;
                            setState('TRANSCRIPT_FOUND');
                            
                            // Try to click COPY button if available
                            clickCopyButton();
                            
                            // Copy transcript to bridge
                            copyTranscript(transcriptText);
                            return;
                        }
                        
                        // Continue monitoring
                        if (checkCount < maxChecks) {
                            setTimeout(checkForResults, 1000);
                        } else {
                            log('tokaudit_timeout_after_checks=' + checkCount);
                            failAutomation('timeout');
                        }
                    }
                    
                    // Start checking after a delay
                    setTimeout(checkForResults, 3000);
                }
                
                function findCopyButton() {
                    try {
                        // Strategy 1: Look for buttons with copy-related classes
                        const classSelectors = [
                            'button[class*="copy"]',
                            'button[class*="Copy"]', 
                            'button[class*="COPY"]',
                            '[role="button"][class*="copy"]',
                            'div[class*="copy"]'
                        ];
                        
                        for (const selector of classSelectors) {
                            const copyButton = document.querySelector(selector);
                            if (copyButton && copyButton.offsetParent !== null) {
                                return copyButton;
                            }
                        }
                        
                        // Strategy 2: Look for buttons with copy text content
                        const buttons = document.querySelectorAll('button, [role="button"], div[onclick]');
                        for (const btn of buttons) {
                            if (btn.textContent && btn.textContent.toLowerCase().includes('copy')) {
                                return btn;
                            }
                        }
                        
                        // Strategy 3: Look for common copy button patterns
                        const commonSelectors = [
                            'button[aria-label*="copy" i]',
                            'button[title*="copy" i]',
                            '[data-testid*="copy"]',
                            '[data-cy*="copy"]'
                        ];
                        
                        for (const selector of commonSelectors) {
                            const copyButton = document.querySelector(selector);
                            if (copyButton && copyButton.offsetParent !== null) {
                                return copyButton;
                            }
                        }
                        
                        return null;
                    } catch (e) {
                        log('find_copy_button_error: ' + e.message);
                        return null;
                    }
                }
                
                function clickCopyButton() {
                    try {
                        const copyButton = findCopyButton();
                        if (copyButton && copyButton.offsetParent !== null) {
                            copyButton.scrollIntoView({ block: 'center' });
                            copyButton.focus();
                            copyButton.click();
                            log('tokaudit_copy_button_clicked');
                            return true;
                        } else {
                            log('tokaudit_copy_button_not_found');
                        }
                    } catch (e) {
                        log('tokaudit_copy_button_error: ' + e.message);
                    }
                    return false;
                }
                
                function findTranscriptNearCopyButton(copyButton) {
                    try {
                        // Look for transcript text near the copy button
                        const parent = copyButton.closest('div');
                        if (parent) {
                            const textElements = parent.querySelectorAll('pre, textarea, code, div[class*="transcript"], div[class*="result"], div[class*="content"]');
                            for (const element of textElements) {
                                if (element && element.textContent && element.textContent.length >= 100) {
                                    const text = element.textContent.trim();
                                    if (isValidTranscript(text)) {
                                        return text;
                                    }
                                }
                            }
                        }
                        return null;
                    } catch (e) {
                        log('find_transcript_near_copy_error: ' + e.message);
                        return null;
                    }
                }
                
                function findTranscriptInPage() {
                    try {
                        const transcriptSelectors = [
                            'pre',
                            'textarea',
                            'code',
                            'div[class*="transcript"]',
                            'div[class*="result"]',
                            'div[class*="content"]'
                        ];
                        
                        for (const selector of transcriptSelectors) {
                            const element = document.querySelector(selector);
                            if (element && element.textContent && element.textContent.length >= 100) {
                                const text = element.textContent.trim();
                                
                                // Validate it's not the input URL
                                if (text.includes('vm.tiktok.com') || text.includes('tiktok.com') || /^https?:\/\//.test(text)) {
                                    continue;
                                }
                                
                                // Validate it looks like a transcript
                                if (isValidTranscript(text)) {
                                    return text;
                                }
                            }
                        }
                        return null;
                    } catch (e) {
                        log('find_transcript_in_page_error: ' + e.message);
                        return null;
                    }
                }
                
                function startGetTranscribeFlow() {
                    try {
                        setState('GETTRANSCRIBE_START');
                        log('gettranscribe_flow_started');
                        
                        // Similar flow for gettranscribe.ai
                        const input = document.querySelector('input[type="url"], input[placeholder*="url" i], textarea');
                        if (input) {
                            input.focus();
                            input.value = TARGET_URL;
                            input.dispatchEvent(new Event('input', { bubbles: true }));
                            input.dispatchEvent(new Event('change', { bubbles: true }));
                            
                            log('gettranscribe_input_filled');
                            
                            // Click submit button
                            const submitBtn = document.querySelector('button:contains("Get Transcript"), button:contains("Start")');
                            if (submitBtn) {
                                submitBtn.click();
                                log('gettranscribe_submit_clicked');
                                
                                // Monitor for results
                                monitorGetTranscribeResults();
                            } else {
                                failAutomation('submit_button_not_found');
                            }
                        } else {
                            failAutomation('input_not_found');
                        }
                    } catch (e) {
                        log('gettranscribe_flow_error: ' + e.message);
                        failAutomation('flow_error');
                    }
                }
                
                function monitorGetTranscribeResults() {
                    setState('MONITORING');
                    log('gettranscribe_monitoring_started');
                    
                    let checkCount = 0;
                    const maxChecks = 60;
                    let transcriptFound = false;
                    
                    function checkForTranscript() {
                        if (transcriptFound || isCompleted) return;
                        checkCount++;
                        
                        log('gettranscribe_check_attempt=' + checkCount);
                        
                        // Look for transcript in specific selectors
                        const selectors = [
                            'div.bg-\\[\\#EAEAEA\\].p-5.rounded-xl p',
                            'div[class*="transcript"] p',
                            'div[class*="result"] p',
                            'pre',
                            'textarea[readonly]'
                        ];
                        
                        for (const selector of selectors) {
                            const elements = document.querySelectorAll(selector);
                            for (const el of elements) {
                                const text = (el.textContent || '').trim();
                                if (text.length > 100 && isValidTranscript(text)) {
                                    log('gettranscribe_transcript_found len=' + text.length);
                                    transcriptFound = true;
                                    setState('TRANSCRIPT_FOUND');
                                    copyTranscript(text);
                                    return;
                                }
                            }
                        }
                        
                        if (checkCount < maxChecks) {
                            setTimeout(checkForTranscript, 1000);
                        } else {
                            log('gettranscribe_timeout_after_checks=' + checkCount);
                            failAutomation('timeout');
                        }
                    }
                    
                    setTimeout(checkForTranscript, 5000);
                }
                
                function isValidTranscript(text) {
                    if (!text || text.length < 100) return false;
                    
                    // Must not be a URL
                    if (/^https?:\/\//.test(text.trim()) || text.includes('vm.tiktok.com') || text.includes('tiktok.com')) {
                        return false;
                    }
                    
                    // Must not contain UI elements
                    const excludeKeywords = [
                        'GetTranscribe', 'Pricing', 'Support', 'Login', 'Signup',
                        'Loading', 'Token', 'User:', 'API Documentation',
                        'Terms of Service', 'Privacy Policy', '© 2025',
                        'TikTok Transcript Generator', 'Turn speech into text'
                    ];
                    
                    for (const keyword of excludeKeywords) {
                        if (text.includes(keyword)) return false;
                    }
                    
                    // Must contain natural language
                    const wordCount = text.split(/\s+/).length;
                    if (wordCount < 20) return false;
                    
                    const hasNaturalLanguage = /[.!?]/.test(text) || 
                                             /\b(the|and|or|but|in|on|at|to|for|of|with|by|is|are|was|were|have|has|had|will|would|could|should)\b/i.test(text);
                    
                    return hasNaturalLanguage;
                }
                
                function copyTranscript(text) {
                    if (isCompleted) return;
                    
                    try {
                        setState('COPYING');
                        log('copying_transcript len=' + text.length);
                        
                        // Skip clipboard operations and directly pass to Android bridge
                        // This avoids permission issues with WebView clipboard access
                        log('bypassing_clipboard_direct_bridge');
                        completeAutomation();
                        
                        try {
                            window.AndroidBridge.onTranscript(text);
                            window.AndroidBridge.onTranscriptComplete();
                            log('transcript_sent_to_bridge_success');
                        } catch (e) {
                            log('bridge_error: ' + e.message);
                            failAutomation('bridge_failed');
                        }
                        
                    } catch (e) {
                        log('copy_error: ' + e.message);
                        failAutomation('copy_failed');
                    }
                }
                
                function fallbackCopy(text) {
                    try {
                        const textarea = document.createElement('textarea');
                        textarea.value = text;
                        textarea.style.position = 'absolute';
                        textarea.style.left = '-9999px';
                        textarea.style.opacity = '0';
                        document.body.appendChild(textarea);
                        textarea.focus();
                        textarea.select();
                        document.execCommand('copy');
                        document.body.removeChild(textarea);
                        
                        log('fallback_copy_success');
                        completeAutomation();
                        try {
                            window.AndroidBridge.onTranscript(text);
                            window.AndroidBridge.onTranscriptComplete();
                        } catch (e) {
                            log('bridge_error: ' + e.message);
                        }
                    } catch (e) {
                        log('fallback_copy_error: ' + e.message);
                        failAutomation('copy_failed');
                    }
                }
                
                // Start the automation
                checkDomain();
            })();
        """.trimIndent()
    }
}