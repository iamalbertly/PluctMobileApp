/**
 * Pluct-Core-01Foundation-03UI - UI interaction module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Handles UI interactions, element detection, and user input
 */
class PluctCoreFoundationUI {
    constructor(config, logger) {
        this.config = config;
        this.logger = logger;
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
            }
            
            return result;
        } catch (error) {
            this.logger.error(`App launch failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Check if app is running
     */
    async isAppRunning() {
        try {
            const result = await this.executeCommand('adb shell dumpsys activity activities | findstr -i pluct');
            return { success: result.success && result.output.includes('pluct') };
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
     * Dump UI hierarchy
     */
    async dumpUIHierarchy() {
        try {
            const result = await this.executeCommand('adb shell uiautomator dump /sdcard/ui_dump.xml');
            
            if (result.success) {
                const pullResult = await this.executeCommand('adb pull /sdcard/ui_dump.xml ui_dump.xml');
                if (pullResult.success) {
                    const fs = require('fs');
                    this.lastUIDump = fs.readFileSync('ui_dump.xml', 'utf8');
                    return { success: true, uiDump: this.lastUIDump };
                }
            }
            
            return { success: false, error: 'UI dump failed' };
        } catch (error) {
            this.logger.error(`UI dump failed: ${error.message}`);
            return { success: false, error: error.message };
        }
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
     */
    async inputText(rawText) {
        try {
            // Clear existing text first
            await this.executeCommand('adb shell input keyevent KEYCODE_A');
            await this.sleep(100);
            await this.executeCommand('adb shell input keyevent KEYCODE_DEL');
            await this.sleep(100);
            
            // Input new text
            const escapedText = rawText.replace(/"/g, '\\"');
            const result = await this.executeCommand(`adb shell input text "${escapedText}"`);
            await this.sleep(500);
            return result;
        } catch (error) {
            this.logger.error(`Input text failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Tap by test tag
     */
    async tapByTestTag(testTag) {
        try {
            await this.dumpUIHierarchy();
            
            const tagRegex = new RegExp(`test-tag="${testTag}"[^>]*bounds="([^"]*)"`, 'g');
            const match = tagRegex.exec(this.lastUIDump);
            
            if (match) {
                const bounds = match[1];
                const coords = this.parseBounds(bounds);
                return await this.tapByCoordinates(coords.x, coords.y);
            }
            
            return { success: false, error: `Test tag "${testTag}" not found` };
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
     * Execute command (delegated from foundation)
     */
    async executeCommand(command, timeout) {
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
