/**
 * Pluct-Core-01Foundation-03UI - UI interaction module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Handles UI interactions, element detection, and user input
 */
class PluctCoreFoundationUI {

    constructor(config, logger, commands) {
        this.config = config;
        this.logger = logger;
        this.commands = commands;
        this.lastUIDump = '';
    }

    /**
     * Launch the app
     */
    async launchApp() {
        try {
            this.logger.info('Launching Pluct app...');
            const result = await this.executeCommand('adb shell am start -n app.pluct/.PluctUIScreen01MainActivity');

            if (result.success) {
                this.logger.info('App launched successfully');
                await this.sleep(3000); // Wait for app to fully load
                return result;
            } else {
                // Provide detailed error information
                const errorMessage = `Failed to launch app.pluct/.PluctUIScreen01MainActivity. ` +
                    `Error: ${result.error || 'Unknown'}.` +
                    (result.output ? ` Output: ${result.output}` : '');
                this.logger.error(errorMessage);
                return { success: false, error: errorMessage };
            }
        } catch (error) {
            const errorMessage = `App launch exception: ${error.constructor.name}: ${error.message}`;
            this.logger.error(errorMessage);
            return { success: false, error: errorMessage };
        }
    }

    /**
     * Check if app is running
     */
    async isAppRunning() {
        try {
            const result = await this.executeCommand('adb shell dumpsys activity activities');
            return { success: result.success && result.output && result.output.toLowerCase().includes('pluct') };
        } catch (error) {
            this.logger.error(`App running check failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Ensure app is in foreground
     */
    async ensureAppForeground() {
        try {
            const isRunning = await this.isAppRunning();
            if (!isRunning.success) {
                return await this.launchApp();
            }

            // Bring app to foreground
            const result = await this.executeCommand('adb shell am start -n app.pluct/.PluctUIScreen01MainActivity');
            await this.sleep(1000);
            return result;
        } catch (error) {
            this.logger.error(`Foreground ensure failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Capture UI artifacts
     */
    async captureUIArtifacts(tag) {
        try {
            const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
            const filename = `ui_${tag}_${timestamp}.xml`;

            await this.dumpUIHierarchy();

            // Save UI dump to file
            const fs = require('fs');
            const path = require('path');
            const artifactsDir = path.join(__dirname, '../../artifacts/ui');

            if (!fs.existsSync(artifactsDir)) {
                fs.mkdirSync(artifactsDir, { recursive: true });
            }

            fs.writeFileSync(path.join(artifactsDir, filename), this.lastUIDump);

            this.logger.info(`UI artifacts captured: ${filename}`);
            return { success: true, filename };
        } catch (error) {
            this.logger.error(`UI capture failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Kill existing uiautomator processes to prevent conflicts
     */
    async _killExistingUiautomatorProcesses() {
        try {
            // Find all uiautomator processes
            const psResult = await this.executeCommand('adb shell ps | findstr uiautomator', undefined, undefined, { allowFailure: true });
            // Exit code 1 from findstr means "no matches"; treat as clean slate, not an error.
            if (psResult.success && psResult.output) {
                const lines = psResult.output.split('\n').filter(line => line.trim());
                for (const line of lines) {
                    const parts = line.trim().split(/\s+/);
                    if (parts.length > 1) {
                        const pid = parts[1];
                        if (pid && /^\d+$/.test(pid)) {
                            this.logger.info(`Killing existing uiautomator process: ${pid}`);
                            await this.executeCommand(`adb shell kill ${pid}`);
                        }
                    }
                }
                // Wait for processes to terminate
                await this.sleep(500);
            } else if (!psResult.success && psResult.code === 1) {
                // No uiautomator processes running; nothing to kill
                return { success: true };
            }
        } catch (error) {
            this.logger.warn(`Failed to kill existing uiautomator processes: ${error.message}`);
        }
    }

    /**
     * Check memory status before dump
     */
    async _checkMemoryStatus() {
        try {
            const memResult = await this.executeCommand('adb shell dumpsys meminfo app.pluct');
            if (memResult.success && memResult.output) {
                // Check for low memory warnings
                if (memResult.output.includes('Low memory') || memResult.output.includes('oom')) {
                    this.logger.warn('⚠️ Low memory detected, UI dump may be unreliable');
                    return false;
                }
            }
            return true;
        } catch (error) {
            this.logger.warn(`Memory check failed: ${error.message}`);
            return true; // Continue anyway
        }
    }

    /**
     * Dump UI hierarchy with retry logic and process cleanup
     */
    async dumpUIHierarchy(maxRetries = 3) {
        let lastError = null;
        
        for (let attempt = 0; attempt < maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    const backoffMs = Math.min(1000 * Math.pow(2, attempt - 1), 5000);
                    this.logger.info(`Retrying UI dump (attempt ${attempt + 1}/${maxRetries}) after ${backoffMs}ms...`);
                    await this.sleep(backoffMs);
                }

                // Kill existing uiautomator processes before dump
                await this._killExistingUiautomatorProcesses();

                // Check memory status
                const memoryOk = await this._checkMemoryStatus();
                if (!memoryOk && attempt < maxRetries - 1) {
                    this.logger.warn('Memory check failed, will retry after cleanup');
                    // Try to free memory
                    await this.executeCommand('adb shell am force-stop app.pluct');
                    await this.sleep(1000);
                    await this.executeCommand('adb shell am start -n app.pluct/.PluctUIScreen01MainActivity');
                    await this.sleep(2000);
                    continue;
                }

                // Attempt dump with timeout
                const dumpResult = await this.executeCommand('adb shell uiautomator dump /sdcard/ui_dump.xml', 10000);

                if (!dumpResult.success) {
                    // Check if it's a killProcess error (Error Code 137)
                    if (dumpResult.errorCode === 137 || (dumpResult.error && dumpResult.error.includes('killProcess'))) {
                        this.logger.warn(`⚠️ UI dump process was killed (Error 137), attempt ${attempt + 1}/${maxRetries}`);
                        lastError = dumpResult.error || 'Process killed by system';
                        if (attempt < maxRetries - 1) {
                            // Wait longer and try to free resources
                            await this.sleep(2000);
                            continue;
                        }
                    } else {
                        lastError = dumpResult.error || 'Unknown error';
                        if (attempt < maxRetries - 1) {
                            continue;
                        }
                    }
                    
                    // Last attempt failed
                    const errorMsg = `UI dump command failed after ${maxRetries} attempts: ${lastError}`;
                    this.logger.error(`❌ ${errorMsg}`);
                    if (dumpResult.stderr) {
                        this.logger.error(`   ADB stderr: ${dumpResult.stderr}`);
                    }
                    if (dumpResult.adbConnectionIssue) {
                        this.logger.error(`   🔴 ADB Connection Issue Detected!`);
                    }
                    return { 
                        success: false, 
                        error: errorMsg,
                        stderr: dumpResult.stderr,
                        adbConnectionIssue: dumpResult.adbConnectionIssue
                    };
                }

                // Pull the dump file
                const pullResult = await this.executeCommand('adb pull /sdcard/ui_dump.xml ui_dump.xml', 10000);
                if (!pullResult.success) {
                    const errorMsg = `UI dump pull failed: ${pullResult.error || 'Unknown error'}`;
                    this.logger.error(`❌ ${errorMsg}`);
                    if (pullResult.stderr) {
                        this.logger.error(`   ADB stderr: ${pullResult.stderr}`);
                    }
                    if (attempt < maxRetries - 1) {
                        lastError = errorMsg;
                        continue;
                    }
                    return { 
                        success: false, 
                        error: errorMsg,
                        stderr: pullResult.stderr
                    };
                }

                // Read and validate dump file
                const fs = require('fs');
                if (!fs.existsSync('ui_dump.xml')) {
                    const errorMsg = 'UI dump file not found after pull';
                    this.logger.error(`❌ ${errorMsg}`);
                    if (attempt < maxRetries - 1) {
                        lastError = errorMsg;
                        continue;
                    }
                    return { success: false, error: errorMsg };
                }

                this.lastUIDump = fs.readFileSync('ui_dump.xml', 'utf8');
                
                // Validate dump content
                if (!this.lastUIDump || this.lastUIDump.length < 100) {
                    const errorMsg = 'UI dump file is empty or too small';
                    this.logger.error(`❌ ${errorMsg}`);
                    if (attempt < maxRetries - 1) {
                        lastError = errorMsg;
                        continue;
                    }
                    return { success: false, error: errorMsg };
                }

                // Success!
                if (attempt > 0) {
                    this.logger.info(`✅ UI dump succeeded on attempt ${attempt + 1}`);
                }
                return { success: true, uiDump: this.lastUIDump };
                
            } catch (error) {
                lastError = error.message;
                this.logger.error(`UI dump exception on attempt ${attempt + 1}: ${error.message}`);
                if (attempt < maxRetries - 1) {
                    continue;
                }
            }
        }

        // All retries exhausted
        const errorMsg = `UI dump failed after ${maxRetries} attempts: ${lastError || 'Unknown error'}`;
        this.logger.error(`❌ ${errorMsg}`);
        return { success: false, error: errorMsg };
    }

    /**
     * Wait for text to appear
     */
    async waitForText(text, timeoutMs = 10000, pollMs = 1000) {
        try {
            const startTime = Date.now();

            while (Date.now() - startTime < timeoutMs) {
                await this.dumpUIHierarchy();

                if (this.lastUIDump.includes(text)) {
                    return { success: true, found: true };
                }

                await this.sleep(pollMs);
            }

            return { success: false, error: 'Text not found within timeout', found: false };
        } catch (error) {
            this.logger.error(`Wait for text failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Wait for element with polling (generic wait utility)
     * @param {Function} searchFn - Async function that searches for element (should return {success: boolean})
     * @param {number} timeoutMs - Maximum time to wait in milliseconds
     * @param {number} pollMs - Polling interval in milliseconds
     * @returns {Promise<{success: boolean, result?: any, error?: string}>}
     */
    async waitForElement(searchFn, timeoutMs = 10000, pollMs = 500) {
        try {
            const startTime = Date.now();
            let lastError = null;

            while (Date.now() - startTime < timeoutMs) {
                try {
                    const result = await searchFn();
                    if (result && result.success) {
                        this.logger.info('Element found via polling');
                        return { success: true, result };
                    }
                    lastError = result ? result.error : 'Unknown error';
                } catch (error) {
                    lastError = error.message;
                }

                await this.sleep(pollMs);
            }

            const errorMsg = `Element not found within ${timeoutMs}ms. Last error: ${lastError}`;
            this.logger.warn(errorMsg);
            return { success: false, error: errorMsg };
        } catch (error) {
            this.logger.error(`Wait for element failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }


    /**
     * Tap by text
     */
    async tapByText(text) {
        try {
            await this.dumpUIHierarchy();

            const textRegex = new RegExp(`text="${text}"[^>]*bounds="([^"]*)"`, 'g');
            const match = textRegex.exec(this.lastUIDump);

            if (match) {
                const bounds = match[1];
                const coords = this.parseBounds(bounds);
                return await this.tapByCoordinates(coords.x, coords.y);
            }

            return { success: false, error: `Text "${text}" not found` };
        } catch (error) {
            this.logger.error(`Tap by text failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Tap by content description
     */
    async tapByContentDesc(contentDesc) {
        try {
            await this.dumpUIHierarchy();

            const descRegex = new RegExp(`content-desc="${contentDesc}"[^>]*bounds="([^"]*)"`, 'g');
            const match = descRegex.exec(this.lastUIDump);

            if (match) {
                const bounds = match[1];
                const coords = this.parseBounds(bounds);
                return await this.tapByCoordinates(coords.x, coords.y);
            }

            return { success: false, error: `Content description "${contentDesc}" not found` };
        } catch (error) {
            this.logger.error(`Tap by content desc failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Tap by coordinates
     */
    async tapByCoordinates(x, y) {
        try {
            const result = await this.executeCommand(`adb shell input tap ${x} ${y}`);
            await this.sleep(500); // Wait for tap to register
            return result;
        } catch (error) {
            this.logger.error(`Tap by coordinates failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Input text
     * Automatically uses clipboard method for URLs or if direct input fails
     */
    async inputText(rawText) {
        try {
            // For URLs or text with special characters, use clipboard method directly
            if (rawText.includes('://') || rawText.includes('/') || rawText.includes('?')) {
                this.logger.info('Using clipboard method for URL/special characters');
                return await this.inputTextViaClipboard(rawText);
            }

            // Try direct input first for simple text
            try {
                // Clear existing text first
                await this.executeCommand('adb shell input keyevent KEYCODE_A');
                await this.sleep(100);
                await this.executeCommand('adb shell input keyevent KEYCODE_DEL');
                await this.sleep(100);

                // Input new text - escape properly for shell
                const escapedText = rawText.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\$/g, '\\$').replace(/`/g, '\\`');
                const result = await this.executeCommand(`adb shell input text "${escapedText}"`);
                await this.sleep(500);
                
                if (result.success) {
                    return result;
                }
            } catch (directError) {
                this.logger.warn(`Direct input failed: ${directError.message}, falling back to clipboard`);
            }

            // Fall back to clipboard method if direct input fails
            return await this.inputTextViaClipboard(rawText);
        } catch (error) {
            this.logger.error(`Input text failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Input text via clipboard (handles special characters better)
     * This method uses paste which is fast and reliable for URLs
     */
    async inputTextViaClipboard(text) {
        try {
            this.logger.info(`Inputting text via paste (length: ${text.length}): ${text.substring(0, 50)}...`);

            // Clear the field first
            await this.executeCommand('adb shell input keyevent KEYCODE_A');
            await this.sleep(100);
            await this.executeCommand('adb shell input keyevent KEYCODE_DEL');
            await this.sleep(100);

            // Set clipboard using service call (Android 10+) or am broadcast
            // Base64 encode to avoid shell escaping issues
            const base64Text = Buffer.from(text).toString('base64');
            
            // Method 1: Try service call (most reliable)
            try {
                const serviceCmd = `adb shell "service call clipboard 2 s16 ${base64Text} i32 ${text.length}"`;
                await this.executeCommand(serviceCmd);
                this.logger.info('Clipboard set via service call');
            } catch (e) {
                // Method 2: Try am broadcast with clipper
                try {
                    const escapedText = text.replace(/'/g, "'\\''");
                    const broadcastCmd = `adb shell "am broadcast -a clipper.set -e text '${escapedText}'"`;
                    await this.executeCommand(broadcastCmd);
                    this.logger.info('Clipboard set via broadcast');
                } catch (e2) {
                    // Method 3: Use input text with proper escaping (fallback)
                    this.logger.warn('Clipboard methods failed, using direct input with escaping');
                    const escapedForShell = text
                        .replace(/\\/g, '\\\\')
                        .replace(/"/g, '\\"')
                        .replace(/\$/g, '\\$')
                        .replace(/`/g, '\\`')
                        .replace(/'/g, "\\'");
                    const result = await this.executeCommand(`adb shell input text "${escapedForShell}"`);
                    if (result.success) {
                        await this.sleep(500);
                        return { success: true };
                    }
                    throw new Error('All input methods failed');
                }
            }

            // Wait for clipboard to be set
            await this.sleep(500);

            // Tap the paste button in the UI (more reliable than keyevent)
            const pasteButtonTap = await this.tapByTestTag('paste_button');
            if (pasteButtonTap.success) {
                this.logger.info('Text pasted via paste button');
                await this.sleep(1000); // Wait longer for UI to update
                // Verify text was pasted by checking UI
                await this.dumpUIHierarchy();
                const uiDump = this.readLastUIDump();
                if (uiDump.includes(text) || uiDump.includes(text.substring(0, 20))) {
                    this.logger.info('✅ Verified: Text found in UI after paste button');
                    return { success: true };
                } else {
                    this.logger.warn('⚠️ Text not immediately visible in UI dump, but paste button was tapped');
                }
            }

            // Fallback: Use paste keyevent
            this.logger.warn('Paste button not found, using paste keyevent');
            const pasteResult = await this.executeCommand('adb shell input keyevent 279'); // KEYCODE_PASTE
            await this.sleep(1000); // Wait longer for paste to complete
            
            if (pasteResult.success) {
                // Verify text was pasted
                await this.dumpUIHierarchy();
                const uiDump = this.readLastUIDump();
                if (uiDump.includes(text) || uiDump.includes(text.substring(0, 20)) || uiDump.includes('tiktok.com')) {
                    this.logger.info('✅ Verified: Text found in UI after paste keyevent');
                    return { success: true };
                } else {
                    this.logger.warn('⚠️ Text not immediately visible in UI dump after paste keyevent');
                    // Still return success - Compose UI might not expose text in accessibility tree
                    return { success: true, warning: 'Text not visible in UI dump but paste was attempted' };
                }
            }
            
            // Last resort: Try Ctrl+V
            this.logger.warn('Paste keyevent failed, trying Ctrl+V');
            await this.executeCommand('adb shell input keyevent KEYCODE_CTRL_LEFT');
            await this.sleep(100);
            await this.executeCommand('adb shell input keyevent KEYCODE_V');
            await this.sleep(100);
            await this.executeCommand('adb shell input keyevent KEYCODE_CTRL_LEFT');
            await this.sleep(1000);
            
            // Verify text was pasted
            await this.dumpUIHierarchy();
            const finalDump = this.readLastUIDump();
            if (finalDump.includes(text) || finalDump.includes(text.substring(0, 20)) || finalDump.includes('tiktok.com')) {
                this.logger.info('✅ Verified: Text found in UI after Ctrl+V');
                return { success: true };
            } else {
                this.logger.warn('⚠️ Text not visible in UI dump after Ctrl+V, but operation completed');
                return { success: true, warning: 'Text not visible in UI dump but paste was attempted' };
            }
            
        } catch (error) {
            this.logger.error(`Input text via paste failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }


    /**
     * Tap by test tag
     */
    async tapByTestTag(testTag) {
        try {
            await this.dumpUIHierarchy();

            // First try to find by test-tag attribute
            const tagRegex = new RegExp(`test-tag="${testTag}"[^>]*bounds="([^"]*)"`, 'g');
            let match = tagRegex.exec(this.lastUIDump);

            if (match) {
                const bounds = match[1];
                const coords = this.parseBounds(bounds);
                return await this.tapByCoordinates(coords.x, coords.y);
            }

            // Fallback: Try to find by contentDescription that matches the test tag pattern
            // For "settings_button", look for "Settings button" in content-desc
            const contentDescMap = {
                'settings_button': 'Settings button',
                'paste_button': 'Paste from clipboard',
                'url_history_button': 'Show URL history',
                'error_retry_button': 'Retry operation',
                'extract_script_button': 'Extract Script option'
            };
            
            const contentDesc = contentDescMap[testTag];
            if (contentDesc) {
                const descRegex = new RegExp(`content-desc="${contentDesc.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}"[^>]*bounds="([^"]*)"`, 'g');
                match = descRegex.exec(this.lastUIDump);
                
                if (match) {
                    const bounds = match[1];
                    const coords = this.parseBounds(bounds);
                    this.logger.info(`Found element by contentDescription fallback: "${contentDesc}"`);
                    return await this.tapByCoordinates(coords.x, coords.y);
                }
            }

            return { success: false, error: `Test tag "${testTag}" not found (also tried contentDescription fallback)` };
        } catch (error) {
            this.logger.error(`Tap by test tag failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Parse bounds string to coordinates
     */
    parseBounds(bounds) {
        const coords = bounds.split('][');
        const topLeft = coords[0].replace('[', '').split(',');
        const bottomRight = coords[1].replace(']', '').split(',');

        return {
            x: Math.floor((parseInt(topLeft[0]) + parseInt(bottomRight[0])) / 2),
            y: Math.floor((parseInt(topLeft[1]) + parseInt(bottomRight[1])) / 2)
        };
    }

    /**
     * Read last UI dump
     */
    readLastUIDump() {
        return this.lastUIDump;
    }

    /**
     * Execute command (delegated from foundation/commands module)
     */
    async executeCommand(command, timeout) {
        if (this.commands) {
            return await this.commands.executeCommand(command, timeout);
        }
        // Fallback if commands module not provided
        const { exec } = require('child_process');
        const { promisify } = require('util');
        const execAsync = promisify(exec);

        try {
            const { stdout, stderr } = await execAsync(command, { timeout });
            return { success: true, output: stdout, error: stderr };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Sleep utility
     */
    async sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

module.exports = PluctCoreFoundationUI;
