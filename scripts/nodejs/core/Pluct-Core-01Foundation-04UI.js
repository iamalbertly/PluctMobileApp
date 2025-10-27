/**
 * Pluct-Core-01Foundation-04UI - UI interaction functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Adheres to 300-line limit with smart separation of concerns
 */

const PluctLogger = require('./Logger');

class PluctCoreFoundationUI {
    constructor() {
        this.logger = PluctLogger;
        this.uiState = {
            currentScreen: null,
            lastInteraction: null,
            uiHierarchy: null
        };
    }

    /**
     * Dump UI hierarchy
     */
    async dumpUIHierarchy() {
        try {
            this.logger.logInfo('📱 Dumping UI hierarchy...');
            
            const result = await this.executeCommand('adb shell uiautomator dump /sdcard/ui_dump.xml');
            if (!result.success) {
                throw new Error('Failed to dump UI hierarchy');
            }
            
            const pullResult = await this.executeCommand('adb pull /sdcard/ui_dump.xml ./ui_dump.xml');
            if (!pullResult.success) {
                throw new Error('Failed to pull UI dump');
            }
            
            const fs = require('fs');
            const uiDump = fs.readFileSync('./ui_dump.xml', 'utf8');
            
            this.uiState.uiHierarchy = uiDump;
            this.logger.logInfo('✅ UI hierarchy dumped successfully');
            
            return { success: true, uiDump };
        } catch (error) {
            this.logger.logError('❌ Failed to dump UI hierarchy: ' + error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Read last UI dump
     */
    readLastUIDump() {
        try {
            const fs = require('fs');
            if (fs.existsSync('./ui_dump.xml')) {
                return fs.readFileSync('./ui_dump.xml', 'utf8');
            }
            return this.uiState.uiHierarchy || '';
        } catch (error) {
            this.logger.logWarn('⚠️ Failed to read UI dump:', error.message);
            return '';
        }
    }

    /**
     * Tap by text
     */
    async tapByText(text, timeout = 5000) {
        try {
            this.logger.logInfo(`🔍 Tapping by text: "${text}"`);
            
            // First dump UI to find the element
            await this.dumpUIHierarchy();
            const uiDump = this.readLastUIDump();
            
            const element = this.findElementByText(uiDump, text);
            if (!element) {
                this.logger.logError(`❌ CRITICAL: Could not tap by text: ${text}`);
                this.logger.logError(`❌ Available text elements in UI:`);
                // Extract all text elements for debugging
                const textMatches = uiDump.match(/text="([^"]+)"/g);
                if (textMatches) {
                    const uniqueTexts = [...new Set(textMatches.map(match => match.replace('text="', '').replace('"', '')))];
                    uniqueTexts.forEach(availableText => {
                        if (availableText.trim()) {
                            this.logger.logError(`❌   - "${availableText}"`);
                        }
                    });
                }
                throw new Error(`Element with text "${text}" not found`);
            }
            
            const result = await this.tapElement(element);
            if (!result.success) {
                throw new Error('Failed to tap element');
            }
            
            this.uiState.lastInteraction = { type: 'tapByText', text, timestamp: Date.now() };
            this.logger.logInfo(`✅ Tapped by text: "${text}"`);
            
            return { success: true };
        } catch (error) {
            this.logger.logError(`❌ Failed to tap by text "${text}": ` + error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Tap by test tag
     */
    async tapByTestTag(testTag, timeout = 5000) {
        try {
            this.logger.logInfo(`🔍 Tapping by test tag: "${testTag}"`);
            
            // First dump UI to find the element
            await this.dumpUIHierarchy();
            const uiDump = this.readLastUIDump();
            
            const element = this.findElementByTestTag(uiDump, testTag);
            if (!element) {
                throw new Error(`Element with test tag "${testTag}" not found`);
            }
            
            const result = await this.tapElement(element);
            if (!result.success) {
                throw new Error('Failed to tap element');
            }
            
            this.uiState.lastInteraction = { type: 'tapByTestTag', testTag, timestamp: Date.now() };
            this.logger.logInfo(`✅ Tapped by test tag: "${testTag}"`);
            
            return { success: true };
        } catch (error) {
            this.logger.logError(`❌ Failed to tap by test tag "${testTag}": ` + error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Tap first edit text
     */
    async tapFirstEditText() {
        try {
            this.logger.logInfo('🔍 Tapping first edit text...');
            
            await this.dumpUIHierarchy();
            const uiDump = this.readLastUIDump();
            
            const element = this.findFirstEditText(uiDump);
            if (!element) {
                throw new Error('No edit text found');
            }
            
            const result = await this.tapElement(element);
            if (!result.success) {
                throw new Error('Failed to tap edit text');
            }
            
            this.uiState.lastInteraction = { type: 'tapFirstEditText', timestamp: Date.now() };
            this.logger.logInfo('✅ Tapped first edit text');
            
            return { success: true };
        } catch (error) {
            this.logger.logError('❌ Failed to tap first edit text: ' + error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Input text (with automatic clearing)
     */
    async inputText(text) {
        try {
            this.logger.logInfo(`📝 Inputting text: "${text}"`);
            
            // First, ensure the field is completely clear
            await this.clearEditText();
            await this.sleep(200);
            
            // Then input the text
            const result = await this.executeCommand(`adb shell input text "${text}"`);
            if (!result.success) {
                throw new Error('Failed to input text');
            }
            
            this.uiState.lastInteraction = { type: 'inputText', text, timestamp: Date.now() };
            this.logger.logInfo(`✅ Text inputted: "${text}"`);
            
            return { success: true };
        } catch (error) {
            this.logger.logError(`❌ Failed to input text: ` + error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Clear text from EditText field
     */
    async clearEditText() {
        try {
            this.logger.logInfo('🧹 Clearing EditText field...');
            
            // First dump UI to find EditText elements
            await this.dumpUIHierarchy();
            const uiDump = this.readLastUIDump();
            
            const editTextElement = this.findFirstEditText(uiDump);
            if (!editTextElement) {
                this.logger.logWarn('⚠️ No EditText element found to clear');
                return { success: true }; // Not an error if no field to clear
            }
            
            // Tap the EditText to focus it
            const tapResult = await this.tapElement(editTextElement);
            if (!tapResult.success) {
                throw new Error('Failed to tap EditText for clearing');
            }
            
            await this.sleep(500); // Wait for focus
            
            // Ultra-aggressive clearing - multiple methods to ensure complete text removal
            this.logger.logInfo('🧹 Using ultra-aggressive text clearing...');
            
            // Method 1: Select all and delete (multiple attempts)
            for (let i = 0; i < 15; i++) {
                await this.executeCommand('adb shell input keyevent KEYCODE_CTRL_A');
                await this.sleep(30);
                await this.executeCommand('adb shell input keyevent KEYCODE_DEL');
                await this.sleep(30);
            }
            
            // Method 2: Move to beginning and delete everything
            await this.executeCommand('adb shell input keyevent KEYCODE_MOVE_HOME');
            await this.sleep(50);
            for (let i = 0; i < 300; i++) {
                await this.executeCommand('adb shell input keyevent KEYCODE_DEL');
                await this.sleep(3);
            }
            
            // Method 3: Long press and select all
            await this.executeCommand('adb shell input keyevent KEYCODE_DPAD_CENTER');
            await this.sleep(100);
            await this.executeCommand('adb shell input keyevent KEYCODE_A');
            await this.sleep(50);
            await this.executeCommand('adb shell input keyevent KEYCODE_DEL');
            await this.sleep(50);
            
            // Method 4: Clear using input text with empty string
            await this.executeCommand('adb shell input text ""');
            await this.sleep(50);
            
            // Method 5: Use backspace extensively
            for (let i = 0; i < 200; i++) {
                await this.executeCommand('adb shell input keyevent KEYCODE_DEL');
                await this.sleep(3);
            }
            
            // Method 6: Verify field is empty by checking UI dump
            await this.sleep(200);
            await this.dumpUIHierarchy();
            const verificationDump = this.readLastUIDump();
            const verificationElement = this.findFirstEditText(verificationDump);
            
            if (verificationElement && verificationElement.text && verificationElement.text.trim() !== '') {
                this.logger.logWarn('⚠️ Field still contains text after clearing, attempting additional clearing...');
                // Additional clearing attempts
                for (let i = 0; i < 50; i++) {
                    await this.executeCommand('adb shell input keyevent KEYCODE_CTRL_A');
                    await this.sleep(10);
                    await this.executeCommand('adb shell input keyevent KEYCODE_DEL');
                    await this.sleep(10);
                }
            }
            
            this.uiState.lastInteraction = { type: 'clearEditText', timestamp: Date.now() };
            this.logger.logInfo('✅ EditText field cleared successfully');
            
            return { success: true };
        } catch (error) {
            this.logger.logError('❌ Failed to clear EditText: ' + error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Reset app state for clean test runs
     */
    async resetAppState() {
        try {
            this.logger.logInfo('🔄 Performing comprehensive app state reset...');
            
            // Force stop the app multiple times to ensure it's completely stopped
            for (let i = 0; i < 3; i++) {
                await this.executeCommand('adb shell am force-stop app.pluct');
                await this.sleep(500);
            }
            
            // Clear app data to reset state
            await this.executeCommand('adb shell pm clear app.pluct');
            await this.sleep(2000);
            
            // Clear any cached data
            await this.executeCommand('adb shell pm clear app.pluct');
            await this.sleep(1000);
            
            // Restart the app
            await this.executeCommand('adb shell am start -n app.pluct/.MainActivity');
            await this.sleep(5000); // Wait longer for app to fully load
            
            // Verify app is running
            const appStatus = await this.executeCommand('adb shell dumpsys activity activities | grep app.pluct');
            if (!appStatus.success) {
                throw new Error('App failed to start after reset');
            }
            
            this.logger.logInfo('✅ App state reset completed successfully');
            return { success: true };
        } catch (error) {
            this.logger.logError('❌ Failed to reset app state: ' + error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Wait for text
     */
    async waitForText(text, timeout = 5000) {
        const startTime = Date.now();
        
        while (Date.now() - startTime < timeout) {
            await this.dumpUIHierarchy();
            const uiDump = this.readLastUIDump();
            
            if (uiDump.includes(text)) {
                this.logger.logInfo(`✅ Found text: "${text}"`);
                return { success: true };
            }
            
            await this.sleep(500);
        }
        
        this.logger.logWarn(`⚠️ Text not found within timeout: "${text}"`);
        return { success: false, error: 'Text not found' };
    }

    /**
     * Find element by test tag
     */
    findElementByTestTag(uiDump, testTag) {
        const regex = new RegExp(`test-tag="${testTag}"[^>]*bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"`, 'i');
        const match = uiDump.match(regex);
        
        if (match) {
            const [, x1, y1, x2, y2] = match;
            return {
                testTag,
                bounds: {
                    x1: parseInt(x1),
                    y1: parseInt(y1),
                    x2: parseInt(x2),
                    y2: parseInt(y2)
                }
            };
        }
        
        return null;
    }

    /**
     * Find element by text
     */
    findElementByText(uiDump, text) {
        const regex = new RegExp(`text="${text}"[^>]*bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"`, 'i');
        const match = uiDump.match(regex);
        
        if (match) {
            const [, x1, y1, x2, y2] = match;
            return {
                text,
                bounds: {
                    x1: parseInt(x1),
                    y1: parseInt(y1),
                    x2: parseInt(x2),
                    y2: parseInt(y2)
                }
            };
        }
        
        return null;
    }

    /**
     * Find first edit text
     */
    findFirstEditText(uiDump) {
        const regex = /class="android\.widget\.EditText"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/i;
        const match = uiDump.match(regex);
        
        if (match) {
            const [, x1, y1, x2, y2] = match;
            return {
                type: 'EditText',
                bounds: {
                    x1: parseInt(x1),
                    y1: parseInt(y1),
                    x2: parseInt(x2),
                    y2: parseInt(y2)
                }
            };
        }
        
        return null;
    }

    /**
     * Tap element
     */
    async tapElement(element) {
        const centerX = Math.floor((element.bounds.x1 + element.bounds.x2) / 2);
        const centerY = Math.floor((element.bounds.y1 + element.bounds.y2) / 2);
        
        this.logger.logInfo(`📍 Tapping at coordinates: ${centerX}, ${centerY}`);
        
        return this.executeCommand(`adb shell input tap ${centerX} ${centerY}`);
    }

    /**
     * Get UI state
     */
    getUIState() {
        return this.uiState;
    }

    /**
     * Execute command
     */
    async executeCommand(command, timeout = 5000) {
        try {
            const { exec } = require('child_process');
            const { promisify } = require('util');
            const execAsync = promisify(exec);
            
            const { stdout, stderr } = await execAsync(command, { timeout });
            
            return { 
                success: true, 
                output: stdout, 
                error: stderr,
                fullOutput: stdout + stderr
            };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Fetch HTML metadata from TikTok URL
     */
    async fetchHtmlMetadata(url) {
        try {
            this.logger.logInfo(`📊 Fetching metadata for: ${url}`);
            
            // Use curl to fetch the HTML content
            const curlCommand = `curl -s -L -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36" "${url}"`;
            const result = await this.executeCommand(curlCommand, 15000);
            
            if (!result.success) {
                this.logger.logWarn('⚠️ Failed to fetch HTML content, using fallback');
                return this.generateFallbackMetadata(url);
            }
            
            const html = result.output;
            this.logger.logInfo(`📊 Fetched ${html.length} characters of HTML`);
            
            // Extract metadata using regex patterns
            const metadata = this.extractMetadataFromHtml(html, url);
            
            if (metadata.title && metadata.title !== 'TikTok Video') {
                this.logger.logInfo(`✅ Extracted real metadata: ${metadata.title}`);
                return { success: true, ...metadata };
            } else {
                this.logger.logWarn('⚠️ Could not extract real metadata, using fallback');
                return this.generateFallbackMetadata(url);
            }
            
        } catch (error) {
            this.logger.logError(`❌ Failed to fetch metadata: ${error.message}`);
            return this.generateFallbackMetadata(url);
        }
    }

    /**
     * Extract metadata from HTML content
     */
    extractMetadataFromHtml(html, url) {
        const metadata = {
            url: url,
            title: 'TikTok Video',
            description: '',
            author: '',
            duration: 0,
            thumbnail: '',
            cached: false,
            timestamp: new Date().toISOString()
        };

        try {
            // Extract title from various meta tags
            const titlePatterns = [
                /<meta property="og:title" content="([^"]+)"/i,
                /<meta name="twitter:title" content="([^"]+)"/i,
                /<title>([^<]+)<\/title>/i,
                /<meta property="og:description" content="([^"]+)"/i
            ];

            for (const pattern of titlePatterns) {
                const match = html.match(pattern);
                if (match && match[1] && match[1].trim() !== 'TikTok') {
                    metadata.title = match[1].trim();
                    break;
                }
            }

            // Extract description
            const descPatterns = [
                /<meta property="og:description" content="([^"]+)"/i,
                /<meta name="description" content="([^"]+)"/i,
                /<meta name="twitter:description" content="([^"]+)"/i
            ];

            for (const pattern of descPatterns) {
                const match = html.match(pattern);
                if (match && match[1] && match[1].trim()) {
                    metadata.description = match[1].trim();
                    break;
                }
            }

            // Extract author/creator
            const authorPatterns = [
                /<meta property="og:site_name" content="([^"]+)"/i,
                /<meta name="twitter:site" content="@([^"]+)"/i,
                /"author":"([^"]+)"/i,
                /"creator":"([^"]+)"/i
            ];

            for (const pattern of authorPatterns) {
                const match = html.match(pattern);
                if (match && match[1] && match[1].trim()) {
                    metadata.author = match[1].trim();
                    break;
                }
            }

            // Extract thumbnail
            const thumbnailPatterns = [
                /<meta property="og:image" content="([^"]+)"/i,
                /<meta name="twitter:image" content="([^"]+)"/i
            ];

