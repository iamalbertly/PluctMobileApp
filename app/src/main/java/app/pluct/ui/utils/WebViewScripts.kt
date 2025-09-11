package app.pluct.ui.utils

import android.util.Log
import android.webkit.WebView
import org.json.JSONObject

/**
 * Simplified WebView script injection utilities
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
     * Build comprehensive automation script
     */
    private fun buildAutomationScript(videoUrl: String, runId: String): String {
        val escapedUrl = JSONObject.quote(videoUrl)
        
        return """
            (function() {
                const RUN_ID = '$runId';
                const TARGET_URL = $escapedUrl;
                const PROVIDER = (function(){
                  try { return (window.location.hostname||'').toLowerCase(); } catch(e){ return ''; }
                })();
                
                // Console proxy to ensure errors/warnings reach Android
                (function(){
                    try {
                        const _log = console.log.bind(console);
                        const _warn = console.warn.bind(console);
                        const _error = console.error.bind(console);
                        const _debug = console.debug ? console.debug.bind(console) : _log;
                        console.log = function(){
                            try { if (window.AndroidBridge && window.AndroidBridge.onLogMessage) window.AndroidBridge.onLogMessage('L:' + Array.from(arguments).join(' ')); } catch(_){}
                            return _log.apply(null, arguments);
                        };
                        console.warn = function(){
                            try { if (window.AndroidBridge && window.AndroidBridge.onLogMessage) window.AndroidBridge.onLogMessage('W:' + Array.from(arguments).join(' ')); } catch(_){}
                            return _warn.apply(null, arguments);
                        };
                        console.error = function(){
                            try { if (window.AndroidBridge && window.AndroidBridge.onLogMessage) window.AndroidBridge.onLogMessage('E:' + Array.from(arguments).join(' ')); } catch(_){}
                            return _error.apply(null, arguments);
                        };
                        console.debug = function(){
                            try { if (window.AndroidBridge && window.AndroidBridge.onLogMessage) window.AndroidBridge.onLogMessage('D:' + Array.from(arguments).join(' ')); } catch(_){}
                            return _debug.apply(null, arguments);
                        };
                        window.addEventListener('error', function(ev){
                            try { if (window.AndroidBridge && window.AndroidBridge.onLogMessage) window.AndroidBridge.onLogMessage('E:window_error ' + (ev.message||'') + ' at ' + (ev.filename||'') + ':' + (ev.lineno||'') ); } catch(_){}
                        });
                        window.addEventListener('unhandledrejection', function(ev){
                            try { if (window.AndroidBridge && window.AndroidBridge.onLogMessage) window.AndroidBridge.onLogMessage('E:unhandled_rejection ' + (ev.reason && (ev.reason.stack||ev.reason.message||ev.reason.toString())) ); } catch(_){}
                        });
                    } catch(_){}
                })();

                function log(message) {
                    try {
                        console.log('WV:J:' + message + ' run=' + RUN_ID);
                        if (window.AndroidBridge && window.AndroidBridge.onLogMessage) window.AndroidBridge.onLogMessage('I:' + message);
                    } catch (e) {
                        console.error('Log error:', e);
                    }
                }
                
                // Early validation
                if (!TARGET_URL || TARGET_URL.trim() === '') {
                    log('fatal_blank_url');
                    try {
                        window.AndroidBridge.onError('blank_url');
                    } catch (e) {
                        log('bridge_error: ' + e.message);
                    }
                    return;
                }
                
                log('url=' + TARGET_URL);
                
                // Domain and readiness guard
                function checkDomainAndReady() {
                    if (location.href === 'about:blank') {
                        log('about_blank_wait');
                        setTimeout(checkDomainAndReady, 250);
                        return;
                    }
                    
                    const isTokAudit = location.hostname.endsWith('tokaudit.io');
                    const isGetTranscribe = location.hostname.includes('gettranscribe.ai');
                    if (!(isTokAudit || isGetTranscribe)) {
                        log('wrong-domain host=' + location.hostname);
                        setTimeout(checkDomainAndReady, 500);
                        return;
                    }
                    
                    if (document.readyState !== 'complete') {
                        log('wait_for_ready');
                        window.addEventListener('load', checkDomainAndReady, { once: true });
                        return;
                    }
                    
                    // Ready to proceed
                    log('page_ready');
                    if (location.hostname.endsWith('tokaudit.io')) startTokAudit();
                    else startGetTranscribe();
                }
                
                function startTokAudit() {
                    try {
                        // Dismiss modals
                        dismissModals();
                        
                        // Find and fill input
                        const input = findInput();
                        if (input) {
                            log('input_found sel=' + input.tagName.toLowerCase());
                            fillInput(input);
                        } else {
                            log('input_not_found');
                            window.AndroidBridge.onError('input_not_found');
                        return;
                    }
                    
                        // Submit form
                        submitForm();
                        
                        // Monitor results
                        monitorTokAuditResults();
                        
                    } catch (e) {
                        log('automation_failed msg=' + e.message);
                        window.AndroidBridge.onError('automation_failed');
                    }
                }

                function startGetTranscribe() {
                    try {
                        log('gt:start');
                        // Simple flow: ensure page ready, attempt to paste URL if input exists, then observe transcript area
                        const input = document.querySelector('input[type="url"], input[placeholder*="url" i], textarea');
                        if (input) {
                            const d = Object.getOwnPropertyDescriptor(input.__proto__, 'value');
                            if (d && d.set) { d.set.call(input, TARGET_URL); input.dispatchEvent(new Event('input',{bubbles:true})); input.dispatchEvent(new Event('change',{bubbles:true})); }
                            log('gt:input_set');
                            // Try submit: prefer the green "Get Transcript" button; fallback to Enter
                            const btns = Array.from(document.querySelectorAll('button, [role="button"], a[role="button"]'));
                            const submitBtn = btns.find(b=>/\bget\s*transcript\b/i.test((b.textContent||'').trim()))
                                  || btns.find(b=>/(transcribe|start|submit|analyze)/i.test((b.textContent||'').trim()));
                            if (submitBtn) {
                                try { submitBtn.scrollIntoView({block:'center'}); } catch(e){}
                                try { submitBtn.click(); log('gt:submit_clicked'); } catch(e){ log('gt:submit_click_failed ' + e.message); }
                            } else {
                                input.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',bubbles:true})); log('gt:enter_fired');
                            }
                        }
                        monitorGetTranscribeResults();
                            } catch (e) {
                        log('gt:failed msg=' + e.message);
                        window.AndroidBridge.onError('automation_failed');
                    }
                }
                
                function dismissModals() {
                    const closeSelectors = [
                        'button[aria-label*="close" i]',
                        'button[aria-label*="dismiss" i]',
                        '.close',
                        '.modal-close',
                        '[data-dismiss="modal"]'
                    ];
                    
                    for (const selector of closeSelectors) {
                        const element = document.querySelector(selector);
                        if (element && element.offsetParent !== null) {
                            element.click();
                            log('modal_dismissed');
                            return;
                        }
                    }
                    
                    log('no_modal');
                }
                
                    function findInput() {
                        const selectors = [
                            'textarea[placeholder*="Video" i]',
                            'input[type="url"]',
                        'input[placeholder*="url" i]',
                            'input[placeholder*="link" i]',
                        'input[placeholder*="tiktok" i]'
                        ];
                        
                        for (const selector of selectors) {
                                const element = document.querySelector(selector);
                                if (element && element.offsetParent !== null) {
                                    return element;
                            }
                        }
                        
                        return null;
                    }
                    
                function fillInput(input) {
                    const descriptor = Object.getOwnPropertyDescriptor(input.__proto__, 'value');
                                if (descriptor && descriptor.set) {
                        descriptor.set.call(input, TARGET_URL);
                        input.dispatchEvent(new Event('input', { bubbles: true }));
                        input.dispatchEvent(new Event('change', { bubbles: true }));
                        
                        // Verify value
                                setTimeout(() => {
                            input.focus();
                                    setTimeout(() => {
                                input.blur();
                                            setTimeout(() => {
                                    input.focus();
                                    if (input.value === TARGET_URL) {
                                        log('value_verified');
                                                setTimeout(() => {
                                            log('pre_submit_wait');
                                        }, 700);
                                    } else {
                                        log('value_reset');
                                        fillInput(input); // Retry
                                                                        }
                                                                    }, 100);
                                    }, 100);
                        }, 500);
                    }
                }
                
                function submitForm() {
                        const buttonSelectors = [
                        'button:contains("Start")',
                        'button:contains("Analyze")',
                        'button:contains("Check")',
                        'button:contains("Go")',
                        'button:contains("Submit")',
                        'button:contains("Get transcript")'
                    ];
                    
                    let button = null;
                        for (const selector of buttonSelectors) {
                        const elements = Array.from(document.querySelectorAll('button'));
                        button = elements.find(btn => 
                            /(start|analyze|check|go|submit|get transcript)/i.test(btn.textContent || '')
                        );
                        if (button && button.offsetParent !== null) break;
                    }
                    
                    if (button) {
                                            button.click();
                        log('submit_clicked');
                    } else {
                        const input = document.querySelector('input, textarea');
                        if (input) {
                            input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
                            log('enter_fired');
                        }
                    }
                }
                
                function monitorTokAuditResults() {
                    let networkCount = 0;
                    let resultFound = false;
                    
                    // Network monitoring
                        const originalFetch = window.fetch;
                        window.fetch = function(...args) {
                        networkCount++;
                        log('net+1 url=' + args[0]);
                        return originalFetch.apply(this, args).finally(() => {
                            networkCount--;
                            log('net-1 url=' + args[0]);
                            if (networkCount === 0) {
                                log('network_idle');
                            }
                                });
                        };
                        
                    // DOM monitoring
                    const observer = new MutationObserver(() => {
                        if (resultFound) return;
                        
                        // Check for transcript
                                                const transcriptSelectors = [
                                                    'pre',
                                                    'textarea',
                                                    '[data-testid*="transcript"]',
                                                    'code',
                                                    'div'
                                                ];
                                                
                                                for (const selector of transcriptSelectors) {
                            const element = document.querySelector(selector);
                            if (element && element.textContent && element.textContent.length >= 30) {
                                resultFound = true;
                                log('result_node_found');
                                copyTranscript(element.textContent);
                                return;
                            }
                        }
                        
                        // Periodic HTML snapshot for diagnostics
                        try {
                            if (window.AndroidBridge && window.AndroidBridge.onHtmlSnapshot) {
                                const head = (document.body && document.body.innerText) ? document.body.innerText.slice(0, 1200) : '';
                                window.AndroidBridge.onHtmlSnapshot(head);
                            }
                        } catch (e) { log('html_snapshot_error ' + e.message); }

                        // Check for errors (expired/invalid)
                        const errorText = document.body.textContent || '';
                        if (/No valid TikTok data found for this link/i.test(errorText)) {
                            log('tokaudit_invalid_data');
                            try { window.AndroidBridge.onError('invalid_data'); } catch(e){}
                            // Dump snapshot html head for diagnosis
                            try { log('html_head=' + (document.body.innerText||'').slice(0,800)); } catch(e){}
                            return;
                        }
                        if (/Invalid URL/i.test(errorText)) {
                            log('invalid_url');
                                return;
                            }
                        if (/Subtitles Not Available/i.test(errorText)) {
                            log('subs_not_available');
                            window.AndroidBridge.onError('no_subtitles');
                                return;
                            }
                        if (/Service Unavailable/i.test(errorText)) {
                            log('service_unavailable');
                                return;
                            }
                        // Copy button missing signal
                        const hasCopy = !!document.querySelector('button, [role="button"]');
                        if (!hasCopy) { log('tokaudit_copy_missing'); }
                    });
                    
                    observer.observe(document.body, { childList: true, subtree: true });
                    
                    // Timeout
                    setTimeout(() => {
                        if (!resultFound) {
                            log('still_waiting inflight=' + networkCount + ' nodes=0');
                        }
                    }, 10000);
                }

                function monitorGetTranscribeResults() {
                    let resultFound = false;
                    let checkCount = 0;
                    const maxChecks = 60; // Check for up to 60 seconds
                    
                    function checkForTranscript() {
                        if (resultFound) return;
                        checkCount++;
                        
                        log('gt:check_attempt=' + checkCount);
                        
                        // First, try the specific selector you mentioned
                        const specificSelectors = [
                            'body > div.flex.min-h-screen.flex-col.bg-black.text-white > main > div > div.flex-grow.w-full.bg-\\[\\#EAEAEA\\].py-12 > div > div > div > div:nth-child(1) > div > div.flex-1 > div > div.bg-\\[\\#EAEAEA\\].p-5.rounded-xl.border.border-\\[\\#081428\\]\\/10.max-h-\\[300px\\].overflow-y-auto > div > p',
                            'div.bg-\\[\\#EAEAEA\\].p-5.rounded-xl.border.border-\\[\\#081428\\]\\/10.max-h-\\[300px\\].overflow-y-auto p',
                            'div.bg-\\[\\#EAEAEA\\].p-5.rounded-xl p',
                            'div.max-h-\\[300px\\].overflow-y-auto p',
                            'div[class*="bg-\\[\\#EAEAEA\\]"] p',
                            'div[class*="rounded-xl"] p'
                        ];
                        
                        for (const selector of specificSelectors) {
                            try {
                                const elements = document.querySelectorAll(selector);
                                for (const el of elements) {
                                    const t = (el.textContent||'').trim();
                                    if (t.length > 50) {
                                        log('gt:found_candidate selector=' + selector + ' len=' + t.length);
                                        log('gt:candidate_content=' + t.substring(0, 200));
                                        
                                        if (isValidTranscript(t)) {
                                            resultFound = true;
                                            log('gt:result_node_found specific selector=' + selector);
                                            log('gt:final_transcript=' + t.substring(0, 500));
                                            copyTranscript(t);
                                            return;
                                        }
                                    }
                                }
                            } catch(e) {
                                log('gt:selector_error=' + selector + ' error=' + e.message);
                            }
                        }
                        
                        // Also check for any paragraph elements that might contain transcript
                        const allParagraphs = document.querySelectorAll('p');
                        for (const p of allParagraphs) {
                            const t = (p.textContent||'').trim();
                            if (t.length > 100 && isValidTranscript(t)) {
                                resultFound = true;
                                log('gt:result_node_found paragraph len=' + t.length);
                                log('gt:paragraph_content=' + t.substring(0, 200));
                                copyTranscript(t);
                                return;
                            }
                        }
                        
                        // Log current page state for debugging
                        try {
                            const bodyText = document.body ? document.body.innerText : '';
                            const bodyLength = bodyText.length;
                            log('gt:page_state body_len=' + bodyLength);
                            
                            // Look for any elements with substantial text
                            const allDivs = document.querySelectorAll('div');
                            for (const div of allDivs) {
                                const text = (div.textContent||'').trim();
                                if (text.length > 200 && text.length < 5000) {
                                    const hasTranscriptKeywords = /transcript|transcription|subtitle|caption/i.test(text);
                                    if (hasTranscriptKeywords) {
                                        log('gt:potential_transcript_div len=' + text.length);
                                        log('gt:div_content=' + text.substring(0, 300));
                                    }
                                }
                            }
                        } catch(e) {
                            log('gt:debug_error=' + e.message);
                        }
                        
                        // Continue checking if we haven't found anything yet
                        if (!resultFound && checkCount < maxChecks) {
                            setTimeout(checkForTranscript, 1000); // Check every second
                        } else if (!resultFound) {
                            log('gt:timeout_after_checks=' + checkCount);
                        }
                    }
                    
                    // Wait a bit for the transcription to start, then check every second
                    setTimeout(() => {
                        checkForTranscript();
                    }, 5000); // Wait 5 seconds before starting to check
                    
                    // Also set up a mutation observer for immediate detection
                    const observer = new MutationObserver(() => {
                        if (!resultFound) {
                            checkForTranscript();
                        }
                    });
                    observer.observe(document.body, { childList: true, subtree: true });
                }
                
                function isValidTranscript(text) {
                    if (!text || text.length < 100) return false;
                    
                    // Must not contain common UI elements or marketing content
                    const excludeKeywords = [
                        'GetTranscribe', 'Pricing', 'Support', 'Login', 'Signup',
                        'Loading', 'Token', 'User:', 'API Documentation',
                        'Terms of Service', 'Privacy Policy', '© 2025',
                        'Transcribing your video', '% complete',
                        'AI delivers', 'accuracy', 'languages', 'learning models',
                        'subscription', 'wallet', 'credits', 'per minute',
                        'simple pricing', 'monthly subscription', 'wallet balance'
                    ];
                    
                    for (const keyword of excludeKeywords) {
                        if (text.includes(keyword)) return false;
                    }
                    
                    // Must not contain JavaScript code patterns
                    const jsPatterns = [
                        /function\s+\w+\s*\(/,
                        /var\s+\w+\s*=/,
                        /let\s+\w+\s*=/,
                        /const\s+\w+\s*=/,
                        /document\./,
                        /window\./,
                        /addEventListener/,
                        /querySelector/,
                        /getElementById/,
                        /innerHTML/,
                        /textContent/,
                        /\.js/,
                        /\.css/,
                        /<script/,
                        /<\/script>/,
                        /console\./,
                        /alert\(/,
                        /setTimeout/,
                        /setInterval/
                    ];
                    
                    for (const pattern of jsPatterns) {
                        if (pattern.test(text)) return false;
                    }
                    
                    // Must not be mostly HTML tags or attributes
                    const htmlTagCount = (text.match(/<[^>]+>/g) || []).length;
                    const htmlAttributeCount = (text.match(/="[^"]*"/g) || []).length;
                    if (htmlTagCount > 5 || htmlAttributeCount > 10) return false;
                    
                    // Should contain substantial natural language
                    const wordCount = text.split(/\s+/).length;
                    if (wordCount < 20) return false;
                    
                    // Should contain sentence endings or common words
                    const hasNaturalLanguage = /[.!?]/.test(text) || 
                                             /\b(the|and|or|but|in|on|at|to|for|of|with|by|is|are|was|were|have|has|had|will|would|could|should)\b/i.test(text);
                    
                    // Should not be mostly numbers or special characters
                    const alphaCount = (text.match(/[a-zA-Z]/g) || []).length;
                    const totalChars = text.length;
                    const alphaRatio = alphaCount / totalChars;
                    
                    // Additional check: should look like actual speech/transcript content
                    const hasSpeechPatterns = /\b(I|you|we|they|he|she|it|this|that|here|there|now|then|so|well|okay|right|yeah|yes|no)\b/i.test(text);
                    
                    return hasNaturalLanguage && alphaRatio > 0.6 && hasSpeechPatterns;
                }
                
                function isPageUI(text) {
                    const uiPatterns = [
                        /GetTranscribe/i,
                        /Pricing|Support|Login|Signup/i,
                        /Loading|Token|User:/i,
                        /API Documentation/i,
                        /Terms of Service|Privacy Policy/i,
                        /© \d{4}/i,
                        /Transcribing your video/i,
                        /% complete/i
                    ];
                    
                    return uiPatterns.some(pattern => pattern.test(text));
                }
                
                function copyTranscript(text) {
                    if (text && text.length >= 30) {
                        // Prefer site Copy Transcript button if available
                        try {
                            const buttons = Array.from(document.querySelectorAll('button, [role="button"], a[role="button"]'));
                            const copyBtn = buttons.find(b => /\bcopy\s*transcript\b/i.test((b.textContent||'').trim()));
                            if (copyBtn) {
                                try { copyBtn.scrollIntoView({block:'center'}); } catch(e){}
                                try { copyBtn.click(); log('gt:copy_button_clicked'); } catch(e){ log('gt:copy_button_click_failed ' + e.message); }
                            }
                        } catch(e) { log('gt:copy_button_lookup_failed ' + e.message); }

                        // Redundant clipboard write via Web Clipboard API
                        try {
                            if (navigator && navigator.clipboard && navigator.clipboard.writeText) {
                                navigator.clipboard.writeText(text).then(()=>log('gt:clipboard_write_ok')).catch(err=>log('gt:clipboard_write_err ' + err));
                            }
                        } catch(e) { log('gt:clipboard_access_err ' + e.message); }

                        log('copied_length=' + text.length);
                        try {
                            window.AndroidBridge.onTranscript(text);
                            window.AndroidBridge.onTranscriptComplete();
                            log('returned');
                        } catch (e) {
                            log('bridge_error: ' + e.message);
                        }
                    }
                }
                
                // Start the automation
                checkDomainAndReady();
            })();
        """.trimIndent()
    }
}
