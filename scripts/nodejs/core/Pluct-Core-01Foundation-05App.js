/**
 * Pluct-Core-01Foundation-05App - App interaction functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Adheres to 300-line limit with smart separation of concerns
 */

class PluctCoreFoundationApp {
    constructor() {
        this.logger = new PluctLogger();
        this.appState = {
            isForeground: false,
            currentActivity: null,
            lastInteraction: null
        };
    }

    /**
     * Ensure app is in foreground
     */
    async ensureAppForeground() {
        try {
            this.logger.info('📱 Ensuring app is in foreground...');
            
            // Check if app is already in foreground
            const isForeground = await this.isAppInForeground();
            if (isForeground) {
                this.logger.info('✅ App is already in foreground');
                return { success: true };
            }
            
            // Launch app
            const launchResult = await this.launchApp();
            if (!launchResult.success) {
                throw new Error('Failed to launch app');
            }
            
            // Wait for app to be in foreground
            const waitResult = await this.waitForAppForeground(10000);
            if (!waitResult.success) {
                throw new Error('App did not come to foreground');
            }
            
            this.appState.isForeground = true;
            this.logger.info('✅ App is now in foreground');
            
            return { success: true };
        } catch (error) {
            this.logger.error('❌ Failed to ensure app foreground:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Check if app is in foreground
     */
    async isAppInForeground() {
        try {
            const result = await this.executeCommand('adb shell dumpsys activity activities | findstr -i "app.pluct"');
            if (!result.success) {
                return false;
            }
            
            const isForeground = result.output.includes('app.pluct');
            this.appState.isForeground = isForeground;
            
            return isForeground;
        } catch (error) {
            this.logger.warn('⚠️ Failed to check app foreground status:', error.message);
            return false;
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
            
            const result = await this.executeCommand('adb shell am start -n app.pluct/.MainActivity');
            
            // Check if the command succeeded or if the app is already running
            if (result.success || (result.error && result.error.includes('currently running'))) {
                this.logger.info('✅ App launched successfully');
                return { success: true };
            }
            
            throw new Error('Failed to launch app');
        } catch (error) {
            this.logger.error('❌ Failed to launch app:', error.message);
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
     * Wait for app to be in foreground
     */
    async waitForAppForeground(timeout = 10000) {
        const startTime = Date.now();
        
        while (Date.now() - startTime < timeout) {
            const isForeground = await this.isAppInForeground();
            if (isForeground) {
                this.logger.info('✅ App is in foreground');
                return { success: true };
            }
            
            await this.sleep(500);
        }
        
        this.logger.warn('⚠️ App did not come to foreground within timeout');
        return { success: false, error: 'Timeout waiting for app foreground' };
    }

    /**
     * Check if capture sheet is already open
     */
    async isCaptureSheetOpen() {
        try {
            const result = await this.dumpUIHierarchy();
            if (!result.success) {
                return false;
            }
            
            const uiContent = result.uiDump;
            
            // Check for capture sheet indicators
            const captureSheetIndicators = [
                'Capture This Insight',
                'Process Video',
                'TikTok URL',
                'Close'
            ];
            
            let foundIndicators = 0;
            for (const indicator of captureSheetIndicators) {
                if (uiContent.includes(indicator)) {
                    foundIndicators++;
                }
            }
            
            // If we find at least 3 indicators, the capture sheet is likely open
            return foundIndicators >= 3;
        } catch (error) {
            this.logger.error('❌ Failed to check capture sheet status:', error.message);
            return false;
        }
    }

    /**
     * Open capture sheet
     */
    async openCaptureSheet() {
        try {
            console.log('DEBUG: openCaptureSheet called');
            this.logger.info('📱 Opening capture sheet...');
            
            // First check if capture sheet is already open
            try {
                const isAlreadyOpen = await this.isCaptureSheetOpen();
                if (isAlreadyOpen) {
                    this.logger.info('✅ Capture sheet is already open');
                    return { success: true };
                }
            } catch (error) {
                this.logger.warn('⚠️ Failed to check if capture sheet is open:', error.message);
            }
            
            // Check if any other sheet is open (e.g., QuickScan sheet) and close it
            try {
                await this.dumpUIHierarchy();
                const uiDump = this.readLastUIDump();
                this.logger.info(`📱 UI dump length: ${uiDump.length}`);
                if (uiDump.includes('Choose Processing Tier') || uiDump.includes('Quick Scan') || uiDump.includes('Standard')) {
                    this.logger.info('📱 Closing other sheets first...');
                    await this.executeCommand('adb shell input keyevent 4'); // Back button
                    await this.sleep(1000);
                } else {
                    this.logger.info('📱 No other sheets detected');
                }
            } catch (error) {
                this.logger.warn('⚠️ Failed to check for other sheets:', error.message);
            }
            
            // Try to find and tap by content description first (most reliable)
            const contentResult = await this.tapByContentDescription('Capture Insight');
            if (contentResult.success) {
                // Wait for capture sheet to appear
                await this.sleep(2000);
                const isOpen = await this.isCaptureSheetOpen();
                if (isOpen) {
                    this.logger.info('✅ Capture sheet opened via content description');
                    return { success: true };
                }
            }
            
            // Try to find and tap the FAB
            const fabResult = await this.tapByTestTag('capture_fab');
            if (fabResult.success) {
                // Wait for capture sheet to appear
                await this.sleep(2000);
                const isOpen = await this.isCaptureSheetOpen();
                if (isOpen) {
                    this.logger.info('✅ Capture sheet opened via FAB');
                    return { success: true };
                }
            }
            
            // Try to find and tap by text
            const textResult = await this.tapByText('Capture This Insight');
            if (textResult.success) {
                // Wait for capture sheet to appear
                await this.sleep(2000);
                const isOpen = await this.isCaptureSheetOpen();
                if (isOpen) {
                    this.logger.info('✅ Capture sheet opened via text');
                    return { success: true };
                }
            }
            
            throw new Error('Could not find capture sheet trigger');
        } catch (error) {
            this.logger.error('❌ Failed to open capture sheet:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Tap by content description
     */
    async tapByContentDescription(contentDesc) {
        try {
            this.logger.info(`🔍 Tapping by content description: "${contentDesc}"`);
            
            // First dump UI to find the element
            await this.dumpUIHierarchy();
            const uiDump = this.readLastUIDump();
            
            const element = this.findElementByContentDescription(uiDump, contentDesc);
            if (!element) {
                throw new Error(`Element with content description "${contentDesc}" not found`);
            }
            
            const result = await this.tapElement(element);
            if (!result.success) {
                throw new Error('Failed to tap element');
            }
            
            this.appState.lastInteraction = { type: 'tapByContentDescription', contentDesc, timestamp: Date.now() };
            this.logger.info(`✅ Tapped by content description: "${contentDesc}"`);
            
            return { success: true };
        } catch (error) {
            this.logger.error(`❌ Failed to tap by content description "${contentDesc}":`, error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Find element by content description
     */
    findElementByContentDescription(uiDump, contentDesc) {
        // First find the content description and its bounds
        const contentRegex = new RegExp(`content-desc="${contentDesc.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}"[^>]*bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"`, 'i');
        const contentMatch = uiDump.match(contentRegex);
        
        if (!contentMatch) {
            this.logger.warn(`Content description "${contentDesc}" not found in UI dump`);
            return null;
        }
        
        const [, ix1, iy1, ix2, iy2] = contentMatch;
        const iconX1 = parseInt(ix1);
        const iconY1 = parseInt(iy1);
        const iconX2 = parseInt(ix2);
        const iconY2 = parseInt(iy2);
        
        // Find all clickable elements and check if they contain the icon
        const clickableRegex = /clickable="true"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/g;
        let clickableMatch;
        let parentBounds = null;
        
        while ((clickableMatch = clickableRegex.exec(uiDump)) !== null) {
            const [, x1, y1, x2, y2] = clickableMatch;
            const elemX1 = parseInt(x1);
            const elemY1 = parseInt(y1);
            const elemX2 = parseInt(x2);
            const elemY2 = parseInt(y2);
            
            // Check if this clickable element contains the icon
            if (elemX1 <= iconX1 && elemY1 <= iconY1 && elemX2 >= iconX2 && elemY2 >= iconY2) {
                parentBounds = {
                    x1: elemX1,
                    y1: elemY1,
                    x2: elemX2,
                    y2: elemY2
                };
                break;
            }
        }
        
        // If we found a parent clickable element, use its bounds; otherwise use the icon bounds
        const elementBounds = parentBounds || {
            x1: iconX1,
            y1: iconY1,
            x2: iconX2,
            y2: iconY2
        };
        
        return {
            contentDesc,
            bounds: elementBounds
        };
    }

    /**
     * Tap element
     */
    async tapElement(element) {
        const centerX = Math.floor((element.bounds.x1 + element.bounds.x2) / 2);
        const centerY = Math.floor((element.bounds.y1 + element.bounds.y2) / 2);
        
        this.logger.info(`📍 Tapping at coordinates: ${centerX}, ${centerY}`);
        
        return this.executeCommand(`adb shell input tap ${centerX} ${centerY}`);
    }

    /**
     * Get app state
     */
    getAppState() {
        return this.appState;
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
     * Dump UI hierarchy
     */
    async dumpUIHierarchy() {
        try {
            const result = await this.executeCommand('adb shell uiautomator dump /sdcard/ui_dump.xml');
            if (!result.success) {
                throw new Error('Failed to dump UI hierarchy');
            }
            
            // Pull to artifacts/ui directory
            const fs = require('fs');
            if (!fs.existsSync('./artifacts/ui')) {
                fs.mkdirSync('./artifacts/ui', { recursive: true });
            }
            
            const pullResult = await this.executeCommand('adb pull /sdcard/ui_dump.xml ./artifacts/ui/ui_dump.xml');
            if (!pullResult.success) {
                throw new Error('Failed to pull UI dump');
            }
            
            const uiDump = fs.readFileSync('./artifacts/ui/ui_dump.xml', 'utf8');
            
            return { success: true, uiDump };
        } catch (error) {
            this.logger.error('❌ Failed to dump UI hierarchy:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Read last UI dump
     */
    readLastUIDump() {
        try {
            const fs = require('fs');
            const paths = ['./artifacts/ui/ui_dump.xml', './ui_dump.xml'];
            for (const path of paths) {
                if (fs.existsSync(path)) {
                    return fs.readFileSync(path, 'utf8');
                }
            }
            return '';
        } catch (error) {
            this.logger.warn('⚠️ Failed to read UI dump:', error.message);
            return '';
        }
    }

    /**
     * Tap by test tag
     */
    async tapByTestTag(testTag) {
        try {
            this.logger.info(`🔍 Tapping by test tag: "${testTag}"`);
            
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
            
            this.appState.lastInteraction = { type: 'tapByTestTag', testTag, timestamp: Date.now() };
            this.logger.info(`✅ Tapped by test tag: "${testTag}"`);
            
            return { success: true };
        } catch (error) {
            this.logger.error(`❌ Failed to tap by test tag "${testTag}":`, error.message);
            return { success: false, error: error.message };
        }
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
     * Tap by text
     */
    async tapByText(text) {
        try {
            this.logger.info(`🔍 Tapping by text: "${text}"`);
            
            const result = await this.executeCommand(`adb shell input text "${text}"`);
            if (!result.success) {
                throw new Error('Failed to tap by text');
            }
            
            this.appState.lastInteraction = { type: 'tapByText', text, timestamp: Date.now() };
            this.logger.info(`✅ Tapped by text: "${text}"`);
            
            return { success: true };
        } catch (error) {
            this.logger.error(`❌ Failed to tap by text "${text}":`, error.message);
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

module.exports = PluctCoreFoundationApp;