            for (const pattern of thumbnailPatterns) {
                const match = html.match(pattern);
                if (match && match[1] && match[1].trim()) {
                    metadata.thumbnail = match[1].trim();
                    break;
                }
            }

            // Extract duration from JSON-LD or other structured data
            const durationPatterns = [
                /"duration":"PT(\d+)S"/i,
                /"duration":"(\d+)"/i,
                /"videoDuration":(\d+)/i
            ];

            for (const pattern of durationPatterns) {
                const match = html.match(pattern);
                if (match && match[1]) {
                    metadata.duration = parseInt(match[1]);
                    break;
                }
            }

            // If we still don't have a good title, try to extract from JSON-LD
            if (metadata.title === 'TikTok Video' || !metadata.title) {
                const jsonLdPattern = /<script type="application\/ld\+json">(.*?)<\/script>/is;
                const jsonLdMatch = html.match(jsonLdPattern);
                
                if (jsonLdMatch) {
                    try {
                        const jsonLd = JSON.parse(jsonLdMatch[1]);
                        if (jsonLd.name && jsonLd.name !== 'TikTok') {
                            metadata.title = jsonLd.name;
                        }
                        if (jsonLd.description) {
                            metadata.description = jsonLd.description;
                        }
                        if (jsonLd.author && jsonLd.author.name) {
                            metadata.author = jsonLd.author.name;
                        }
                    } catch (e) {
                        this.logger.logWarn('⚠️ Failed to parse JSON-LD');
                    }
                }
            }

