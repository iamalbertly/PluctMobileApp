/**
 * Pluct-Core-01Foundation - Core foundation functionality
 * Single source of truth for basic operations
 * Adheres to 300-line limit with smart separation of concerns
 */

class PluctCoreFoundation {
    constructor() {
        this.config = {
            url: 'https://vm.tiktok.com/ZMADQVF4e/',
            businessEngineUrl: 'https://pluct-business-engine.romeo-lya2.workers.dev',
            timeouts: { default: 5000, short: 2000, long: 10000 },
            retry: { maxAttempts: 3, delay: 1000 }
        };
        this.logger = new PluctLogger();
    }

    /**
     * Execute command with error handling
     */
    async executeCommand(command, timeout = this.config.timeouts.default) {
        try {
            const { exec } = require('child_process');
            const { promisify } = require('util');
            const execAsync = promisify(exec);
            
            const { stdout, stderr } = await execAsync(command, { timeout });
            
            // Command succeeded if no error was thrown
            return { 
                success: true, 
                output: stdout, 
                error: stderr,
                fullOutput: stdout + stderr
            };
        } catch (error) {
            this.logger.error(`Command failed: ${command}`, error);
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
     * Validate environment
     */
    async validateEnvironment() {
        try {
            // Check ADB connectivity
            const adbResult = await this.executeCommand('adb devices');
            if (!adbResult.success) {
                return { success: false, error: 'ADB not available' };
            }

            // Check if device is connected
            if (!adbResult.output.includes('device')) {
                return { success: false, error: 'No device connected' };
            }

            // Check if app is installed
            const appResult = await this.executeCommand('adb shell pm list packages');
            if (!appResult.success || !appResult.output.includes('app.pluct')) {
                return { success: false, error: 'App not installed' };
            }

            this.logger.info('✅ Environment validation passed');
            return { success: true };
        } catch (error) {
            this.logger.error('❌ Environment validation failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Launch app
     */
    async launchApp() {
        try {
            this.logger.info('🚀 Launching app...');
            
            // First check if app is already running
            const isRunning = await this.isAppRunning();
            if (isRunning) {
                this.logger.info('✅ App is already running');
                return { success: true };
            }
            
            // Start app (will either launch new or bring existing to foreground)
            const result = await this.executeCommand('adb shell am start -n app.pluct/.MainActivity');
            
            // Check if the command succeeded or if the app is already running
            if (result.success || (result.error && result.error.includes('currently running'))) {
                this.logger.info('✅ App launched successfully');
                return { success: true };
            }
            
            throw new Error('Failed to launch app');
        } catch (error) {
            this.logger.error('❌ App launch failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Check if app is running
     */
    async isAppRunning() {
        try {
            const result = await this.executeCommand('adb shell dumpsys activity activities | grep app.pluct');
            return result.success && result.output.includes('app.pluct');
        } catch (error) {
            return false;
        }
    }

    /**
     * Ensure app is in foreground
     */
    async ensureAppForeground() {
        try {
            // Use cross-platform approach - get all activities and check in JavaScript
            const result = await this.executeCommand('adb shell dumpsys activity activities');
            
            if (!result.success || !result.output.includes('app.pluct')) {
                // Try to bring app to foreground
                await this.executeCommand('adb shell am start -n app.pluct/.MainActivity');
                await this.sleep(1000);
            }
            
            return { success: true };
        } catch (error) {
            this.logger.error('❌ Failed to ensure app foreground:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Capture UI artifacts
     */
    async captureUIArtifacts(tag) {
        try {
            const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
            const screenshotPath = `/sdcard/screen-${tag}-${timestamp}.png`;
            
            const result = await this.executeCommand(`adb shell screencap -p ${screenshotPath}`);
            if (result.success) {
                await this.executeCommand(`adb pull ${screenshotPath} artifacts/ui/`);
                this.logger.info(`📸 Screenshot captured: ${tag}`);
            } else {
                this.logger.warn(`⚠️ Screenshot capture failed for ${tag}`);
            }
            
            return { success: true };
        } catch (error) {
            this.logger.error(`❌ UI artifact capture failed for ${tag}:`, error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Dump UI hierarchy
     */
    async dumpUIHierarchy() {
        try {
            const result = await this.executeCommand('adb shell uiautomator dump /sdcard/ui_dump.xml');
            if (result.success) {
                await this.executeCommand('adb pull /sdcard/ui_dump.xml artifacts/ui/');
                this.logger.info('📊 UI hierarchy dumped to: /sdcard/ui_dump.xml');
                
                // Read and display full XML
                const fs = require('fs');
                if (fs.existsSync('artifacts/ui/ui_dump.xml')) {
                    const xmlContent = fs.readFileSync('artifacts/ui/ui_dump.xml', 'utf8');
                    console.log('\n----- UI DUMP (full) -----');
                    console.log(xmlContent);
                    console.log('----- END UI DUMP -----\n');
                }
            }
            
            return { success: true };
        } catch (error) {
            this.logger.error('❌ UI hierarchy dump failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Read last UI dump
     */
    readLastUIDump() {
        try {
            const fs = require('fs');
            if (fs.existsSync('artifacts/ui/ui_dump.xml')) {
                return fs.readFileSync('artifacts/ui/ui_dump.xml', 'utf8');
            }
            return '';
        } catch (error) {
            this.logger.error('❌ Failed to read UI dump:', error.message);
            return '';
        }
    }

    /**
     * Wait for text in UI
     */
    async waitForText(text, timeoutMs = 10000, pollMs = 1000) {
        const startTime = Date.now();
        
        while (Date.now() - startTime < timeoutMs) {
            await this.dumpUIHierarchy();
            const uiDump = this.readLastUIDump();
            
            if (uiDump.includes(text)) {
                this.logger.info(`✅ Found text: ${text}`);
                return { success: true };
            }
            
            await this.sleep(pollMs);
        }
        
        this.logger.warn(`⚠️ Text not found within timeout: ${text}`);
        return { success: false, error: `Text not found: ${text}` };
    }

    /**
     * Find bounds for text
     */
    findBoundsForText(targetText) {
        try {
            const uiDump = this.readLastUIDump();
            
            // Find the exact text match with bounds
            const textRegex = new RegExp(`text="${targetText.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}"[^>]*bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"`, 'i');
            const match = uiDump.match(textRegex);
            
            if (match) {
                const [, x1, y1, x2, y2] = match;
                return {
                    x: Math.floor((parseInt(x1) + parseInt(x2)) / 2),
                    y: Math.floor((parseInt(y1) + parseInt(y2)) / 2)
                };
            }
            
            return null;
        } catch (error) {
            this.logger.error('❌ Failed to find bounds for text:', error.message);
            return null;
        }
    }

    /**
     * Tap by text
     */
    async tapByText(text) {
        try {
            this.logger.info(`🔍 Tapping by text: "${text}"`);
            
            // Dump UI hierarchy first to get fresh state
            await this.executeCommand('adb shell uiautomator dump /sdcard/ui_dump.xml');
            await this.executeCommand('adb pull /sdcard/ui_dump.xml ./artifacts/ui/ui_dump.xml');
            
            // Try to find by text first
            let bounds = this.findBoundsForText(text);
            
            // If not found by text, try by contentDescription
            if (!bounds) {
                const uiDump = this.readLastUIDump();
                const match = uiDump.match(new RegExp(`content-desc="${text}"[^>]*bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"`));
                if (match) {
                    const [, x1, y1, x2, y2] = match;
                    bounds = {
                        x: Math.floor((parseInt(x1) + parseInt(x2)) / 2),
                        y: Math.floor((parseInt(y1) + parseInt(x2)) / 2)
                    };
                }
            }
            
            if (bounds) {
                this.logger.info(`📍 Found element at (${bounds.x}, ${bounds.y})`);
                const result = await this.executeCommand(`adb shell input tap ${bounds.x} ${bounds.y}`);
                if (result.success) {
                    this.logger.info(`✅ Tapped by text: ${text}`);
                    return { success: true };
                }
            }
            
            this.logger.error(`❌ CRITICAL: Could not tap by text: ${text}`);
            throw new Error(`CRITICAL: Could not tap by text: ${text}`);
        } catch (error) {
            this.logger.error(`❌ Tap by text failed: ${text}`, error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Tap by content description
     */
    async tapByContentDesc(contentDesc) {
        try {
            const uiDump = this.readLastUIDump();
            const match = uiDump.match(new RegExp(`content-desc="${contentDesc}"[^>]*bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"`));
            
            if (match) {
                const [, x1, y1, x2, y2] = match;
                const x = Math.floor((parseInt(x1) + parseInt(x2)) / 2);
                const y = Math.floor((parseInt(y1) + parseInt(y2)) / 2);
                
                const result = await this.executeCommand(`adb shell input tap ${x} ${y}`);
                if (result.success) {
                    this.logger.info(`✅ Tapped by content-desc: ${contentDesc}`);
                    return { success: true };
                }
            }
            
            this.logger.error(`❌ CRITICAL: Could not tap by content-desc: ${contentDesc}`);
            throw new Error(`CRITICAL: Could not tap by content-desc: ${contentDesc}`);
        } catch (error) {
            this.logger.error('❌ Tap by content-desc failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Tap by coordinates
     */
    async tapByCoordinates(x, y) {
        try {
            this.logger.info(`🎯 Tapping at coordinates (${x}, ${y})`);
            const result = await this.executeCommand(`adb shell input tap ${x} ${y}`);
            
            if (result.success) {
                this.logger.info(`✅ Tapped at (${x}, ${y})`);
                return { success: true };
            }
            
            return { success: false, error: 'Tap failed' };
        } catch (error) {
            this.logger.error(`❌ Tap at (${x}, ${y}) failed:`, error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Input text safely
     */
    async inputText(rawText) {
        try {
            // Escape special characters
            const escapedText = rawText.replace(/[&|;`$(){}[\]\\]/g, '\\$&');
            const result = await this.executeCommand(`adb shell input text "${escapedText}"`);
            
            if (result.success) {
                this.logger.info(`✅ Text input: ${rawText}`);
                return { success: true };
            }
            
            return { success: false, error: 'Text input failed' };
        } catch (error) {
            this.logger.error(`❌ Text input failed: ${rawText}`, error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Clear app cache
     */
    async clearAppCache() {
        try {
            await this.executeCommand('adb shell pm clear app.pluct');
            this.logger.info('✅ App cache cleared');
            return { success: true };
        } catch (error) {
            this.logger.error('❌ Failed to clear app cache:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Clear WorkManager tasks
     */
    async clearWorkManagerTasks() {
        try {
            await this.executeCommand('adb shell am force-stop app.pluct');
            this.logger.info('✅ WorkManager tasks cleared');
            return { success: true };
        } catch (error) {
            this.logger.error('❌ Failed to clear WorkManager tasks:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Generate test JWT token
     */
    generateTestJWT() {
        // Simple JWT generation for testing
        const header = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url');
        const payload = Buffer.from(JSON.stringify({
            sub: 'mobile',
            scope: 'ttt:transcribe',
            iat: Math.floor(Date.now() / 1000),
            exp: Math.floor(Date.now() / 1000) + 900 // 15 minutes
        })).toString('base64url');
        
        // Use a simple secret for testing
        const secret = 'prod-jwt-secret-Z8qKsL2wDn9rFy6aVbP3tGxE0cH4mN5jR7sT1uC9e';
        const signature = require('crypto')
            .createHmac('sha256', secret)
            .update(`${header}.${payload}`)
            .digest('base64url');
        
        return `${header}.${payload}.${signature}`;
    }

    /**
     * Tap by test tag
     */
    async tapByTestTag(testTag) {
        try {
            const uiDump = this.readLastUIDump();
            const match = uiDump.match(new RegExp(`testTag="${testTag}"[^>]*bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"`));
            
            if (match) {
                const [, x1, y1, x2, y2] = match;
                const x = Math.floor((parseInt(x1) + parseInt(x2)) / 2);
                const y = Math.floor((parseInt(y1) + parseInt(y2)) / 2);
                
                const result = await this.executeCommand(`adb shell input tap ${x} ${y}`);
                if (result.success) {
                    this.logger.info(`✅ Tapped by test tag: ${testTag}`);
                    return { success: true };
                }
            }
            
            this.logger.error(`❌ CRITICAL: Could not tap by test tag: ${testTag}`);
            throw new Error(`CRITICAL: Could not tap by test tag: ${testTag}`);
        } catch (error) {
            this.logger.error(`❌ Tap by test tag failed: ${testTag}`, error.message);
            return { success: false, error: error.message };
        }
    }
}

/**
 * PluctLogger - Logging utility
 */
class PluctLogger {
    constructor() {
        this.logLevel = 'INFO';
    }

    log(level, message, category = '') {
        const timestamp = new Date().toISOString();
        const categoryStr = category ? ` [${category}]` : '';
        console.log(`${timestamp} ${level}${categoryStr} ${message}`);
    }

    error(message, category = '') { this.log('ERROR', message, category); }
    warn(message, category = '') { this.log('WARN', message, category); }
    info(message, category = '') { this.log('INFO', message, category); }
    debug(message, category = '') { this.log('DEBUG', message, category); }
}

module.exports = PluctCoreFoundation;
