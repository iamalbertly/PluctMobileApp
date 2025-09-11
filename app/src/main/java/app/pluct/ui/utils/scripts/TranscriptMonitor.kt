package app.pluct.ui.utils.scripts

/**
 * Transcript monitoring and extraction logic
 */
object TranscriptMonitor {
    
    fun getMonitoringScript(): String {
        return """
            // Monitor for transcript completion
            function monitorTranscriptCompletion() {
                try {
                    // Log HTML snapshot for debugging
                    const htmlSnapshot = document.documentElement.outerHTML;
                    const htmlLength = htmlSnapshot.length;
                    const htmlHead = htmlSnapshot.substring(0, 500).replace(/\\n/g, ' ').replace(/\\r/g, ' ');
                    log('html_snapshot len=' + htmlLength + ' head=' + htmlHead);
                    
                    // Check for error conditions first
                    const pageText = document.body ? document.body.innerText || document.body.textContent || '' : '';
                    
                    if (pageText.includes('Invalid URL')) {
                        log('invalid_url');
                        // Retry once by toggling trailing slash
                        const currentUrl = TARGET_URL.url;
                        const newUrl = currentUrl.endsWith('/') ? currentUrl.slice(0, -1) : currentUrl + '/';
                        log('retry_trailing_slash old=' + currentUrl + ' new=' + newUrl);
                        TARGET_URL.url = newUrl;
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
                        'iframe[src*="captcha"]',
                        'iframe[src*="recaptcha"]',
                        '.g-recaptcha',
                        '[class*="challenge"]',
                        '[data-testid*="challenge"]'
                    ];
                    
                    let captchaFound = false;
                    captchaSelectors.forEach(selector => {
                        if (document.querySelector(selector)) {
                            captchaFound = true;
                        }
                    });
                    
                    if (captchaFound) {
                        log('challenge_detected');
                        if (window.Android && window.Android.onError) {
                            try {
                                window.Android.onError('captcha_detected');
                            } catch (e) {
                                log('error_calling_onError: ' + e.message);
                            }
                        }
                        log('returned');
                        return;
                    }
                    
                    // Check for transcript result
                    const transcriptResult = findTranscriptResult();
                    if (transcriptResult) {
                        log('result_node_found');
                        
                        const transcriptText = extractTranscriptText(transcriptResult);
                        if (transcriptText && transcriptText.trim().length >= 30) {
                            log('transcript_element_candidate');
                            
                            // Try clipboard first, then direct extraction
                            try {
                                navigator.clipboard.writeText(transcriptText).then(() => {
                                    log('clipboard_write_ok');
                                    navigator.clipboard.readText().then(clipboardText => {
                                        if (clipboardText === transcriptText) {
                                            log('copied_length=' + transcriptText.trim().length);
                                            sendTranscript(transcriptText);
                                        } else {
                                            log('clipboard_mismatch');
                                            sendTranscript(transcriptText);
                                        }
                                    }).catch(() => {
                                        log('clipboard_read_failed: Failed to execute 'readText' on 'Clipboard': Document is not focused.');
                                        sendTranscript(transcriptText);
                                    });
                                }).catch(() => {
                                    log('clipboard_write_failed');
                                    sendTranscript(transcriptText);
                                });
                            } catch (e) {
                                log('clipboard_error: ' + e.message);
                                sendTranscript(transcriptText);
                            }
                            
                            return;
                        }
                        } else {
                            // Log waiting status every 10s
                            const nodeCount = document.querySelectorAll('*').length;
                            const elapsed = Date.now() - startTs;
                            
                            if (elapsed % 10000 < 1000) { // Every 10 seconds
                                log('still_waiting inflight=' + inflightCount + ' nodes=' + nodeCount);
                            }
                            
                            // HTML snapshot at 12-15s for debugging
                            if (elapsed > 12000 && elapsed < 15000) {
                                const htmlSnapshot = document.documentElement.outerHTML;
                                const htmlLength = htmlSnapshot.length;
                                const htmlHead = htmlSnapshot.substring(0, 500).replace(/\\n/g, ' ').replace(/\\r/g, ' ');
                                log('html_snapshot len=' + htmlLength + ' head=' + htmlHead);
                            }
                            
                            // Continue monitoring
                            setTimeout(monitorTranscriptCompletion, 1000);
                        }
                    
                } catch (e) {
                    log('error_in_monitorTranscriptCompletion: ' + e.message);
                    setTimeout(monitorTranscriptCompletion, 2000);
                }
            }
            
            // Find transcript result element with MutationObserver
            function findTranscriptResult() {
                const selectors = [
                    'pre',
                    '[data-testid*="transcript"]',
                    '[class*="transcript"]',
                    '[id*="transcript"]',
                    'code',
                    '.result',
                    '.mantine-*',
                    'article',
                    'section',
                    'div'
                ];
                
                let bestCandidate = null;
                let bestScore = 0;
                
                for (const selector of selectors) {
                    const elements = document.querySelectorAll(selector);
                    for (const element of elements) {
                        if (element && element.offsetParent !== null) {
                            const text = element.innerText || element.textContent || '';
                            const textLength = text.length;
                            
                            // Log each candidate
                            if (textLength > 30) {
                                const head = text.substring(0, 120).replace(/\\n/g, ' ').replace(/\\r/g, ' ');
                                log('transcript_element_candidate tag=' + element.tagName + ' len=' + textLength + ' head=' + head);
                                
                                // Score based on length and content indicators
                                let score = textLength;
                                if (text.includes('00:') || text.includes('[') || text.includes(']')) {
                                    score += 1000;
                                }
                                if (element.tagName === 'PRE' || element.tagName === 'CODE') {
                                    score += 500;
                                }
                                
                                if (score > bestScore) {
                                    bestScore = score;
                                    bestCandidate = element;
                                }
                            }
                        }
                    }
                }
                
                if (bestCandidate) {
                    log('result_node_found');
                }
                
                return bestCandidate;
            }
            
            // Extract transcript text
            function extractTranscriptText(element) {
                try {
                    return element.innerText || element.textContent || '';
                } catch (e) {
                    log('error_extracting_text: ' + e.message);
                    return '';
                }
            }
            
            // Send transcript to Android
            function sendTranscript(transcript) {
                try {
                    if (transcriptSent) {
                        log('transcript_already_sent');
                        return;
                    }
                    
                    transcriptSent = true;
                    window.transcriptAlreadyProcessed = true;
                    
                    log('sending_transcript length=' + transcript.length);
                    
                    if (window.Android && window.Android.onTranscript) {
                        try {
                            window.Android.onTranscript(transcript);
                            log('returned');
                            
                            // Call completion callback
                            if (window.Android.onTranscriptComplete) {
                                window.Android.onTranscriptComplete();
                            }
                        } catch (e) {
                            log('error_calling_onTranscript: ' + e.message);
                        }
                    } else {
                        log('android_bridge_not_available');
                    }
                    
                } catch (e) {
                    log('error_in_sendTranscript: ' + e.message);
                }
            }
        """.trimIndent()
    }
}