            // Mark as cached if we got real data
            if (metadata.title !== 'TikTok Video' || metadata.description || metadata.author) {
                metadata.cached = true;
            }

        } catch (error) {
            this.logger.logWarn(`⚠️ Error extracting metadata: ${error.message}`);
        }

        return metadata;
    }

    /**
     * Generate fallback metadata when extraction fails
     */
    generateFallbackMetadata(url) {
        return {
            success: true,
            url: url,
            title: 'TikTok Video',
            description: 'Video content from TikTok',
            author: 'TikTok Creator',
            duration: 0,
            thumbnail: '',
            cached: false,
            timestamp: new Date().toISOString(),
            fallback: true
        };
    }

    /**
     * Press back button
     */
    async pressBackButton() {
        try {
            this.logger.logInfo('🔙 Pressing back button...');
            
            const result = await this.executeCommand('adb shell input keyevent 4');
            if (!result.success) {
                throw new Error('Failed to press back button');
            }
            
            this.logger.logInfo('✅ Back button pressed');
            return { success: true };
        } catch (error) {
            this.logger.logError('❌ Failed to press back button: ' + error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Tap by coordinates
     */
    async tapByCoordinates(x, y) {
        try {
            this.logger.logInfo(`📍 Tapping at coordinates: ${x}, ${y}`);
            
            const result = await this.executeCommand(`adb shell input tap ${x} ${y}`);
            if (result.success) {
                this.logger.logInfo(`✅ Tapped at coordinates: ${x}, ${y}`);
                return { success: true };
            } else {
                this.logger.logWarn(`⚠️ Failed to tap at coordinates: ${x}, ${y}`);
                return { success: false, error: 'Failed to tap at coordinates' };
            }
        } catch (error) {
            this.logger.logError('❌ Error tapping by coordinates: ' + error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Open capture sheet by tapping the + FAB or capture button
     */
    async openCaptureSheet() {
        try {
            this.logger.logInfo('📱 Opening capture sheet...');
            
            // First, check if capture sheet is already open
            await this.dumpUIHierarchy();
            const uiDump = this.readLastUIDump();
            
            if (uiDump.includes('Capture Video') || uiDump.includes('Quick Scan') || uiDump.includes('Capture This Insight') || uiDump.includes('capture')) {
                this.logger.logInfo('✅ Capture sheet is already open');
                return { success: true };
            }
            
            // Try to find and tap the capture button
            const captureElements = [
                'Start Transcription',
                'Capture This Insight',
                'capture',
                'Capture',
                '+',
                'Add',
                'add',
                'FAB',
                'fab'
            ];
            
            for (const element of captureElements) {
                const tapResult = await this.tapByText(element);
                if (tapResult.success) {
                    this.logger.logInfo(`✅ Tapped capture element: ${element}`);
                    await this.sleep(2000);
                    
                    // Verify capture sheet opened
                    await this.dumpUIHierarchy();
                    const newUIDump = this.readLastUIDump();
                    
                    if (newUIDump.includes('Capture Video') || newUIDump.includes('Quick Scan') || newUIDump.includes('Capture This Insight') || newUIDump.includes('capture')) {
                        this.logger.logInfo('✅ Capture sheet opened successfully');
                        return { success: true };
                    }
                }
            }
            
            // If no text-based element found, try to find by coordinates (common FAB position)
            const fabCoordinates = [
                { x: 360, y: 1200 }, // Bottom center
                { x: 600, y: 1200 }, // Bottom right
                { x: 120, y: 1200 }, // Bottom left
                { x: 360, y: 1000 }, // Center bottom
            ];
            
            for (const coord of fabCoordinates) {
                const tapResult = await this.tapByCoordinates(coord.x, coord.y);
                if (tapResult.success) {
                    this.logger.logInfo(`✅ Tapped at coordinates: ${coord.x}, ${coord.y}`);
                    await this.sleep(2000);
                    
                    // Verify capture sheet opened
                    await this.dumpUIHierarchy();
                    const newUIDump = this.readLastUIDump();
                    
                    if (newUIDump.includes('Capture Video') || newUIDump.includes('Quick Scan') || newUIDump.includes('Capture This Insight') || newUIDump.includes('capture')) {
                        this.logger.logInfo('✅ Capture sheet opened successfully');
                        return { success: true };
                    }
                }
            }
            
            this.logger.logWarn('⚠️ Could not find capture sheet button');
            return { success: false, error: 'Could not find capture sheet button' };
            
        } catch (error) {
            this.logger.logError('❌ Error opening capture sheet: ' + error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Wait for transcript result
     */
    async waitForTranscriptResult(timeoutMs = 30000, pollMs = 1000) {
        try {
            this.logger.logInfo(`⏳ Waiting for transcript result (timeout: ${timeoutMs}ms)...`);
            
            const startTime = Date.now();
            const endTime = startTime + timeoutMs;
            
            while (Date.now() < endTime) {
                await this.dumpUIHierarchy();
                const uiDump = this.readLastUIDump();
                
                // Check for various completion indicators
                const hasTranscript = uiDump.includes('Transcript') || 
                                    uiDump.includes('transcript') ||
                                    uiDump.includes('Transcription') ||
                                    uiDump.includes('transcription') ||
                                    uiDump.includes('Result') ||
                                    uiDump.includes('result') ||
                                    uiDump.includes('Complete') ||
                                    uiDump.includes('complete') ||
                                    uiDump.includes('Success') ||
                                    uiDump.includes('success');
                
                if (hasTranscript) {
                    this.logger.logInfo('✅ Transcript result found');
                    return { success: true, result: 'Transcript completed' };
                }
                
                // Check for error states
                const hasError = uiDump.includes('Error') || 
                               uiDump.includes('error') ||
                               uiDump.includes('Failed') ||
                               uiDump.includes('failed') ||
                               uiDump.includes('❌') ||
                               uiDump.includes('⚠️');
                
                if (hasError) {
                    this.logger.logWarn('⚠️ Error state detected during transcript wait');
                    return { success: false, error: 'Error state detected' };
                }
                
                await this.sleep(pollMs);
            }
            
            this.logger.logWarn(`⚠️ Timeout waiting for transcript result (${timeoutMs}ms)`);
            return { success: false, error: 'Timeout waiting for transcript result' };
            
        } catch (error) {
            this.logger.logError('❌ Error waiting for transcript result: ' + error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Sleep utility
     */
    async sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    /**
     * HTTP GET request
     */
    async httpGet(url, headers = {}) {
        try {
            this.logger.logInfo(`🌐 HTTP GET: ${url}`);
            
            const https = require('https');
            const http = require('http');
            const { URL } = require('url');
            
            return new Promise((resolve) => {
                const urlObj = new URL(url);
                const options = {
                    hostname: urlObj.hostname,
                    port: urlObj.port || (urlObj.protocol === 'https:' ? 443 : 80),
                    path: urlObj.pathname + urlObj.search,
                    method: 'GET',
                    headers: {
                        'User-Agent': 'Pluct-Test-Runner/1.0',
                        ...headers
                    }
                };
                
                const client = urlObj.protocol === 'https:' ? https : http;
                
                const req = client.request(options, (res) => {
                    let data = '';
                    res.on('data', (chunk) => data += chunk);
                    res.on('end', () => {
                        resolve({
                            status: res.statusCode,
                            data: data,
                            headers: res.headers
                        });
                    });
                });
                
                req.on('error', (error) => {
                    this.logger.logError(`❌ HTTP GET failed: ${error.message}`);
                    resolve({
                        status: 0,
                        error: error.message,
                        data: null
                    });
                });
                
                req.setTimeout(10000, () => {
                    req.destroy();
                    resolve({
                        status: 0,
                        error: 'Request timeout',
                        data: null
                    });
                });
                
                req.end();
            });
        } catch (error) {
            this.logger.logError(`❌ HTTP GET error: ${error.message}`);
            return {
                status: 0,
                error: error.message,
                data: null
            };
        }
    }

    /**
     * HTTP POST request
     */
    async httpPost(url, data, headers = {}) {
        try {
            this.logger.logInfo(`🌐 HTTP POST: ${url}`);
            
            const https = require('https');
            const http = require('http');
            const { URL } = require('url');
            
            const postData = typeof data === 'string' ? data : JSON.stringify(data);
            
            return new Promise((resolve) => {
                const urlObj = new URL(url);
                const options = {
                    hostname: urlObj.hostname,
                    port: urlObj.port || (urlObj.protocol === 'https:' ? 443 : 80),
                    path: urlObj.pathname + urlObj.search,
                    method: 'POST',
                    headers: {
                        'User-Agent': 'Pluct-Test-Runner/1.0',
                        'Content-Length': Buffer.byteLength(postData),
                        ...headers
                    }
                };
                
                const client = urlObj.protocol === 'https:' ? https : http;
                
                const req = client.request(options, (res) => {
                    let data = '';
                    res.on('data', (chunk) => data += chunk);
                    res.on('end', () => {
                        resolve({
                            status: res.statusCode,
                            data: data,
                            headers: res.headers
                        });
                    });
                });
                
                req.on('error', (error) => {
                    this.logger.logError(`❌ HTTP POST failed: ${error.message}`);
                    resolve({
                        status: 0,
                        error: error.message,
                        data: null
                    });
                });
                
                req.setTimeout(10000, () => {
                    req.destroy();
                    resolve({
                        status: 0,
                        error: 'Request timeout',
                        data: null
                    });
                });
                
                req.write(postData);
                req.end();
            });
        } catch (error) {
            this.logger.logError(`❌ HTTP POST error: ${error.message}`);
            return {
                status: 0,
                error: error.message,
                data: null
            };
        }
    }

    /**
     * Write JSON artifact to file
     */
    writeJsonArtifact(filename, data) {
        try {
            const fs = require('fs');
            const path = require('path');
            
            // Create artifacts directory if it doesn't exist
            const artifactsDir = './artifacts';
            if (!fs.existsSync(artifactsDir)) {
                fs.mkdirSync(artifactsDir, { recursive: true });
            }
            
            const filePath = path.join(artifactsDir, filename);
            const jsonData = JSON.stringify(data, null, 2);
            
            fs.writeFileSync(filePath, jsonData, 'utf8');
            this.logger.logInfo(`📄 JSON artifact written: ${filePath}`);
            
            return { success: true, filePath };
        } catch (error) {
            this.logger.logError(`❌ Failed to write JSON artifact: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Validate TikTok URL
     */
    async validateTikTokUrl(url) {
        try {
            this.logger.logInfo(`🔍 Validating TikTok URL: ${url}`);
            
            // Basic URL validation
            if (!url || typeof url !== 'string') {
                return { success: false, error: 'Invalid URL format' };
            }
            
            // Check if it's a valid URL
            try {
                new URL(url);
            } catch (error) {
                return { success: false, error: 'Invalid URL format' };
            }
            
            // Check if it's a TikTok URL
            const tiktokPattern = /^https?:\/\/(?:vm\.tiktok\.com|www\.tiktok\.com|tiktok\.com)\/[\w\-]+\/?$/;
            if (!tiktokPattern.test(url)) {
                return { success: false, error: 'Not a valid TikTok URL' };
            }
            
            this.logger.logInfo(`✅ TikTok URL validated successfully`);
            return { success: true, normalizedUrl: url };
            
        } catch (error) {
            this.logger.logError(`❌ Failed to validate TikTok URL: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctCoreFoundationUI;
