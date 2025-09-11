/**
 * Comprehensive TokAudit Automation Script
 * 
 * This script implements the complete end-to-end automation workflow:
 * 1. Modal removal with comprehensive selectors
 * 2. React-proof input setting with native setters
 * 3. START button detection and clicking
 * 4. AJAX monitoring and network idle detection
 * 5. Transcript candidate detection and logging
 * 6. Copy/extract and bridge to Android
 * 
 * All logs follow the format: WV:J:<message> run=<runId>
 */

(function() {
    'use strict';
    
    // Configuration - these will be replaced by the injection system
    const RUN_ID = '{{RUN_ID}}';
    const TARGET_URL = '{{TARGET_URL}}';
    
    // Logging function - all logs must start with WV:J: and end with run=<id>
    function log(x) {
        try {
            console.log('WV:J:' + x + ' run=' + RUN_ID);
        } catch (e) {
            // Silent fail on logging errors
        }
    }
    
    // Global deduplication - prevent any transcript processing if already sent
    if (window.transcriptAlreadyProcessed) {
        log('script_already_processed_globally');
        return;
    }
    
    // Early return if TARGET_URL is falsy
    if (!TARGET_URL || TARGET_URL.trim() === '') {
        log('fatal_blank_url');
        if (window.Android && window.Android.onError) {
            try {
                window.Android.onError('blank_url');
            } catch (e) {
                log('error_calling_onError: ' + e.message);
            }
        }
        return;
    }
    
    log('url=' + TARGET_URL);
    
    // Wait for page to be ready
    function waitForPageReady() {
        if (location.href === 'about:blank') {
            log('about_blank_wait');
            setTimeout(waitForPageReady, 250);
            return;
        }
        
        // Accept *.tokaudit.io domains
        if (location.hostname !== 'tokaudit.io' && !location.hostname.endsWith('.tokaudit.io')) {
            log('not_on_tokaudit_page_current_url=' + location.href);
            if (window.Android && window.Android.onError) {
                try {
                    window.Android.onError('Not on transcript service page. Current URL: ' + location.href);
                } catch (e) {
                    log('error_calling_onError: ' + e.message);
                }
            }
            return;
        }
        
        if (document.readyState !== 'complete') {
            log('wait_for_ready state=' + document.readyState);
            window.addEventListener('load', function() {
                setTimeout(startAutomation, 1000);
            }, { once: true });
            return;
        }
        
        log('phase=page_ready');
        startAutomation();
    }
    
    function startAutomation() {
        try {
            log('starting_automation');
            
            // Close modals first
            dismissModals();
            
            // Set up AJAX monitoring
            setupAjaxMonitoring();
            
            // Fill and submit URL
            fillAndSubmitUrl();
            
            // Start monitoring for completion
            setTimeout(monitorTranscriptCompletion, 3000);
            
        } catch (e) {
            log('error_in_startAutomation: ' + e.message);
            if (window.Android && window.Android.onError) {
                try {
                    window.Android.onError('Automation failed: ' + e.message);
                } catch (err) {
                    log('error_calling_onError: ' + err.message);
                }
            }
        }
    }
    
    // Close any modals that might be blocking the interface
    function dismissModals() {
        try {
            if (document.readyState !== 'complete' || !document.body) {
                log('page_not_ready_for_modal_close');
                return;
            }
            
            let modalDismissed = false;
            
            // First try clicking clear close controls
            const closeSelectors = [
                '[aria-label*="close" i]',
                '.ant-modal-close',
                '[data-dismiss]',
                '.close',
                '.close-btn',
                '[aria-label*="Close"]'
            ];
            
            for (const selector of closeSelectors) {
                try {
                    const elements = document.querySelectorAll(selector);
                    for (const element of elements) {
                        if (element.offsetParent !== null) {
                            element.click();
                            modalDismissed = true;
                            log('close_button_clicked: ' + selector);
                        }
                    }
                } catch (e) {
                    log('error_clicking_close_' + selector + ': ' + e.message);
                }
            }
            
            // Try buttons with close/ok/got it/accept text
            const buttons = document.querySelectorAll('button');
            for (const button of buttons) {
                if (button.offsetParent !== null) {
                    const text = button.textContent.toLowerCase();
                    if (/close|ok|got it|accept/i.test(text)) {
                        button.click();
                        modalDismissed = true;
                        log('close_button_clicked: text=' + button.textContent.trim());
                    }
                }
            }
            
            // If still present, hide backdrops/overlays by style changes
            const modalSelectors = [
                '[role="dialog"]',
                '.modal',
                '.popup',
                '.overlay',
                '[data-testid*="modal"]',
                '[aria-modal="true"]',
                '.ant-modal-mask',
                '.modal-backdrop'
            ];
            
            for (const selector of modalSelectors) {
                try {
                    const elements = document.querySelectorAll(selector);
                    for (const element of elements) {
                        if (element.style.display !== 'none') {
                            element.style.display = 'none';
                            element.style.visibility = 'hidden';
                            element.style.pointerEvents = 'none';
                            modalDismissed = true;
                            log('modal_hidden: ' + selector);
                        }
                    }
                } catch (e) {
                    log('error_hiding_modal_' + selector + ': ' + e.message);
                }
            }
            
            if (modalDismissed) {
                log('phase=modal_dismissed');
            } else {
                log('phase=no_modal');
            }
            
        } catch (e) {
            log('error_in_dismissModals: ' + e.message);
        }
    }
    
    // Monitor AJAX requests to detect when TokAudit completes
    function setupAjaxMonitoring() {
        log('setting_up_ajax_monitoring');
        
        // Guard to prevent multiple hooking
        if (window._ajaxMonitoringHooked) {
            return;
        }
        window._ajaxMonitoringHooked = true;
        
        let inflightCount = 0;
        
        // Monitor fetch requests
        const originalFetch = window.fetch;
        window.fetch = function(...args) {
            const url = args[0];
            const startTime = Date.now();
            inflightCount++;
            log('net+1 url=' + url);
            
            return originalFetch.apply(this, args)
                .then(response => {
                    const duration = Date.now() - startTime;
                    inflightCount--;
                    let bodyHead = '';
                    try {
                        // Try to get response body preview (first 200 chars)
                        if (response.body) {
                            const reader = response.body.getReader();
                            reader.read().then(({ value }) => {
                                if (value) {
                                    bodyHead = new TextDecoder().decode(value.slice(0, 200));
                                }
                            });
                        }
                    } catch (e) {
                        // Ignore body reading errors
                    }
                    log('net-1 url=' + url + ' status=' + response.status + ' ms=' + duration + ' body_head=' + bodyHead);
                    
                    if (inflightCount === 0) {
                        log('network_idle');
                    }
                    
                    return response;
                })
                .catch(error => {
                    const duration = Date.now() - startTime;
                    inflightCount--;
                    log('net-1 url=' + url + ' status=error ms=' + duration + ' error=' + error.message);
                    
                    if (inflightCount === 0) {
                        log('network_idle');
                    }
                    
                    throw error;
                });
        };
        
        // Start interval for waiting status logging (every 10 seconds)
        let waitingTicks = 0;
        const waitingInterval = setInterval(() => {
            waitingTicks++;
            if (waitingTicks % 5 === 0) { // Every 5th tick = 10 seconds
                const nodeCount = document.querySelectorAll('*').length;
                log('still_waiting inflight=' + inflightCount + ' nodes=' + nodeCount);
            }
        }, 2000);
        
        // Store interval for cleanup
        window._waitingInterval = waitingInterval;
        
        // Set up MutationObserver for transcript candidate detection
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                mutation.addedNodes.forEach((node) => {
                    if (node.nodeType === Node.ELEMENT_NODE) {
                        const element = node;
                        const text = element.textContent ? element.textContent.trim() : '';
                        
                        if (text.length >= 30) {
                            // Check if it looks like a transcript candidate
                            const transcriptSelectors = [
                                'pre', '[data-testid*="transcript"]', '[class*="transcript"]', 
                                'code', '.result', '.mantine-*', 'article', 'section', 'div'
                            ];
                            
                            let isCandidate = false;
                            for (const selector of transcriptSelectors) {
                                if (element.matches && element.matches(selector)) {
                                    isCandidate = true;
                                    break;
                                }
                            }
                            
                            if (isCandidate || element.tagName === 'DIV') {
                                const cssPath = getBestCssPath(element);
                                const head = text.substring(0, 120).replace(/\n/g, ' ').replace(/\r/g, ' ');
                                log('transcript_element_candidate tag=' + element.tagName.toLowerCase() + ' sel=' + cssPath + ' len=' + text.length + ' head=' + head);
                            }
                        }
                    }
                });
            });
        });
        
        observer.observe(document.body, { childList: true, subtree: true });
        window._transcriptObserver = observer;
        
        // Monitor XMLHttpRequest
        const originalXHROpen = XMLHttpRequest.prototype.open;
        const originalXHRSend = XMLHttpRequest.prototype.send;
        
        XMLHttpRequest.prototype.open = function(method, url, ...args) {
            this._url = url;
            return originalXHROpen.apply(this, [method, url, ...args]);
        };
        
        XMLHttpRequest.prototype.send = function(data) {
            const url = this._url;
            const startTime = Date.now();
            inflightCount++;
            log('net+1 url=' + url);
            
            this.addEventListener('loadend', () => {
                const duration = Date.now() - startTime;
                inflightCount--;
                log('net-1 url=' + url + ' status=' + this.status + ' ms=' + duration);
                
                if (inflightCount === 0) {
                    log('network_idle');
                }
            });
            
            this.addEventListener('error', () => {
                const duration = Date.now() - startTime;
                inflightCount--;
                log('net-1 url=' + url + ' status=error ms=' + duration);
                
                if (inflightCount === 0) {
                    log('network_idle');
                }
            });
            
            return originalXHRSend.apply(this, [data]);
        };
    }
    
    // Helper function to get the best CSS selector for an element
    function getBestCssPath(element) {
        if (element.id) {
            return '#' + element.id;
        }
        
        if (element.className) {
            const classes = element.className.split(' ').filter(c => c.trim());
            if (classes.length > 0) {
                return element.tagName.toLowerCase() + '.' + classes.join('.');
            }
        }
        
        // Try data attributes
        for (const attr of element.attributes) {
            if (attr.name.startsWith('data-')) {
                return element.tagName.toLowerCase() + '[' + attr.name + '="' + attr.value + '"]';
            }
        }
        
        return element.tagName.toLowerCase();
    }
    
    // Fill URL and submit
    function fillAndSubmitUrl() {
        try {
            log('starting_url_fill_and_submit');
            
            // Find input field
            const urlInput = findInput();
            if (!urlInput) {
                log('could_not_find_url_input_field');
                if (window.Android && window.Android.onError) {
                    try {
                        window.Android.onError('Could not find URL input field');
                    } catch (e) {
                        log('error_calling_onError: ' + e.message);
                    }
                }
                return;
            }
            
            // Set value with native setter that survives React controlled resets
            setInputValue(urlInput, TARGET_URL);
            
            // Submit the form
            submitForm(urlInput);
            
        } catch (e) {
            log('error_in_fillAndSubmitUrl: ' + e.message);
            if (window.Android && window.Android.onError) {
                try {
                    window.Android.onError('Error filling form: ' + e.message);
                } catch (err) {
                    log('error_calling_onError: ' + err.message);
                }
            }
        }
    }
    
    // Find input field with proper selectors
    function findInput() {
        const urlInputSelectors = [
            'textarea[placeholder*="Video" i]',
            'input[type="url"]',
            'input[placeholder*="URL"]',
            'input[placeholder*="url"]',
            'input[placeholder*="link"]',
            'input[placeholder*="Link"]',
            'input[name*="url"]',
            'input[id*="url"]',
            'input[class*="url"]',
            'textarea[placeholder*="URL"]',
            'textarea[placeholder*="url"]',
            'textarea[placeholder*="link"]',
            'textarea[placeholder*="Link"]',
            'textarea[name*="url"]',
            'textarea[id*="url"]',
            'textarea[class*="url"]',
            'input[type="text"]',
            'textarea'
        ];
        
        for (const selector of urlInputSelectors) {
            const element = document.querySelector(selector);
            if (element && element.offsetParent !== null) {
                log('input_found sel=' + selector);
                return element;
            }
        }
        
        return null;
    }
    
    // Set input value with native setter that survives React controlled resets
    function setInputValue(element, value) {
        let attempts = 0;
        const maxAttempts = 3;
        
        function setValueReactSafe(el, val) {
            const proto = Object.getPrototypeOf(el);
            const desc = proto && Object.getOwnPropertyDescriptor(proto, 'value');
            if (desc && desc.set) {
                desc.set.call(el, val);
            } else {
                el.value = val;
            }
            el.dispatchEvent(new Event('input', { bubbles: true }));
            el.dispatchEvent(new Event('change', { bubbles: true }));
        }
        
        function trySetValue() {
            attempts++;
            
            try {
                setValueReactSafe(element, value);
                
                // Focus and wait
                element.focus();
                
                setTimeout(() => {
                    element.blur();
                    
                    setTimeout(() => {
                        element.focus();
                        
                        setTimeout(() => {
                            // Verify the value survived
                            if (element.value === value) {
                                log('value_verified');
                            } else {
                                log('value_reset');
                                if (attempts < maxAttempts) {
                                    trySetValue();
                                } else {
                                    // Last resort: type character by character
                                    typeCharacterByCharacter(element, value);
                                }
                            }
                        }, 100);
                    }, 100);
                }, 500);
                
            } catch (e) {
                log('error_setting_value: ' + e.message);
                if (attempts < maxAttempts) {
                    trySetValue();
                }
            }
        }
        
        trySetValue();
    }
    
    // Type character by character as last resort
    function typeCharacterByCharacter(element, value) {
        element.value = '';
        element.focus();
        
        let index = 0;
        const interval = setInterval(() => {
            if (index < value.length) {
                element.value += value[index];
                element.dispatchEvent(new Event('input', { bubbles: true }));
                index++;
            } else {
                clearInterval(interval);
                element.dispatchEvent(new Event('change', { bubbles: true }));
                log('value_verified');
            }
        }, 20);
    }
    
    // Submit the form
    function submitForm(inputElement) {
        try {
            // Look for visible button whose text matches start/analyze/check/go/submit/get transcript
            const buttons = document.querySelectorAll('button');
            let submitButton = null;
            
            for (const button of buttons) {
                if (button.offsetParent !== null && !button.disabled) {
                    const text = button.textContent.toLowerCase().trim();
                    if (/\b(start|analyze|check|go|submit|get transcript)\b/i.test(text)) {
                        submitButton = button;
                        break;
                    }
                }
            }
            
            if (submitButton) {
                log('btn_start_found txt="' + submitButton.textContent.trim() + '"');
                log('submit_clicked');
                submitButton.click();
                return;
            }
            
            // If no submit button found, try Enter key
            log('enter_fired');
            inputElement.focus();
            inputElement.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
            inputElement.dispatchEvent(new KeyboardEvent('keyup', { key: 'Enter', bubbles: true }));
            
        } catch (e) {
            log('error_in_submitForm: ' + e.message);
        }
    }
    
    // Monitor for transcript completion
    function monitorTranscriptCompletion() {
        try {
            // Check for error conditions first
            const pageText = document.body ? document.body.innerText || document.body.textContent || '' : '';
            
            if (pageText.includes('Invalid URL')) {
                log('invalid_url');
                // Retry once by toggling trailing slash
                const currentUrl = TARGET_URL;
                const newUrl = currentUrl.endsWith('/') ? currentUrl.slice(0, -1) : currentUrl + '/';
                log('retry_trailing_slash old=' + currentUrl + ' new=' + newUrl);
                TARGET_URL = newUrl;
                setTimeout(() => {
                    const input = findInput();
                    if (input) {
                        setInputValue(input, newUrl);
                        submitForm(input);
                    }
                }, 1000);
                return;
            }
            
            if (pageText.includes('Subtitles Not Available')) {
                log('subs_not_available');
                if (window.Android && window.Android.onError) {
                    try {
                        window.Android.onError('no_subtitles');
                    } catch (e) {
                        log('error_calling_onError: ' + e.message);
                    }
                }
                log('returned');
                return;
            }
            
            if (pageText.includes('Service Unavailable') || pageText.includes('500') || pageText.includes('502') || pageText.includes('503') || pageText.includes('504')) {
                log('service_unavailable');
                setTimeout(() => {
                    log('retry_service');
                    const input = findInput();
                    if (input) {
                        submitForm(input);
                    }
                }, 2500);
                return;
            }
            
            // Check for captcha/challenge
            const captchaSelectors = [
                '[data-testid*="captcha"]',
                '[class*="captcha"]',
                '[id*="captcha"]',
                '[data-testid*="challenge"]',
                '[class*="challenge"]',
                '[id*="challenge"]'
            ];
            
            for (const selector of captchaSelectors) {
                if (document.querySelector(selector)) {
                    log('challenge_detected');
                    if (window.Android && window.Android.onError) {
                        try {
                            window.Android.onError('challenge');
                        } catch (e) {
                            log('error_calling_onError: ' + e.message);
                        }
                    }
                    log('returned');
                    return;
                }
            }
            
            // Look for copy button or transcript text
            const copyButtonSelectors = [
                'button[aria-label*="copy"]',
                'button[aria-label*="Copy"]',
                '[data-testid*="copy"]',
                'button[title*="copy"]',
                'button[title*="Copy"]'
            ];
            
            const transcriptSelectors = [
                'pre',
                '[data-testid*="transcript"]',
                '[class*="transcript"]',
                'code',
                '.result',
                '.mantine-*',
                'article',
                'section',
                'div'
            ];
            
            let copyButton = null;
            let transcriptElement = null;
            
            // Find copy button
            for (const selector of copyButtonSelectors) {
                copyButton = document.querySelector(selector);
                if (copyButton && copyButton.offsetParent !== null) {
                    log('found_copy_button: ' + selector);
                    break;
                }
            }
            
            // Also look for buttons with "Copy" text
            if (!copyButton) {
                const buttons = document.querySelectorAll('button');
                for (const button of buttons) {
                    if (button.offsetParent !== null) {
                        const text = button.textContent.toLowerCase().trim();
                        if (text.includes('copy')) {
                            copyButton = button;
                            log('copy_button_found');
                            break;
                        }
                    }
                }
            }
            
            // Find transcript element with comprehensive candidate logging
            log('searching_for_transcript_elements');
            let resultNodeFound = false;
            
            for (const selector of transcriptSelectors) {
                transcriptElement = document.querySelector(selector);
                if (transcriptElement && transcriptElement.offsetParent !== null) {
                    const text = transcriptElement.textContent || transcriptElement.value || '';
                    const cleanText = text.trim();
                    
                    if (cleanText.length >= 30) {
                        // Generate best CSS path for this element
                        const cssPath = getBestCssPath(transcriptElement);
                        const head = cleanText.substring(0, 120).replace(/\n/g, ' ').replace(/\r/g, ' ');
                        
                        log('transcript_element_candidate tag=' + transcriptElement.tagName.toLowerCase() + ' sel=' + cssPath + ' len=' + cleanText.length + ' head=' + head);
                        
                        if (!resultNodeFound) {
                            log('result_node_found');
                            resultNodeFound = true;
                        }
                        
                        log('found_transcript_element: ' + selector);
                        break;
                    }
                }
            }
            
            // If no transcript element found, try to find any element with substantial text
            if (!transcriptElement) {
                log('no_transcript_element_found_trying_general_search');
                const allDivs = document.querySelectorAll('div');
                for (const div of allDivs) {
                    if (div.offsetParent !== null) {
                        const text = div.textContent || '';
                        if (text.trim().length >= 100 && text.trim().length <= 5000) {
                            // Check if it looks like a transcript (contains common transcript words)
                            const transcriptWords = ['said', 'speaking', 'voice', 'audio', 'video', 'transcript', 'subtitle', 'caption'];
                            const hasTranscriptWords = transcriptWords.some(word => text.toLowerCase().includes(word));
                            if (hasTranscriptWords || text.includes(' ')) {
                                log('found_potential_transcript_div length=' + text.length);
                                transcriptElement = div;
                                break;
                            }
                        }
                    }
                }
            }
            
            if (copyButton || transcriptElement) {
                log('transcript_ready_detected');
                
                if (copyButton) {
                    // Click copy button to get transcript
                    copyButton.click();
                    log('copy_button_clicked');
                    
                    // Wait a bit for clipboard to be populated, then try to get transcript
                    setTimeout(() => {
                        let transcriptText = '';
                        
                        // Try to get from clipboard first
                        if (navigator.clipboard && navigator.clipboard.readText) {
                            log('attempting_clipboard_read');
                            navigator.clipboard.readText().then(text => {
                                log('clipboard_read_success length=' + (text ? text.length : 0));
                                if (text && text.trim().length >= 30) {
                                    transcriptText = text;
                                    log('copied_length=' + transcriptText.trim().length);
                                } else {
                                    log('clipboard_text_too_short length=' + (text ? text.length : 0));
                                }
                                sendTranscript(transcriptText);
                            }).catch((error) => {
                                log('clipboard_read_failed: ' + error.message);
                                // Fallback to direct extraction
                                if (transcriptElement) {
                                    transcriptText = transcriptElement.textContent || transcriptElement.value || '';
                                    log('direct_extraction length=' + transcriptText.length);
                                    if (transcriptText.trim().length >= 30) {
                                        log('copied_length=' + transcriptText.trim().length);
                                    }
                                }
                                sendTranscript(transcriptText);
                            });
                        } else {
                            log('clipboard_api_not_available');
                            // Fallback to direct extraction
                            if (transcriptElement) {
                                transcriptText = transcriptElement.textContent || transcriptElement.value || '';
                                log('direct_extraction length=' + transcriptText.length);
                                if (transcriptText.trim().length >= 30) {
                                    log('copied_length=' + transcriptText.trim().length);
                                }
                            }
                            sendTranscript(transcriptText);
                        }
                    }, 1000);
                } else if (transcriptElement) {
                    // Get transcript directly from element
                    const transcriptText = transcriptElement.textContent || transcriptElement.value || '';
                    log('direct_transcript_extraction length=' + transcriptText.length);
                    if (transcriptText.trim().length >= 30) {
                        log('copied_length=' + transcriptText.trim().length);
                    }
                    sendTranscript(transcriptText);
                }
            } else {
                // Log waiting status
                const nodeCount = document.querySelectorAll('*').length;
                log('still_waiting inflight=' + inflightCount + ' nodes=' + nodeCount);
                
                // Check if we've been waiting too long (12-15s after submit)
                const now = Date.now();
                if (!window._submitTime) {
                    window._submitTime = now;
                }
                
                if (now - window._submitTime > 15000) {
                    // Log HTML snapshot on stall
                    const bodyText = document.body ? document.body.innerText || document.body.textContent || '' : '';
                    const head = bodyText.substring(0, 500).replace(/\n/g, ' ').replace(/\r/g, ' ');
                    log('html_snapshot len=' + bodyText.length + ' head=' + head);
                }
                
                // Continue monitoring
                setTimeout(monitorTranscriptCompletion, 2000);
            }
            
        } catch (e) {
            log('error_in_monitorTranscriptCompletion: ' + e.message);
            setTimeout(monitorTranscriptCompletion, 2000);
        }
    }
    
    // Send transcript to Android
    function sendTranscript(transcriptText) {
        if (transcriptText && transcriptText.trim().length >= 30) {
            log('sending_transcript_to_android length=' + transcriptText.trim().length);
            if (window.Android && window.Android.onTranscript) {
                try {
                    window.Android.onTranscript(transcriptText);
                    window.Android.onTranscriptComplete();
                    transcriptSent = true;
                    window.transcriptAlreadyProcessed = true;
                    log('transcript_sent_to_android');
                } catch (e) {
                    log('error_calling_onTranscript: ' + e.message);
                }
            } else {
                log('android_bridge_not_available');
            }
        } else {
            log('transcript_too_short length=' + (transcriptText ? transcriptText.trim().length : 0));
        }
        
        log('returned');
    }
    
    // Start the automation
    waitForPageReady();
    
})();
