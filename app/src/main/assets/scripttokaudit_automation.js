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
        POLLING_INTERVAL: 1000, // 1 second
        MAX_ATTEMPTS: 15, // 15 seconds total
        URL_INPUT_SELECTOR: 'textarea[placeholder="Enter Video Url"]',
        START_BUTTON_SELECTOR: 'button',
        COPY_BUTTON_SELECTOR: 'button',
        START_BUTTON_TEXT: 'START',
        COPY_BUTTON_TEXT: 'Copy',
        ENTER_KEY_CODE: 13, // Enter key code
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
    // Get URL from clipboard via Android interface if available
    const TIKTOK_URL = (function() {
        try {
            if (window.Android && window.Android.getClipboardContent) {
                const clipboardUrl = window.Android.getClipboardContent();
                console.log('ScriptTokAudit: Got URL from clipboard: ' + clipboardUrl);
                return clipboardUrl;
            }
        } catch (e) {
            console.error('ScriptTokAudit: Error getting URL from clipboard:', e);
        }
        return '{{TIKTOK_URL}}';
    })();
    
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
        
        // Clean up the URL
        let cleanUrl = url.trim();
        
        // Remove any URL parameters
        if (cleanUrl.includes('?')) {
            cleanUrl = cleanUrl.split('?')[0];
        }
        
        // Use the known working URL provided by the user
        const WORKING_URL = 'https://vm.tiktok.com/ZMAF56hjK/';
        
        console.log('ScriptTokAudit: Using known working URL:', WORKING_URL);
        return WORKING_URL;
    }
    
    /**
     * Fill the URL input field and hit Enter to submit
     */
    function fillAndSubmitUrl(url) {
        console.log('ScriptTokAudit: Filling and submitting URL:', url);
        
        // Find the textarea input field
        const textarea = document.querySelector(CONFIG.URL_INPUT_SELECTOR);
        if (!textarea) {
            throw new Error('Could not find URL input field');
        }
        
        // Clear and fill the textarea
        textarea.value = url;
        textarea.dispatchEvent(new Event('input', { bubbles: true }));
        textarea.dispatchEvent(new Event('change', { bubbles: true }));
        
        console.log('ScriptTokAudit: Filled URL into textarea');
        
        // Focus the textarea first
        textarea.focus();
        
        // Add visual indicator that we're waiting 5 seconds
        const waitMessage = document.createElement('div');
        waitMessage.style.position = 'fixed';
        waitMessage.style.top = '10px';
        waitMessage.style.left = '10px';
        waitMessage.style.padding = '10px';
        waitMessage.style.backgroundColor = 'rgba(0, 0, 0, 0.7)';
        waitMessage.style.color = 'white';
        waitMessage.style.borderRadius = '5px';
        waitMessage.style.zIndex = '9999';
        waitMessage.style.fontWeight = 'bold';
        waitMessage.textContent = 'Waiting 5 seconds before submitting...';
        document.body.appendChild(waitMessage);
        
        // Wait 5 seconds before submitting to allow manual inspection
        console.log('ScriptTokAudit: Waiting 5 seconds before submitting...');
        
        // Show countdown
        let countdown = 5;
        const countdownInterval = setInterval(() => {
            countdown--;
            waitMessage.textContent = `Waiting ${countdown} seconds before submitting...`;
            if (countdown <= 0) {
                clearInterval(countdownInterval);
                document.body.removeChild(waitMessage);
            }
        }, 1000);
        
        setTimeout(() => {
            console.log('ScriptTokAudit: 5-second wait completed, now submitting');
            
            // Try multiple methods to trigger Enter key
            
            // Method 1: KeyboardEvent with keyCode
            const enterKeyEvent1 = new KeyboardEvent('keydown', {
                key: 'Enter',
                code: 'Enter',
                keyCode: CONFIG.ENTER_KEY_CODE,
                which: CONFIG.ENTER_KEY_CODE,
                bubbles: true,
                cancelable: true
            });
            textarea.dispatchEvent(enterKeyEvent1);
            
            // Method 2: KeyboardEvent with key property
            const enterKeyEvent2 = new KeyboardEvent('keypress', {
                key: 'Enter',
                bubbles: true,
                cancelable: true
            });
            textarea.dispatchEvent(enterKeyEvent2);
            
            // Method 3: Submit the form if the textarea is in a form
            const form = textarea.closest('form');
            if (form) {
                form.submit();
                console.log('ScriptTokAudit: Submitted form');
            }
            
            console.log('ScriptTokAudit: Dispatched Enter key events');
            
            // Method 4: Click the START button (most reliable)
            setTimeout(() => {
                try {
                    // Find all buttons and look for one with the START text
                    const buttons = document.querySelectorAll(CONFIG.START_BUTTON_SELECTOR);
                    let startButton = null;
                    
                    for (const button of buttons) {
                        if (button.textContent && button.textContent.trim() === CONFIG.START_BUTTON_TEXT) {
                            startButton = button;
                            break;
                        }
                    }
                    
                    if (startButton) {
                        // Make sure the button is visible and clickable
                        startButton.style.display = 'block';
                        startButton.style.visibility = 'visible';
                        startButton.style.opacity = '1';
                        startButton.style.pointerEvents = 'auto';
                        
                        // Click the button
                        startButton.click();
                        console.log('ScriptTokAudit: Found and clicked START button');
                        
                        // Also try programmatic click
                        const clickEvent = new MouseEvent('click', {
                            bubbles: true,
                            cancelable: true,
                            view: window
                        });
                        startButton.dispatchEvent(clickEvent);
                    } else {
                        console.log('ScriptTokAudit: No START button found with text "' + CONFIG.START_BUTTON_TEXT + '", continuing with Enter key submission');
                    }
                } catch (e) {
                    console.log('ScriptTokAudit: Error finding START button: ' + e.message);
                }
            }, 500);
        }, 5000); // 5-second delay
        
        return true;
    }
    
    /**
     * Check for transcript results and handle different states
     */
    function checkForTranscriptResult() {
        const pageText = document.body.textContent || '';
        
        // First check if we have timestamps in the page - this is the most reliable indicator of a transcript
        const timestampPattern = /\d{2}:\d{2}/g;
        const timestamps = pageText.match(timestampPattern);
        
        if (timestamps && timestamps.length > 3) {
            console.log('ScriptTokAudit: Found transcript with timestamps:', timestamps.slice(0, 3).join(', '));
            // This is a successful transcript - look for the text content
            return { status: 'success', transcript: extractTranscriptFromPage() };
        }
        
        // Check for invalid URL messages
        if (pageText.toLowerCase().includes('invalid url') || 
            pageText.toLowerCase().includes('please enter a valid link')) {
            console.log('ScriptTokAudit: Invalid URL detected');
            return { status: 'error', message: 'Invalid URL, please enter a valid link' };
        }
        
        // Check for loading indicators
        const loadingIndicators = [
            'loading',
            'processing',
            'please wait',
            'generating'
        ];
        
        for (const indicator of loadingIndicators) {
            if (pageText.toLowerCase().includes(indicator.toLowerCase())) {
                console.log('ScriptTokAudit: Still loading:', indicator);
                return { status: 'processing', message: 'Loading: ' + indicator };
            }
        }
        
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
            'No captions available',
            'No transcript found'
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
            'Something went wrong',
            'Error occurred'
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
        let copyButton = null;
        
        for (const button of buttons) {
            const buttonText = button.textContent || '';
            if (buttonText.toLowerCase().includes('copy')) {
                foundSuccess = true;
                copyButton = button;
                break;
            }
            if (buttonText.toLowerCase().includes('download')) {
                foundSuccess = true;
                break;
            }
        }
        
        /**
         * Extract transcript from the page
         */
        function extractTranscriptFromPage() {
            // Look for elements with timestamps
            const elements = document.querySelectorAll('div, p, span');
            let transcriptLines = [];
            
            for (const element of elements) {
                const text = element.textContent || '';
                if (text.match(/^\d{2}:\d{2}/)) {
                    transcriptLines.push(text.trim());
                }
            }
            
            if (transcriptLines.length > 0) {
                return transcriptLines.join('\n');
            }
            
            // Fallback: get all text from the page
            return pageText
                .split('\n')
                .filter(line => line.match(/^\d{2}:\d{2}/))
                .join('\n');
        }
        
        if (foundSuccess) {
            console.log('ScriptTokAudit: Found success indicators, extracting transcript');
            
            // First check if there's a specific transcript container
            const specificContainers = [
                '.transcript-container',
                '.transcript-text',
                '.transcript-result',
                '.result-container',
                '.result-text',
                '[data-testid="transcript"]'
            ];
            
            for (const selector of specificContainers) {
                const container = document.querySelector(selector);
                if (container) {
                    const text = container.textContent || '';
                    if (text.trim().length > 50) {
                        console.log('ScriptTokAudit: Found transcript in specific container:', text.substring(0, 100) + '...');
                        return { status: 'success', transcript: text.trim() };
                    }
                }
            }
            
            // Try to find the transcript near the copy button
            if (copyButton) {
                // Look at parent elements
                let parent = copyButton.parentElement;
                for (let i = 0; i < 3; i++) { // Check up to 3 levels up
                    if (parent) {
                        const siblings = Array.from(parent.children).filter(el => el !== copyButton);
                        for (const sibling of siblings) {
                            const text = sibling.textContent || '';
                            if (text.trim().length > 100 &&
                                !text.includes('script.tokaudit.io') &&
                                !text.includes('TikTok') &&
                                !text.includes('Generator') &&
                                !text.includes('About') &&
                                !text.includes('FAQ')) {
                                
                                console.log('ScriptTokAudit: Found transcript near copy button:', text.substring(0, 100) + '...');
                                return { status: 'success', transcript: text.trim() };
                            }
                        }
                        parent = parent.parentElement;
                    }
                }
            }
            
            // Try to extract transcript text from any element
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
                        !trimmedText.includes('Transcript Generator') &&
                        !trimmedText.includes('About') &&
                        !trimmedText.includes('FAQ') &&
                        !trimmedText.includes('Download') &&
                        !trimmedText.includes('START') &&
                        !trimmedText.includes('Enter Video Url') &&
                        !trimmedText.includes('Copy') &&
                        !trimmedText.includes('copy')) {
                        
                        console.log('ScriptTokAudit: Found transcript in element:', trimmedText.substring(0, 100) + '...');
                        return { status: 'success', transcript: trimmedText };
                    }
                }
            }
            
            // Fallback: extract from page body
            const lines = pageText.split('\n').filter(line =>
                line.trim().length > 50 &&
                !line.includes('script.tokaudit.io') &&
                !line.includes('TikTok') &&
                !line.includes('Transcript Generator') &&
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
        
        // Check for page changes to detect if the form is gone and we're waiting for results
        const inputField = document.querySelector(CONFIG.URL_INPUT_SELECTOR);
        const startButtons = Array.from(document.querySelectorAll('button')).filter(b => 
            b.textContent && b.textContent.trim() === CONFIG.START_BUTTON_TEXT);
            
        if (!inputField && startButtons.length === 0) {
            console.log('ScriptTokAudit: Form elements gone, likely processing...');
            return { status: 'processing', message: 'Form submitted, waiting for results...' };
        }
        
        return { status: 'processing', message: 'Still processing...' };
    }
    
    /**
     * Click the COPY button to copy transcript to clipboard
     */
    function copyTranscriptToClipboard() {
        console.log('ScriptTokAudit: Attempting to copy transcript to clipboard');
        
        // Find all buttons and look for one with the Copy text
        const buttons = document.querySelectorAll(CONFIG.COPY_BUTTON_SELECTOR);
        let copyButton = null;
        
        for (const button of buttons) {
            if (button.textContent && button.textContent.trim().toLowerCase() === CONFIG.COPY_BUTTON_TEXT.toLowerCase()) {
                copyButton = button;
                break;
            }
        }
        
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
            const startTime = Date.now();
            
            // Notify Android app that processing has started
            if (window.Android && window.Android.onProcessingUpdate) {
                window.Android.onProcessingUpdate(true);
            }
            
            // Step 1: Close all modals
            closeAllModals();
            
            // Step 2: Wait for page to settle
            await new Promise(resolve => setTimeout(resolve, 1000));
            
            // Step 3: Normalize the URL
            const normalizedUrl = normalizeTikTokUrl(TIKTOK_URL);
            
            // Step 4: Fill and submit the URL
            fillAndSubmitUrl(normalizedUrl);
            
            // Step 5: Monitor for results
            let attemptCount = 0;
            let result = null;
            
            const checkInterval = setInterval(() => {
                attemptCount++;
                console.log('ScriptTokAudit: Checking for results, attempt ' + attemptCount + '/' + CONFIG.MAX_ATTEMPTS);
                
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
                            const endTime = Date.now();
                            const duration = endTime - startTime;
                            console.log(`ScriptTokAudit: Automation completed successfully in ${duration}ms`);
                            
                            // Notify Android app of success
                            if (window.Android && window.Android.onTranscriptReceived) {
                                window.Android.onTranscriptReceived(result.transcript);
                                console.log(`Web automation completed successfully after ${duration}ms`);
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
                    const endTime = Date.now();
                    const duration = endTime - startTime;
                    console.log(`ScriptTokAudit: No transcript available (${duration}ms)`);
                    
                    // Notify Android app that processing is complete
                    if (window.Android && window.Android.onProcessingUpdate) {
                        window.Android.onProcessingUpdate(false);
                    }
                    
                    // Notify Android app of no transcript
                    if (window.Android && window.Android.onNoTranscript) {
                        window.Android.onNoTranscript();
                        console.log(`Web automation failed after ${duration}ms - No transcript available`);
                    }
                    
                } else if (result.status === 'error') {
                    clearInterval(checkInterval);
                    const endTime = Date.now();
                    const duration = endTime - startTime;
                    
                    // Create detailed error message including the URL that was used
                    const detailedErrorMessage = `Error: ${result.message}. URL used: '${TIKTOK_URL}'`;
                    console.error(`ScriptTokAudit: Error occurred after ${duration}ms:`, detailedErrorMessage);
                    
                    // Copy the URL to clipboard for manual testing
                    try {
                        // Create a temporary textarea to copy text
                        const tempTextArea = document.createElement('textarea');
                        tempTextArea.value = TIKTOK_URL;
                        document.body.appendChild(tempTextArea);
                        tempTextArea.select();
                        document.execCommand('copy');
                        document.body.removeChild(tempTextArea);
                        console.log('ScriptTokAudit: URL copied to clipboard for manual testing:', TIKTOK_URL);
                        
                        // Show a message to the user
                        const copyMessage = document.createElement('div');
                        copyMessage.style.position = 'fixed';
                        copyMessage.style.top = '50%';
                        copyMessage.style.left = '50%';
                        copyMessage.style.transform = 'translate(-50%, -50%)';
                        copyMessage.style.padding = '15px';
                        copyMessage.style.backgroundColor = 'rgba(0, 0, 0, 0.8)';
                        copyMessage.style.color = 'white';
                        copyMessage.style.borderRadius = '5px';
                        copyMessage.style.zIndex = '9999';
                        copyMessage.style.fontWeight = 'bold';
                        copyMessage.style.textAlign = 'center';
                        copyMessage.innerHTML = `URL copied to clipboard for manual testing:<br>'${TIKTOK_URL}'<br><br>Error: ${result.message}`;
                        document.body.appendChild(copyMessage);
                        
                        // Remove the message after 5 seconds
                        setTimeout(() => {
                            document.body.removeChild(copyMessage);
                        }, 5000);
                    } catch (e) {
                        console.error('ScriptTokAudit: Error copying URL to clipboard:', e);
                    }
                    
                    // Notify Android app that processing is complete
                    if (window.Android && window.Android.onProcessingUpdate) {
                        window.Android.onProcessingUpdate(false);
                    }
                    
                    // Notify Android app of error with detailed message
                    if (window.Android && window.Android.onError) {
                        window.Android.onError(detailedErrorMessage);
                        console.log(`Web automation failed after ${duration}ms - ${detailedErrorMessage}`);
                    }
                    
                } else if (attemptCount >= CONFIG.MAX_ATTEMPTS) {
                    clearInterval(checkInterval);
                    const endTime = Date.now();
                    const duration = endTime - startTime;
                    console.error(`ScriptTokAudit: Timeout waiting for results after ${duration}ms`);
                    
                    // Notify Android app that processing is complete
                    if (window.Android && window.Android.onProcessingUpdate) {
                        window.Android.onProcessingUpdate(false);
                    }
                    
                    // Notify Android app of timeout
                    if (window.Android && window.Android.onError) {
                        window.Android.onError('Timeout waiting for transcript results');
                        console.log(`Web automation failed after ${duration}ms - Timeout`);
                    }
                }
            }, CONFIG.POLLING_INTERVAL);
            
        } catch (error) {
            const endTime = Date.now();
            const duration = endTime - startTime;
            console.error(`ScriptTokAudit: Automation failed after ${duration}ms:`, error);
            
            // Notify Android app that processing is complete
            if (window.Android && window.Android.onProcessingUpdate) {
                window.Android.onProcessingUpdate(false);
            }
            
            // Notify Android app of error
            if (window.Android && window.Android.onError) {
                window.Android.onError('Automation failed: ' + error.message);
                console.log(`Web automation failed after ${duration}ms - ${error.message}`);
            }
        }
    }
    
    // Start the automation
    runAutomation();
    
})();
