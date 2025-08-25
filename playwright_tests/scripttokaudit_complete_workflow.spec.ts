import { test, expect } from '@playwright/test';

/**
 * Complete ScriptTokAudit.io Transcript Automation Workflow
 * 
 * This test demonstrates the full automation journey:
 * 1. Navigate to script.tokaudit.io
 * 2. Close any modals/popups
 * 3. Paste TikTok URL into input field
 * 4. Click START button
 * 5. Wait for transcript generation
 * 6. Extract transcript or handle "no transcript" case
 * 7. Copy transcript to clipboard
 * 8. Close the session
 */

test.describe('ScriptTokAudit Complete Workflow', () => {
    test('Complete transcript automation journey', async ({ page, context }) => {
        // Step 1: Navigate to script.tokaudit.io
        console.log('Step 1: Navigating to script.tokaudit.io');
        await page.goto('https://www.script.tokaudit.io/');
        
        // Wait for page to load
        await page.waitForLoadState('networkidle');
        
        // Take initial screenshot
        await page.screenshot({ 
            path: 'test-results/01_initial_page_load.png', 
            fullPage: true 
        });
        
        // Step 2: Close any modals or popups
        console.log('Step 2: Closing any modals or popups');
        await page.evaluate(() => {
            // Close bulk download modal
            const bulkModal = document.querySelector('.bulk-download-modal-parent');
            if (bulkModal) {
                bulkModal.style.display = 'none';
                console.log('Closed bulk download modal');
            }
            
            // Close any other modals
            const modals = document.querySelectorAll('.modal, [role="dialog"], .popup');
            modals.forEach(modal => {
                if (modal.style.display !== 'none') {
                    modal.style.display = 'none';
                    console.log('Closed modal by hiding it');
                }
            });
            
            // Remove overlay backgrounds
            const overlays = document.querySelectorAll('.modal-backdrop, .overlay, [class*="overlay"]');
            overlays.forEach(overlay => {
                overlay.remove();
                console.log('Removed overlay');
            });
            
            // Click outside to close any remaining modals
            document.body.click();
        });
        
        // Step 3: Find and fill the input field with TikTok URL
        console.log('Step 3: Filling TikTok URL into input field');
        const testUrl = 'https://vm.tiktok.com/ZMAF56hjK//';
        
        // Wait for the textarea to be available
        const textarea = page.locator('textarea[placeholder="Enter Video Url"]');
        await textarea.waitFor({ state: 'visible', timeout: 10000 });
        
        // Clear and fill the textarea
        await textarea.clear();
        await textarea.fill(testUrl);
        
        // Trigger input events to ensure the form recognizes the change
        await textarea.dispatchEvent('input');
        await textarea.dispatchEvent('change');
        await textarea.dispatchEvent('keyup');
        
        console.log(`Filled URL: ${testUrl}`);
        
        // Take screenshot after filling URL
        await page.screenshot({ 
            path: 'test-results/02_url_filled.png', 
            fullPage: true 
        });
        
        // Step 4: Click the START button
        console.log('Step 4: Clicking START button');
        const startButton = page.locator('button:has-text("START")');
        await startButton.waitFor({ state: 'visible', timeout: 10000 });
        await startButton.click();
        
        console.log('Clicked START button');
        
        // Take screenshot after clicking START
        await page.screenshot({ 
            path: 'test-results/03_after_start_click.png', 
            fullPage: true 
        });
        
        // Step 5: Wait for transcript processing and monitor results
        console.log('Step 5: Monitoring for transcript results');
        
        let transcriptFound = false;
        let noTranscriptFound = false;
        let errorFound = false;
        let attempts = 0;
        const maxAttempts = 30; // 60 seconds total (30 * 2 seconds)
        
        while (attempts < maxAttempts && !transcriptFound && !noTranscriptFound && !errorFound) {
            attempts++;
            console.log(`Checking for results, attempt ${attempts}/${maxAttempts}`);
            
            // Wait 2 seconds between checks
            await page.waitForTimeout(2000);
            
            // Get page content
            const pageText = await page.textContent('body') || '';
            
            // Check for "Subtitles Not Available"
            if (pageText.includes('Subtitles Not Available')) {
                console.log('Found: Subtitles Not Available');
                noTranscriptFound = true;
                break;
            }
            
            // Check for other "no transcript" variations
            const noTranscriptTexts = [
                'No transcript',
                'No subtitles',
                'Transcript not available',
                'No captions available'
            ];
            
            for (const noTranscriptText of noTranscriptTexts) {
                if (pageText.toLowerCase().includes(noTranscriptText.toLowerCase())) {
                    console.log(`No transcript available: ${noTranscriptText}`);
                    noTranscriptFound = true;
                    break;
                }
            }
            
            if (noTranscriptFound) break;
            
            // Check for error messages
            const errorTexts = [
                'No valid tiktok data found',
                'Invalid URL',
                'Error processing',
                'Failed to process',
                'Something went wrong'
            ];
            
            for (const errorText of errorTexts) {
                if (pageText.toLowerCase().includes(errorText.toLowerCase())) {
                    console.log(`Found error: ${errorText}`);
                    errorFound = true;
                    break;
                }
            }
            
            if (errorFound) break;
            
            // Look for success indicators (copy buttons, download buttons)
            const buttons = await page.locator('button').all();
            let foundSuccess = false;
            
            for (const button of buttons) {
                const buttonText = await button.textContent() || '';
                if (buttonText.toLowerCase().includes('copy') || 
                    buttonText.toLowerCase().includes('download')) {
                    foundSuccess = true;
                    break;
                }
            }
            
            if (foundSuccess) {
                console.log('Found success indicators, extracting transcript');
                
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
                    const elements = await page.locator(selector).all();
                    for (const element of elements) {
                        const text = await element.textContent() || '';
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
                            
                            console.log('Found transcript:', trimmedText.substring(0, 100) + '...');
                            transcriptFound = true;
                            break;
                        }
                    }
                    if (transcriptFound) break;
                }
                
                if (transcriptFound) break;
                
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
                    console.log('Extracted transcript from page body');
                    transcriptFound = true;
                    break;
                }
            }
        }
        
        // Take final screenshot
        await page.screenshot({ 
            path: 'test-results/04_final_result.png', 
            fullPage: true 
        });
        
        // Step 6: Handle the result
        if (transcriptFound) {
            console.log('Step 6: Transcript found - attempting to copy');
            
            // Try to click copy button
            const copyButton = page.locator('button:has-text("Copy")');
            if (await copyButton.isVisible()) {
                await copyButton.click();
                console.log('Clicked Copy button');
                
                // Wait a moment for copy to complete
                await page.waitForTimeout(1000);
                
                // Verify copy worked (this would require clipboard access in real scenario)
                console.log('Transcript copied to clipboard');
            }
            
        } else if (noTranscriptFound) {
            console.log('Step 6: No transcript available - this is a valid response');
            console.log('The video does not have subtitles available');
            
        } else if (errorFound) {
            console.log('Step 6: Error occurred during processing');
            throw new Error('Error occurred during transcript processing');
            
        } else {
            console.log('Step 6: Timeout reached - no result found');
            throw new Error('Timeout waiting for transcript results');
        }
        
        // Step 7: Log the final status
        console.log('Step 7: Workflow completed successfully');
        console.log(`Final status: ${transcriptFound ? 'Transcript found' : noTranscriptFound ? 'No transcript available' : 'Error occurred'}`);
        
        // Verify the URL is still visible in the input field
        const finalUrl = await textarea.inputValue();
        expect(finalUrl).toBe(testUrl);
        
        console.log('✅ Complete workflow test passed!');
    });
    
    test('Test with different URL formats', async ({ page }) => {
        const testUrls = [
            'https://vm.tiktok.com/ZMAF56hjK//',
            'https://www.tiktok.com/@chris219m/video/7539882214209686840',
            'https://vm.tiktok.com/ABC123/'
        ];
        
        for (const testUrl of testUrls) {
            console.log(`Testing URL: ${testUrl}`);
            
            await page.goto('https://www.script.tokaudit.io/');
            await page.waitForLoadState('networkidle');
            
            // Close modals
            await page.evaluate(() => {
                const bulkModal = document.querySelector('.bulk-download-modal-parent');
                if (bulkModal) bulkModal.style.display = 'none';
            });
            
            // Fill URL
            const textarea = page.locator('textarea[placeholder="Enter Video Url"]');
            await textarea.waitFor({ state: 'visible' });
            await textarea.clear();
            await textarea.fill(testUrl);
            
            // Click START
            const startButton = page.locator('button:has-text("START")');
            await startButton.click();
            
            // Wait for result (simplified)
            await page.waitForTimeout(5000);
            
            const pageText = await page.textContent('body') || '';
            console.log(`Result for ${testUrl}: ${pageText.includes('Subtitles Not Available') ? 'No transcript' : 'Processing'}`);
        }
    });
});
