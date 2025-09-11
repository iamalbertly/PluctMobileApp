package app.pluct.ui.utils.scripts

/**
 * Builds manual mode scripts for WebView
 */
object ManualModeScriptBuilder {
    
    fun buildScript(runId: String): String {
        return """
            (function(){
                const RUN_ID = '$runId';
                let monitoringActive = false;
                
                function log(msg) {
                    console.log('WV:J:MANUAL:' + msg + ' run=' + RUN_ID);
                }
                
                function startMonitoring() {
                    if (monitoringActive) {
                        log('monitoring_already_active');
                        return;
                    }
                    
                    monitoringActive = true;
                    log('starting_manual_monitoring');
                    
                    // Monitor for transcript completion
                    function checkForTranscript() {
                        try {
                            // Look for copy button or transcript text
                            const copyButtonSelectors = [
                                'button:contains("Copy")',
                                'button:contains("copy")',
                                'button[aria-label*="copy"]',
                                'button[aria-label*="Copy"]',
                                '[data-testid*="copy"]',
                                'button[title*="copy"]',
                                'button[title*="Copy"]'
                            ];
                            
                            const transcriptSelectors = [
                                '[data-testid*="transcript"]',
                                '[class*="transcript"]',
                                '[id*="transcript"]',
                                'textarea[readonly]',
                                'div[contenteditable="false"]'
                            ];
                            
                            let copyButton = null;
                            let transcriptElement = null;
                            
                            // Find copy button
                            for (const selector of copyButtonSelectors) {
                                if (selector.includes(':contains')) {
                                    const baseSelector = selector.split(':contains')[0];
                                    const text = selector.match(/:contains\("([^"]+)"\)/)?.[1];
                                    if (text) {
                                        const buttons = document.querySelectorAll(baseSelector);
                                        for (const button of buttons) {
                                            if (button.textContent && button.textContent.toLowerCase().includes(text.toLowerCase())) {
                                                copyButton = button;
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    copyButton = document.querySelector(selector);
                                }
                                
                                if (copyButton && copyButton.offsetParent !== null) {
                                    log('found_copy_button: ' + selector);
                                    break;
                                }
                            }
                            
                            // Find transcript element
                            for (const selector of transcriptSelectors) {
                                transcriptElement = document.querySelector(selector);
                                if (transcriptElement && transcriptElement.offsetParent !== null) {
                                    log('found_transcript_element: ' + selector);
                                    break;
                                }
                            }
                            
                            if (copyButton || transcriptElement) {
                                log('transcript_ready_detected');
                                
                                if (copyButton) {
                                    // Click copy button to get transcript
                                    copyButton.click();
                                    log('copy_button_clicked');
                                    
                                    // Wait a bit for clipboard to be populated
                                    setTimeout(() => {
                                        if (window.Android && window.Android.getClipboardContent) {
                                            const clipboardContent = window.Android.getClipboardContent();
                                            if (clipboardContent && clipboardContent.trim().length > 0) {
                                                log('transcript_copied_from_clipboard');
                                                if (window.Android && window.Android.onTranscriptReceived) {
                                                    window.Android.onTranscriptReceived(clipboardContent);
                                                }
                                            }
                                        }
                                    }, 1000);
                                } else if (transcriptElement) {
                                    // Get transcript directly from element
                                    const transcriptText = transcriptElement.textContent || transcriptElement.value || '';
                                    if (transcriptText.trim().length > 0) {
                                        log('transcript_extracted_from_element');
                                        if (window.Android && window.Android.onTranscriptReceived) {
                                            window.Android.onTranscriptReceived(transcriptText);
                                        }
                                    }
                                }
                                
                                monitoringActive = false;
                                return;
                            }
                            
                            // Continue monitoring
                            setTimeout(checkForTranscript, 2000);
                            
                        } catch (e) {
                            log('error_in_checkForTranscript: ' + e.message);
                            setTimeout(checkForTranscript, 2000);
                        }
                    }
                    
                    // Start monitoring
                    checkForTranscript();
                }
                
                // Start monitoring when page is ready
                if (document.readyState === 'complete') {
                    startMonitoring();
                } else {
                    window.addEventListener('load', startMonitoring);
                }
                
                log('manual_mode_script_loaded');
                
            })();
        """.trimIndent()
    }
}
