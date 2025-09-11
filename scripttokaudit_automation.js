/**
 * ScriptTokAudit.io Complete Automation Script
 * 
 * This script automates the entire transcript generation workflow:
 * 1. Close any modal popups
 * 2. Fill the TikTok URL into the input field
 * 3. Click START button
 * 4. Wait for transcript generation
 * 5. Click COPY button to copy transcript to clipboard
 * 
 * Usage: Inject this script into the WebView after page load
 */

(function() {
    'use strict';
    
    console.log('ScriptTokAudit: Starting complete automation workflow');
    
    // Configuration
    const CONFIG = {
        POLLING_INTERVAL: 2000, // 2 seconds
        MAX_ATTEMPTS: 30, // 60 seconds total
        URL_INPUT_SELECTOR: 'textarea[placeholder="Enter Video Url"]',
        START_BUTTON_SELECTOR: 'button:has-text("START")',
        COPY_BUTTON_SELECTOR: 'button:has-text("Copy")',
        MODAL_SELECTORS: [
            '.bulk-download-modal-parent',
            '.modal',
            '[role="dialog"]',
            '.popup',
            '.modal-backdrop',
            '.overlay',
            '[class*="overlay"]'
        ]
    };
    
    // The TikTok URL to process (will be replaced by the app)
    const TIKTOK_URL = '{{TIKTOK_URL}}';
    
    /**
     * Close all modal popups and overlays
     */
    function closeAllModals() {
        console.log('ScriptTokAudit: Closing all modals and overlays');
        
        // Close bulk download modal specifically
        const bulkModal = document.querySelector('.bulk-download-modal-parent');
        if (bulkModal) {
            bulkModal.style.display = 'none';
            console.log('ScriptTokAudit: Closed bulk download modal');
        }
        
        // Close all other modals
        CONFIG.MODAL_SELECTORS.forEach(selector => {
            const elements = document.querySelectorAll(selector);
            elements.forEach(element => {
                if (element.style.display !== 'none') {
                    element.style.display = 'none';
                    console.log('ScriptTokAudit: Closed modal:', selector);
                }
            });
        });
        
        // Remove overlay backgrounds
        const overlays = document.querySelectorAll('.modal-backdrop, .overlay, [class*="overlay"]');
        overlays.forEach(overlay => {
            overlay.remove();
            console.log('ScriptTokAudit: Removed overlay');
        });
        
        // Click outside to close any remaining modals
        document.body.click();
        
        console.log('ScriptTokAudit: Modal closing completed');
    }
    
    /**
     * Normalize TikTok URL for optimal processing
     */
    function normalizeTikTokUrl(url) {
        console.log('ScriptTokAudit: Normalizing URL:', url);
        
        // Keep vm.tiktok.com URLs as-is (preferred format)
        if (url.includes('vm.tiktok.com')) {
            return url;
        }
        
        // Convert full tiktok.com URLs to vm.tiktok.com format
        if (url.includes('tiktok.com/@')) {
            const match = url.match(/tiktok\.com\/@[^\/]+\/video\/(\d+)/);
            if (match) {
                const videoId = match[1];
                const normalizedUrl = `https://vm.tiktok.com/${videoId}/`;
                console.log('ScriptTokAudit: Converted to vm.tiktok.com format:', normalizedUrl);
                return normalizedUrl;
            }
        }
        
        // Return original URL if no conversion possible
        return url;
    }
    
    /**
     * Properly simulate user input to prevent URL from disappearing
     */
    function simulateProperInput(element, text) {
        console.log('ScriptTokAudit: Simulating proper input for:', text);
        
        // Focus the element first
        element.focus();
        element.click();
        
        // Clear existing value
        element.value = '';
        element.dispatchEvent(new Event('input', { bubbles: true }));
        element.dispatchEvent(new Event('change', { bubbles: true }));
        
        // Use a more direct approach - set value and trigger events
        element.value = text;
        
        // Dispatch all necessary events
        element.dispatchEvent(new Event('input', { bubbles: true }));
        element.dispatchEvent(new Event('change', { bubbles: true }));
        element.dispatchEvent(new Event('keydown', { bubbles: true }));
        element.dispatchEvent(new Event('keypress', { bubbles: true }));
        element.dispatchEvent(new Event('keyup', { bubbles: true }));
        element.dispatchEvent(new Event('blur', { bubbles: true }));
        element.dispatchEvent(new Event('focus', { bubbles: true }));
        
        // Force the element to update its internal state
        if (element.setSelectionRange) {
            element.setSelectionRange(text.length, text.length);
        }
        
        console.log('ScriptTokAudit: Completed input simulation, value:', element.value);
    }
    
    /**
     * Fill the URL input field and click START
     */
    function fillAndSubmitUrl(url) {
        console.log('ScriptTokAudit: Filling and submitting URL:', url);
        
        // Find the textarea input field
        const textarea = document.querySelector(CONFIG.URL_INPUT_SELECTOR);
        if (!textarea) {
            throw new Error('Could not find URL input field');
        }
        
        // Use proper input simulation
        simulateProperInput(textarea, url);
        
        // Wait a bit and verify the URL was properly entered
        setTimeout(() => {
            console.log('ScriptTokAudit: URL filled, current value:', textarea.value);
            
            // Verify the URL was properly entered
            if (textarea.value !== url) {
                console.warn('ScriptTokAudit: URL not properly entered, retrying with different approach...');
                
                // Try a different approach - direct assignment
                textarea.value = url;
                textarea.dispatchEvent(new Event('input', { bubbles: true }));
                textarea.dispatchEvent(new Event('change', { bubbles: true }));
                
                setTimeout(() => {
                    console.log('ScriptTokAudit: After retry, value:', textarea.value);
                    clickStartButton();
                }, 500);
            } else {
                clickStartButton();
            }
        }, 500);
    }
    
    /**
     * Click the START button
     */
    function clickStartButton() {
        // Find and click the START button
        const startButton = document.querySelector(CONFIG.START_BUTTON_SELECTOR);
        if (!startButton) {
            throw new Error('Could not find START button');
        }
        
        startButton.click();
        console.log('ScriptTokAudit: Clicked START button');
        
        // Notify Android app that processing has started
        if (window.Android && window.Android.onProcessingUpdate) {
            window.Android.onProcessingUpdate(true);
        }
    }
    
    /**
     * Check for transcript results and handle different states
     */
    function checkForTranscriptResult() {
        const pageText = document.body.textContent || '';
        
        // Check for "Subtitles Not Available" (valid response)
        if (pageText.includes('Subtitles Not Available')) {
            console.log('ScriptTokAudit: Found: Subtitles Not Available');
            return { status: 'no_transcript', message: 'Subtitles Not Available' };
        }
        
        // Check for other "no transcript" variations
        const noTranscriptTexts = [
            'No transcript',
            'No subtitles',
            'Transcript not available',
            'No captions available'
        ];
        
        for (const text of noTranscriptTexts) {
            if (pageText.toLowerCase().includes(text.toLowerCase())) {
                console.log('ScriptTokAudit: No transcript available:', text);
                return { status: 'no_transcript', message: text };
            }
        }
        
        // Check for error messages
        const errorTexts = [
            'No valid tiktok data found',
            'Invalid URL',
            'Error processing',
            'Failed to process',
            'Something went wrong'
        ];
        
        for (const text of errorTexts) {
            if (pageText.toLowerCase().includes(text.toLowerCase())) {
                console.log('ScriptTokAudit: Found error:', text);
                return { status: 'error', message: text };
            }
        }
        
        // Look for success indicators (copy buttons, download buttons)
        const buttons = document.querySelectorAll('button');
        let foundSuccess = false;
        
        for (const button of buttons) {
            const buttonText = button.textContent || '';
            if (buttonText.toLowerCase().includes('copy') || 
                buttonText.toLowerCase().includes('download')) {
                foundSuccess = true;
                break;
            }
        }
        
        if (foundSuccess) {
            console.log('ScriptTokAudit: Found success indicators, extracting transcript');
            
            // Try to extract transcript text
            const transcriptSelectors = [
                '[class*="transcript"]',
                '[class*="subtitle"]',
                '[class*="text"]',
                '.result',
                '.output',
                '.content',
                'div'
            ];
            
            for (const selector of transcriptSelectors) {
                const elements = document.querySelectorAll(selector);
                for (const element of elements) {
                    const text = element.textContent || '';
                    const trimmedText = text.trim();
                    
                    if (trimmedText.length > 100 &&
                        !trimmedText.includes('script.tokaudit.io') &&
                        !trimmedText.includes('TikTok') &&
                        !trimmedText.includes('Transcript') &&
                        !trimmedText.includes('Generator') &&
                        !trimmedText.includes('About') &&
                        !trimmedText.includes('FAQ') &&
                        !trimmedText.includes('Download') &&
                        !trimmedText.includes('START') &&
                        !trimmedText.includes('Enter Video Url') &&
                        !trimmedText.includes('Copy') &&
                        !trimmedText.includes('copy')) {
                        
                        console.log('ScriptTokAudit: Found transcript:', trimmedText.substring(0, 100) + '...');
                        return { status: 'success', transcript: trimmedText };
                    }
                }
            }
            
            // Fallback: extract from page body
            const lines = pageText.split('\n').filter(line =>
                line.trim().length > 50 &&
                !line.includes('script.tokaudit.io') &&
                !line.includes('TikTok') &&
                !line.includes('Transcript') &&
                !line.includes('Generator') &&
                !line.includes('About') &&
                !line.includes('FAQ') &&
                !line.includes('Download') &&
                !line.includes('START') &&
                !line.includes('Enter Video Url') &&
                !line.includes('Copy') &&
                !line.includes('copy')
            );
            
            if (lines.length > 0) {
                const potentialTranscript = lines.join('\n').substring(0, 3000);
                console.log('ScriptTokAudit: Extracted transcript from page body');
                return { status: 'success', transcript: potentialTranscript };
            }
        }
        
        return { status: 'processing', message: 'Still processing...' };
    }
    
    /**
     * Click the COPY button to copy transcript to clipboard
     */
    function copyTranscriptToClipboard() {
        console.log('ScriptTokAudit: Attempting to copy transcript to clipboard');
        
        const copyButton = document.querySelector(CONFIG.COPY_BUTTON_SELECTOR);
        if (!copyButton) {
            throw new Error('Could not find COPY button');
        }
        
        copyButton.click();
        console.log('ScriptTokAudit: Clicked COPY button');
        
        // Wait a moment for copy to complete
        setTimeout(() => {
            console.log('ScriptTokAudit: Transcript copied to clipboard');
        }, 1000);
        
        return true;
    }
    
    /**
     * Main automation workflow
     */
    async function runAutomation() {
        try {
            console.log('ScriptTokAudit: Starting automation workflow');
            
            // Step 1: Close all modals
            closeAllModals();
            
            // Step 2: Wait for page to settle
            await new Promise(resolve => setTimeout(resolve, 1000));
            
            // Step 3: Normalize the URL
            const normalizedUrl = normalizeTikTokUrl(TIKTOK_URL);
            
            // Step 4: Fill and submit the URL
            fillAndSubmitUrl(normalizedUrl);
            
            // Step 5: Monitor for results
            let attempts = 0;
            let result = null;
            
            const checkInterval = setInterval(() => {
                attempts++;
                console.log(`ScriptTokAudit: Checking for results, attempt ${attempts}/${CONFIG.MAX_ATTEMPTS}`);
                
                result = checkForTranscriptResult();
                
                if (result.status === 'success') {
                    clearInterval(checkInterval);
                    console.log('ScriptTokAudit: Success! Transcript found');
                    
                    // Notify Android app that processing is complete
                    if (window.Android && window.Android.onProcessingUpdate) {
                        window.Android.onProcessingUpdate(false);
                    }
                    
                    // Step 6: Copy transcript to clipboard
                    setTimeout(() => {
                        try {
                            copyTranscriptToClipboard();
                            console.log('ScriptTokAudit: Automation completed successfully');
                            
                            // Notify Android app of success
                            if (window.Android && window.Android.onTranscriptReceived) {
                                window.Android.onTranscriptReceived(result.transcript);
                            }
                        } catch (error) {
                            console.error('ScriptTokAudit: Error copying transcript:', error);
                            if (window.Android && window.Android.onError) {
                                window.Android.onError('Failed to copy transcript: ' + error.message);
                            }
                        }
                    }, 1000);
                    
                } else if (result.status === 'no_transcript') {
                    clearInterval(checkInterval);
                    console.log('ScriptTokAudit: No transcript available');
                    
                    // Notify Android app that processing is complete
                    if (window.Android && window.Android.onProcessingUpdate) {
                        window.Android.onProcessingUpdate(false);
                    }
                    
                    // Notify Android app of no transcript
                    if (window.Android && window.Android.onNoTranscript) {
                        window.Android.onNoTranscript();
                    }
                    
                } else if (result.status === 'error') {
                    clearInterval(checkInterval);
                    console.error('ScriptTokAudit: Error occurred:', result.message);
                    
                    // Notify Android app that processing is complete
                    if (window.Android && window.Android.onProcessingUpdate) {
                        window.Android.onProcessingUpdate(false);
                    }
                    
                    // Notify Android app of error
                    if (window.Android && window.Android.onError) {
                        window.Android.onError('Error: ' + result.message);
                    }
                    
                } else if (attempts >= CONFIG.MAX_ATTEMPTS) {
                    clearInterval(checkInterval);
                    console.error('ScriptTokAudit: Timeout waiting for results');
                    
                    // Notify Android app that processing is complete
                    if (window.Android && window.Android.onProcessingUpdate) {
                        window.Android.onProcessingUpdate(false);
                    }
                    
                    // Notify Android app of timeout
                    if (window.Android && window.Android.onError) {
                        window.Android.onError('Timeout waiting for transcript results');
                    }
                }
            }, CONFIG.POLLING_INTERVAL);
            
        } catch (error) {
            console.error('ScriptTokAudit: Automation failed:', error);
            
            // Notify Android app that processing is complete
            if (window.Android && window.Android.onProcessingUpdate) {
                window.Android.onProcessingUpdate(false);
            }
            
            // Notify Android app of error
            if (window.Android && window.Android.onError) {
                window.Android.onError('Automation failed: ' + error.message);
            }
        }
    }
    
    // Start the automation
    runAutomation();
    
})();
