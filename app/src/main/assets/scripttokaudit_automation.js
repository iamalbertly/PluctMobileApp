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
    
    if (window.__PLUCT_AUTOMATION_RUNNING__) {
        console.log('ScriptTokAudit: Automation already running, skipping duplicate');
        return;
    }
    window.__PLUCT_AUTOMATION_RUNNING__ = true;
    
    console.log('ScriptTokAudit: Starting complete automation workflow');
    
    const CONFIG = {
        POLLING_INTERVAL: 2000,
        MAX_ATTEMPTS: 30,
        URL_INPUT_SELECTORS: [
            'textarea[placeholder="Enter Video Url"]',
            'textarea[placeholder*="Video"]',
            'textarea'
        ]
    };
    
    // Always use the vm.tiktok.com URL path as requested
    const TIKTOK_URL = '{{TIKTOK_URL}}';
    let __processingStarted = false;
    let __intervalId = null;
    
    function safeAndroidCall(fnName, ...args) {
        try { if (window.Android && typeof window.Android[fnName] === 'function') { window.Android[fnName](...args); } }
        catch (e) { console.log('ScriptTokAudit: Android call failed', fnName, e && e.message); }
    }
    
    function waitForElement(selector, timeout = 10000) {
        return new Promise((resolve, reject) => {
            const startTime = Date.now();
            
            const checkElement = () => {
                const element = document.querySelector(selector);
                if (element) {
                    resolve(element);
                    return;
                }
                
                if (Date.now() - startTime > timeout) {
                    reject(new Error(`Timeout waiting for element: ${selector}`));
                    return;
                }
                
                setTimeout(checkElement, 100);
            };
            
            checkElement();
        });
    }
    
    function findUrlInput() {
        for (const sel of CONFIG.URL_INPUT_SELECTORS) {
            const el = document.querySelector(sel);
            if (el) { console.log('ScriptTokAudit: Found URL input via selector:', sel); return el; }
        }
        return null;
    }
    
    function nativeSetValue(el, value) {
        const proto = Object.getPrototypeOf(el);
        const desc = Object.getOwnPropertyDescriptor(proto, 'value');
        if (desc && desc.set) {
            desc.set.call(el, value);
        } else {
            el.value = value;
        }
    }
    
    function simulatePaste(el, text) {
        try {
            nativeSetValue(el, '');
            el.dispatchEvent(new InputEvent('input', { bubbles: true, inputType: 'deleteContentBackward' }));
            nativeSetValue(el, text);
            el.dispatchEvent(new InputEvent('input', { bubbles: true, inputType: 'insertFromPaste', data: text }));
            el.dispatchEvent(new Event('change', { bubbles: true }));
        } catch (e) {
            // Fallback
            el.value = text;
            el.dispatchEvent(new Event('input', { bubbles: true }));
            el.dispatchEvent(new Event('change', { bubbles: true }));
        }
    }
    
    async function ensureValueSticks(el, url) {
        // Focus then blur to emulate user interaction and detect clearing
        el.focus();
        el.click();
        await new Promise(r => setTimeout(r, 150));
        let current = (el.value || '').trim();
        console.log('ScriptTokAudit: After focus, current value:', current);
        if (!current) {
            console.log('ScriptTokAudit: Value cleared on focus, restoring via native setter');
            simulatePaste(el, url);
            current = (el.value || '').trim();
            console.log('ScriptTokAudit: Restored value now:', current);
        }
        // Additional blur/focus cycle to verify retention
        el.blur();
        await new Promise(r => setTimeout(r, 120));
        el.focus();
        await new Promise(r => setTimeout(r, 120));
        current = (el.value || '').trim();
        console.log('ScriptTokAudit: Re-validated after focus cycle, value:', current);
        if (!current) {
            simulatePaste(el, url);
            console.log('ScriptTokAudit: Re-applied paste after re-validation. New value:', (el.value || '').trim());
        }
    }
    
    function clickStartButton() {
        const buttons = Array.from(document.querySelectorAll('button'));
        const startButton = buttons.find(b => (b.textContent || '').trim().toUpperCase().includes('START'))
                           || buttons.find(b => (b.textContent || '').trim().toUpperCase().includes('SUBMIT'));
        if (!startButton) throw new Error('Could not find START button');
        __processingStarted = true;
        startButton.click();
        console.log('ScriptTokAudit: Clicked START button');
        safeAndroidCall('onProcessingUpdate', true);
    }
    
    async function fillAndSubmitUrl(url) {
        console.log('ScriptTokAudit: Filling and submitting URL:', url);
        
        // Wait for the URL input field to be available
        let textarea;
        try {
            textarea = await waitForElement('textarea[placeholder="Enter Video Url"]', 15000);
        } catch (e) {
            console.log('ScriptTokAudit: Could not find URL input field, trying alternative selectors');
            textarea = findUrlInput();
        }
        
        if (!textarea) {
            throw new Error('Could not find URL input field after waiting');
        }
        
        simulatePaste(textarea, url);
        await ensureValueSticks(textarea, url);
        
        const current = (textarea.value || '').trim();
        console.log('ScriptTokAudit: Final pre-submit value:', current);
        if (current && (current === url || current === url.replace(/\/$/, ''))) {
            clickStartButton();
        } else {
            // One more forced assignment before clicking
            console.log('ScriptTokAudit: Forcing assignment before START');
            simulatePaste(textarea, url);
            console.log('ScriptTokAudit: Forced value:', (textarea.value || '').trim());
            clickStartButton();
        }
    }
    
    function checkForTranscriptResult() {
        const pageText = document.body.textContent || '';
        
        // Check for error conditions first
        if (/invalid url|no valid tiktok data/i.test(pageText)) {
            return { status: 'error', message: 'Invalid URL or no valid TikTok data found' };
        }
        
        // Look for copy button - this indicates transcript is ready
        const copyButtons = Array.from(document.querySelectorAll('button')).filter(b => 
            /copy/i.test(b.textContent || '') && !/copy.*link/i.test(b.textContent || '')
        );
        
        if (copyButtons.length > 0) {
            console.log('ScriptTokAudit: Found copy button(s):', copyButtons.map(b => b.textContent));
            return { status: 'ready_to_copy', copyButton: copyButtons[0] };
        }
        
        // Check if still processing
        const processingIndicators = Array.from(document.querySelectorAll('*')).filter(el => {
            const text = el.textContent || '';
            return /processing|loading|generating|please wait/i.test(text);
        });
        
        if (processingIndicators.length > 0) {
            return { status: 'processing', message: 'Still processing transcript...' };
        }
        
        return { status: 'waiting', message: 'Waiting for transcript to be ready...' };
    }
    
    function clickCopyButton() {
        const copyButtons = Array.from(document.querySelectorAll('button')).filter(b => 
            /copy/i.test(b.textContent || '') && !/copy.*link/i.test(b.textContent || '')
        );
        
        if (copyButtons.length === 0) {
            throw new Error('Could not find COPY button for transcript');
        }
        
        const copyButton = copyButtons[0];
        console.log('ScriptTokAudit: Clicking copy button:', copyButton.textContent);
        copyButton.click();
        
        // Wait a moment for the copy to complete
        setTimeout(() => {
            console.log('ScriptTokAudit: Copy button clicked, transcript should be in clipboard');
        }, 500);
        
        return true;
    }
    
    function extractTranscriptFromPage() {
        console.log('ScriptTokAudit: Attempting to extract transcript from page...');
        
        // Wait a bit more for AJAX content to fully load
        return new Promise((resolve) => {
            setTimeout(() => {
                // First try the specific selector the user mentioned
                const specificSelector = '/html/body/div[1]/div[1]/div[2]/div[2]/div/div/div/div[2]';
                try {
                    const specificElement = document.evaluate(specificSelector, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
                    if (specificElement) {
                        const text = (specificElement.value || specificElement.textContent || '').trim();
                        if (text && text.length > 10 && !isWebsiteText(text)) {
                            console.log('ScriptTokAudit: Found transcript via specific XPath selector');
                            resolve(text);
                            return;
                        }
                    }
                } catch (e) {
                    console.log('ScriptTokAudit: XPath selector failed:', e.message);
                }
                
                // Look for transcript content in the main result area only
                const mainResultArea = document.querySelector('.result-container, .transcript-container, .subtitle-container, [class*="result"], [class*="transcript"], [class*="subtitle"]');
                if (mainResultArea) {
                    const text = (mainResultArea.value || mainResultArea.textContent || '').trim();
                    if (text && text.length > 10 && !isWebsiteText(text)) {
                        console.log('ScriptTokAudit: Found transcript in main result area');
                        resolve(text);
                        return;
                    }
                }
                
                // Look for readonly textarea (common for transcript display)
                const readonlyTextarea = document.querySelector('textarea[readonly]');
                if (readonlyTextarea) {
                    const text = readonlyTextarea.value.trim();
                    if (text && text.length > 10 && !isWebsiteText(text)) {
                        console.log('ScriptTokAudit: Found transcript in readonly textarea');
                        resolve(text);
                        return;
                    }
                }
                
                // Look for pre-formatted text (common for transcript display)
                const preElement = document.querySelector('pre');
                if (preElement) {
                    const text = preElement.textContent.trim();
                    if (text && text.length > 10 && !isWebsiteText(text)) {
                        console.log('ScriptTokAudit: Found transcript in pre element');
                        resolve(text);
                        return;
                    }
                }
                
                // If no transcript found, check if it's explicitly "Subtitles Not Available"
                const pageText = document.body.textContent || '';
                if (/subtitles not available/i.test(pageText)) {
                    console.log('ScriptTokAudit: Confirmed "Subtitles Not Available"');
                    resolve(null); // Signal no transcript available
                    return;
                }
                
                console.log('ScriptTokAudit: No valid transcript content found');
                resolve(null);
            }, 3000); // Wait 3 seconds for AJAX content to load
        });
    }
    
    function isWebsiteText(text) {
        // Check if text contains website-specific content that's not a transcript
        const websitePatterns = [
            /this website is 100% free/i,
            /if you like this tool/i,
            /we suggest checking out/i,
            /social media growth/i,
            /we support and are compatible/i,
            /google chrome/i,
            /mozilla firefox/i,
            /microsoft edge/i,
            /safari/i,
            /script\.tokaudit\.io/i,
            /tokscript/i,
            /about tokscript/i,
            /frequently asked questions/i,
            /download/i,
            /generator/i,
            /enter video url/i,
            /start/i,
            /copy link/i,
            /hide timestamps/i,
            /subtitles not available/i
        ];
        
        return websitePatterns.some(pattern => pattern.test(text));
    }

    function extractTranscriptFromAvailableData() {
        console.log('ScriptTokAudit: Attempting to extract transcript from available data...');
        
        // Look for transcript data in script tags
        const scripts = document.querySelectorAll('script');
        for (const script of scripts) {
            const content = script.textContent || script.innerHTML || '';
            if (content.includes('WEBVTT') || content.includes('transcript') || content.includes('subtitles')) {
                console.log('ScriptTokAudit: Found potential transcript in script tag');
                // Extract WEBVTT content
                const webvttMatch = content.match(/WEBVTT[\s\S]*?(?=\n\n|\n$|$)/);
                if (webvttMatch) {
                    const transcript = webvttMatch[0].trim();
                    if (transcript.length > 50 && !isWebsiteText(transcript)) {
                        console.log('ScriptTokAudit: Extracted WEBVTT from script tag');
                        return transcript;
                    }
                }
            }
        }
        
        // Look for transcript data in data attributes
        const elementsWithData = document.querySelectorAll('[data-transcript], [data-subtitles], [data-text]');
        for (const element of elementsWithData) {
            const transcript = element.getAttribute('data-transcript') || 
                             element.getAttribute('data-subtitles') || 
                             element.getAttribute('data-text');
            if (transcript && transcript.length > 50 && !isWebsiteText(transcript)) {
                console.log('ScriptTokAudit: Found transcript in data attribute');
                return transcript;
            }
        }
        
        // Look for hidden elements with transcript content
        const hiddenElements = document.querySelectorAll('[style*="display: none"], [style*="visibility: hidden"], .hidden, [hidden]');
        for (const element of hiddenElements) {
            const text = element.textContent || element.value || '';
            if (text && text.length > 50 && !isWebsiteText(text)) {
                console.log('ScriptTokAudit: Found transcript in hidden element');
                return text;
            }
        }
        
        // Look for global variables that might contain transcript data
        try {
            if (window.transcriptData || window.subtitlesData || window.videoTranscript) {
                const data = window.transcriptData || window.subtitlesData || window.videoTranscript;
                if (typeof data === 'string' && data.length > 50 && !isWebsiteText(data)) {
                    console.log('ScriptTokAudit: Found transcript in global variable');
                    return data;
                }
            }
        } catch (e) {
            console.log('ScriptTokAudit: Error accessing global variables:', e.message);
        }
        
        // Look for JSON data in the page
        const jsonScripts = document.querySelectorAll('script[type="application/json"], script[type="application/ld+json"]');
        for (const script of jsonScripts) {
            try {
                const data = JSON.parse(script.textContent);
                if (data.transcript || data.subtitles || data.text) {
                    const transcript = data.transcript || data.subtitles || data.text;
                    if (typeof transcript === 'string' && transcript.length > 50 && !isWebsiteText(transcript)) {
                        console.log('ScriptTokAudit: Found transcript in JSON data');
                        return transcript;
                    }
                }
            } catch (e) {
                console.log('ScriptTokAudit: Error parsing JSON:', e.message);
            }
        }
        
        console.log('ScriptTokAudit: No transcript found in available data');
        return null;
    }

    function sendTranscriptToAndroid(text) {
        try {
            const maxChunk = 1000;
            const total = Math.ceil(text.length / maxChunk) || 1;
            for (let i = 0; i < total; i++) {
                const chunk = text.slice(i * maxChunk, (i + 1) * maxChunk);
                if (window.Android && typeof window.Android.postMessage === 'function') {
                    window.Android.postMessage('transcript', chunk, i, total);
                }
            }
        } catch (e) {
            try { window.Android && window.Android.postMessage && window.Android.postMessage('error', 'Chunk send failed: ' + (e && e.message), 0, 1); } catch(_){ }
        }
    }
    
    async function removePopup() {
        console.log('ScriptTokAudit: Checking for and removing popups...');
        
        // Common popup selectors
        const popupSelectors = [
            '.modal',
            '.popup',
            '.overlay',
            '[class*="modal"]',
            '[class*="popup"]',
            '[class*="overlay"]',
            '.bulk-download-modal-parent',
            '.image-div',
            '[class*="close"]',
            '[class*="dismiss"]',
            'button[aria-label*="close"]',
            'button[aria-label*="dismiss"]',
            '.close-button',
            '.dismiss-button',
            '.modal-backdrop',
            '.modal-dialog',
            '[role="dialog"]'
        ];
        
        for (const selector of popupSelectors) {
            try {
                const elements = document.querySelectorAll(selector);
                for (const element of elements) {
                    // Check if it's visible
                    const style = window.getComputedStyle(element);
                    if (style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0') {
                        console.log('ScriptTokAudit: Found popup, attempting to remove:', selector);
                        
                        // Try clicking close buttons first
                        const closeButtons = element.querySelectorAll('button, .close, .dismiss, [aria-label*="close"], [aria-label*="dismiss"], .btn-close');
                        for (const closeBtn of closeButtons) {
                            try {
                                closeBtn.click();
                                console.log('ScriptTokAudit: Clicked close button');
                                await new Promise(r => setTimeout(r, 500));
                            } catch (e) {
                                console.log('ScriptTokAudit: Failed to click close button:', e.message);
                            }
                        }
                        
                        // If no close buttons, try clicking the element itself
                        if (closeButtons.length === 0) {
                            try {
                                element.click();
                                console.log('ScriptTokAudit: Clicked popup element');
                                await new Promise(r => setTimeout(r, 500));
                            } catch (e) {
                                console.log('ScriptTokAudit: Failed to click popup element:', e.message);
                            }
                        }
                        
                        // Try to hide it with CSS
                        try {
                            element.style.display = 'none';
                            element.style.visibility = 'hidden';
                            element.style.opacity = '0';
                            console.log('ScriptTokAudit: Hidden popup with CSS');
                        } catch (e) {
                            console.log('ScriptTokAudit: Failed to hide popup with CSS:', e.message);
                        }
                    }
                }
            } catch (e) {
                console.log('ScriptTokAudit: Error handling popup selector', selector, ':', e.message);
            }
        }
        
        // Also try clicking outside any modal to dismiss it
        try {
            document.body.click();
            console.log('ScriptTokAudit: Clicked outside to dismiss modal');
        } catch (e) {
            console.log('ScriptTokAudit: Failed to click outside:', e.message);
        }
        
        // Try pressing Escape key to close modals
        try {
            document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', keyCode: 27, which: 27, bubbles: true }));
            console.log('ScriptTokAudit: Pressed Escape key to close modal');
        } catch (e) {
            console.log('ScriptTokAudit: Failed to press Escape key:', e.message);
        }
    }
    
    function pollForResults() {
        let attempts = 0;
        const maxAttempts = 90; // Increased to 90 attempts (3 minutes total)
        const pollInterval = 2000; // 2 seconds between checks
        
        const __intervalId = setInterval(() => {
            attempts++;
            console.log(`ScriptTokAudit: Check #${attempts}, status=${checkForTranscriptResult().status}`);
            
            const result = checkForTranscriptResult();
            
            if (result.status === 'processing') {
                if (attempts >= maxAttempts) {
                    clearInterval(__intervalId);
                    window.__PLUCT_AUTOMATION_RUNNING__ = false;
                    safeAndroidCall('onProcessingUpdate', false);
                    safeAndroidCall('postMessage', 'error', 'Processing timeout - transcript generation took too long', 0, 1);
                }
            } else if (result.status === 'no_subtitles') {
                clearInterval(__intervalId);
                window.__PLUCT_AUTOMATION_RUNNING__ = false;
                safeAndroidCall('onProcessingUpdate', false);
                safeAndroidCall('postMessage', 'no_subtitles', 'Subtitles not available for this video', 0, 1);
            } else if (result.status === 'ready_to_copy') {
                clearInterval(__intervalId); 
                __intervalId = null; 
                window.__PLUCT_AUTOMATION_RUNNING__ = false;
                safeAndroidCall('onProcessingUpdate', false);
                
                // Click the copy button to get the transcript
                setTimeout(async () => {
                    try {
                        console.log('ScriptTokAudit: Clicking copy button:', result.copyButton.textContent);
                        result.copyButton.click();
                        
                        // Wait for copy action to complete, then extract transcript
                        setTimeout(async () => {
                            console.log('ScriptTokAudit: Copy button clicked, attempting to extract transcript...');
                            
                            // First try to get transcript from the page
                            const transcriptText = await extractTranscriptFromPage();
                            
                            if (transcriptText) {
                                console.log('ScriptTokAudit: Extracted transcript length:', transcriptText.length);
                                sendTranscriptToAndroid(transcriptText);
                            } else {
                                // If we can't find transcript in page, try to get it from the available data
                                console.log('ScriptTokAudit: No transcript found in page, trying to extract from available data...');
                                const availableTranscript = extractTranscriptFromAvailableData();
                                
                                if (availableTranscript) {
                                    console.log('ScriptTokAudit: Extracted transcript from available data, length:', availableTranscript.length);
                                    sendTranscriptToAndroid(availableTranscript);
                                } else {
                                    console.log('ScriptTokAudit: No transcript found, sending "Subtitles Not Available" message');
                                    safeAndroidCall('postMessage', 'no_subtitles', 'Subtitles not available for this video', 0, 1);
                                }
                            }
                        }, 4000); // Increased to 4 seconds after clicking copy button
                    } catch (err) { 
                        safeAndroidCall('postMessage', 'error', 'Copy failed: ' + (err && err.message), 0, 1); 
                    }
                }, 1500); // Increased to 1.5 seconds
            }
        }, pollInterval);
    }
    
    (async function runAutomation() {
        if (window.__PLUCT_AUTOMATION_RUNNING__) {
            console.log('ScriptTokAudit: Automation already running, skipping...');
            return;
        }
        
        window.__PLUCT_AUTOMATION_RUNNING__ = true;
        console.log('ScriptTokAudit: Starting complete automation workflow');
        
        try {
            // Wait for page to be fully loaded
            console.log('ScriptTokAudit: Waiting for page to be ready...');
            await new Promise(r => setTimeout(r, 3000)); // Increased initial wait
            
            // First, try to remove any popup that might be blocking the interface
            console.log('ScriptTokAudit: Removing popups...');
            await removePopup();
            
            // Wait a bit more for the page to settle after popup removal
            await new Promise(r => setTimeout(r, 2000));
            
            // Try removing popups again in case new ones appeared
            await removePopup();
            
            // Wait again
            await new Promise(r => setTimeout(r, 1000));
            
            // Now proceed with the automation
            console.log('ScriptTokAudit: Starting URL submission...');
            await fillAndSubmitUrl(TIKTOK_URL);
            
            // Start polling for results with increased patience
            console.log('ScriptTokAudit: Starting to poll for results...');
            pollForResults();
            
        } catch (e) {
            console.error('ScriptTokAudit: Automation error:', e);
            window.__PLUCT_AUTOMATION_RUNNING__ = false;
            safeAndroidCall('onProcessingUpdate', false);
            safeAndroidCall('postMessage', 'error', 'Automation failed: ' + e.message, 0, 1);
        }
    })();
})();
