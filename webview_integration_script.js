/**
 * Simplified ScriptTokAudit.io Automation for WebView Integration
 * 
 * Replace {{TIKTOK_URL}} with the actual TikTok URL before injecting
 */

(function() {
    console.log('ScriptTokAudit: Starting automation');
    
    const TIKTOK_URL = '{{TIKTOK_URL}}';
    
    // Close modals immediately
    function closeModals() {
        const bulkModal = document.querySelector('.bulk-download-modal-parent');
        if (bulkModal) bulkModal.style.display = 'none';
        
        document.querySelectorAll('.modal, [role="dialog"], .popup').forEach(modal => {
            if (modal.style.display !== 'none') modal.style.display = 'none';
        });
        
        document.querySelectorAll('.modal-backdrop, .overlay, [class*="overlay"]').forEach(overlay => {
            overlay.remove();
        });
        
        document.body.click();
    }
    
    // Normalize URL
    function normalizeUrl(url) {
        if (url.includes('vm.tiktok.com')) return url;
        if (url.includes('tiktok.com/@')) {
            const match = url.match(/tiktok\.com\/@[^\/]+\/video\/(\d+)/);
            if (match) return `https://vm.tiktok.com/${match[1]}/`;
        }
        return url;
    }
    
    // Fill and submit
    function fillAndSubmit(url) {
        const textarea = document.querySelector('textarea[placeholder="Enter Video Url"]');
        if (!textarea) throw new Error('No textarea found');
        
        textarea.value = url;
        textarea.dispatchEvent(new Event('input', { bubbles: true }));
        textarea.dispatchEvent(new Event('change', { bubbles: true }));
        
        const startButton = document.querySelector('button:has-text("START")');
        if (!startButton) throw new Error('No START button found');
        
        startButton.click();
    }
    
    // Check for results
    function checkResult() {
        const pageText = document.body.textContent || '';
        
        if (pageText.includes('Subtitles Not Available')) {
            return { status: 'no_transcript' };
        }
        
        if (pageText.includes('No valid tiktok data found') || pageText.includes('Invalid URL')) {
            return { status: 'error' };
        }
        
        const buttons = document.querySelectorAll('button');
        for (const button of buttons) {
            if (button.textContent.toLowerCase().includes('copy')) {
                return { status: 'success' };
            }
        }
        
        return { status: 'processing' };
    }
    
    // Copy transcript
    function copyTranscript() {
        const copyButton = document.querySelector('button:has-text("Copy")');
        if (copyButton) {
            copyButton.click();
            return true;
        }
        return false;
    }
    
    // Main workflow
    async function runWorkflow() {
        try {
            // Step 1: Close modals
            closeModals();
            await new Promise(resolve => setTimeout(resolve, 1000));
            
            // Step 2: Fill and submit
            const normalizedUrl = normalizeUrl(TIKTOK_URL);
            fillAndSubmit(normalizedUrl);
            
            // Step 3: Monitor results
            let attempts = 0;
            const maxAttempts = 30;
            
            const interval = setInterval(() => {
                attempts++;
                const result = checkResult();
                
                if (result.status === 'success') {
                    clearInterval(interval);
                    setTimeout(() => {
                        if (copyTranscript()) {
                            console.log('ScriptTokAudit: Success - transcript copied');
                            if (window.Android && window.Android.onTranscriptReceived) {
                                window.Android.onTranscriptReceived('Transcript copied to clipboard');
                            }
                        }
                    }, 1000);
                } else if (result.status === 'no_transcript') {
                    clearInterval(interval);
                    console.log('ScriptTokAudit: No transcript available');
                    if (window.Android && window.Android.onNoTranscript) {
                        window.Android.onNoTranscript();
                    }
                } else if (result.status === 'error') {
                    clearInterval(interval);
                    console.log('ScriptTokAudit: Error occurred');
                    if (window.Android && window.Android.onError) {
                        window.Android.onError('Error processing URL');
                    }
                } else if (attempts >= maxAttempts) {
                    clearInterval(interval);
                    console.log('ScriptTokAudit: Timeout');
                    if (window.Android && window.Android.onError) {
                        window.Android.onError('Timeout waiting for results');
                    }
                }
            }, 2000);
            
        } catch (error) {
            console.error('ScriptTokAudit: Error:', error);
            if (window.Android && window.Android.onError) {
                window.Android.onError('Automation failed: ' + error.message);
            }
        }
    }
    
    // Start automation
    runWorkflow();
})();
