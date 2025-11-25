const PluctCoreFoundationCommands = require('./Pluct-Core-01Foundation-02Commands');
const PluctCoreFoundationUI = require('./Pluct-Core-01Foundation-03UI');
const PluctCoreFoundationValidation = require('./Pluct-Core-01Foundation-04Validation');
const PluctCoreFoundationUtils = require('./Pluct-Core-01Foundation-05Utils');
const { logInfo, logSuccess, logWarn, logError } = require('./Logger');

/**
 * Pluct-Core-01Foundation - Core foundation functionality orchestrator
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Orchestrates all foundation modules for basic operations
 */
class PluctCoreFoundation {
    constructor() {
        this.config = {
            url: 'https://vm.tiktok.com/ZMADQVF4e/',
            businessEngineUrl: 'https://pluct-business-engine.romeo-lya2.workers.dev',
            timeouts: { default: 5000, short: 2000, long: 10000 },
            retry: { maxAttempts: 3, delay: 1000 }
        };
        this.logger = { info: logInfo, success: logSuccess, warn: logWarn, error: logError };
        
        // Initialize specialized modules
        this.commands = new PluctCoreFoundationCommands(this.config, this.logger);
        this.ui = new PluctCoreFoundationUI(this.config, this.logger, this.commands);
        this.validation = new PluctCoreFoundationValidation(this.config, this.logger, this.commands);
        this.utils = new PluctCoreFoundationUtils(this.config, this.logger);
    }

    // Delegate to specialized modules
    async executeCommand(command, timeout) {
        return this.commands.executeCommand(command, timeout);
    }

    async sleep(ms) {
        return this.utils.sleep(ms);
    }

    async validateEnvironment() {
        return this.validation.validateEnvironment();
    }

    async launchApp() {
        return this.ui.launchApp();
    }

    async isAppRunning() {
        return this.ui.isAppRunning();
    }

    async ensureAppForeground() {
        return this.ui.ensureAppForeground();
    }

    async captureUIArtifacts(tag) {
        return this.ui.captureUIArtifacts(tag);
    }

    async dumpUIHierarchy() {
        return this.ui.dumpUIHierarchy();
    }

    async waitForText(text, timeoutMs, pollMs) {
        return this.ui.waitForText(text, timeoutMs, pollMs);
    }

    async tapByText(text) {
        return this.ui.tapByText(text);
    }

    async tapByContentDesc(contentDesc) {
        return this.ui.tapByContentDesc(contentDesc);
    }

    async tapByCoordinates(x, y) {
        return this.ui.tapByCoordinates(x, y);
    }

    async inputText(rawText) {
        return this.ui.inputText(rawText);
    }

    async clearAppCache() {
        return this.commands.clearAppCache();
    }

    async clearWorkManagerTasks() {
        return this.commands.clearWorkManagerTasks();
    }

    async tapByTestTag(testTag) {
        return this.ui.tapByTestTag(testTag);
    }

    async tapFirstEditText() {
        this.logger.info('👆 Tapping first edit text field...');
        try {
            await this.dumpUIHierarchy();
            const uiDump = this.readLastUIDump();
            
            // Look for the first EditText element
            const regex = /class="android\.widget\.EditText".*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/;
            const match = uiDump.match(regex);
            
            if (match) {
                const x1 = parseInt(match[1], 10);
                const y1 = parseInt(match[2], 10);
                const x2 = parseInt(match[3], 10);
                const y2 = parseInt(match[4], 10);
                const centerX = (x1 + x2) / 2;
                const centerY = (y1 + y2) / 2;
                
                this.logger.info(`Found EditText at coordinates: (${centerX}, ${centerY})`);
                return await this.tapByCoordinates(centerX, centerY);
            } else {
                this.logger.warn('⚠️ No EditText found in UI dump');
                return { success: false, error: 'No EditText found' };
            }
        } catch (error) {
            this.logger.error(`❌ Failed to tap first edit text: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    // Additional utility methods
    readLastUIDump() {
        return this.ui.readLastUIDump();
    }

    generateTestJWT(userId) {
        return this.utils.generateTestJWT(userId);
    }

    httpGet(url, headers = {}) {
        return this.utils.httpGet(url, headers);
    }

    httpPost(url, data, headers = {}) {
        return this.utils.httpPost(url, data, headers);
    }

    // Additional navigation methods
    async pressBackButton() {
        return await this.commands.executeCommand('adb shell input keyevent KEYCODE_BACK');
    }

    async pressHomeButton() {
        return await this.commands.executeCommand('adb shell input keyevent KEYCODE_HOME');
    }

    async pressMenuButton() {
        return await this.commands.executeCommand('adb shell input keyevent KEYCODE_MENU');
    }

    // UI interaction methods
    async openCaptureSheet() {
        this.logger.info('🎬 Opening capture sheet...');
        try {
            // Try to tap the FAB (Floating Action Button) to open capture sheet
            const result = await this.tapByContentDesc('Start transcription button');
            if (result.success) {
                this.logger.info('✅ Capture sheet opened successfully');
                return { success: true };
            } else {
                this.logger.warn('⚠️ Could not find FAB, trying alternative method');
                // Alternative: try to tap by coordinates where FAB might be
                await this.tapByCoordinates(650, 1200); // Bottom right area
                await this.sleep(1000);
                return { success: true };
            }
        } catch (error) {
            this.logger.error(`❌ Failed to open capture sheet: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    // Transcription monitoring methods
    async pollTranscriptionStatus(jobId, timeoutMs = 60000, pollMs = 5000) {
        this.logger.info(`⏳ Polling transcription status for job ${jobId} (timeout: ${timeoutMs}ms)...`);
        const startTime = Date.now();
        while (Date.now() - startTime < timeoutMs) {
            const statusResponse = await this.httpGet(`/v1/transcribe/status/${jobId}`);
            if (statusResponse.success && statusResponse.status === 200) {
                const statusData = JSON.parse(statusResponse.body);
                this.logger.info(`Current status for job ${jobId}: ${statusData.status}`);
                if (statusData.status === 'completed') {
                    this.logger.info(`✅ Transcription job ${jobId} completed.`);
                    return { success: true, details: statusData };
                } else if (statusData.status === 'failed') {
                    this.logger.error(`❌ Transcription job ${jobId} failed.`);
                    return { success: false, error: `Transcription job ${jobId} failed.` };
                }
            } else {
                this.logger.warn(`⚠️ Failed to get status for job ${jobId}: ${statusResponse.error || statusResponse.status}`);
            }
            await this.sleep(pollMs);
        }
        this.logger.error(`❌ Timeout polling for transcription job ${jobId}.`);
        return { success: false, error: `Timeout polling for transcription job ${jobId}.` };
    }

    async waitForTranscriptResult(timeoutMs = 160000) {
        this.logger.info(`⏳ Waiting for transcript result (timeout: ${timeoutMs}ms)...`);
        const startTime = Date.now();
        
        while (Date.now() - startTime < timeoutMs) {
            // Check UI for completion indicators
            await this.dumpUIHierarchy();
            const uiDump = this.readLastUIDump();
            
            // Look for completion indicators in the UI
            if (uiDump.includes('transcript') || uiDump.includes('completed') || uiDump.includes('success')) {
                this.logger.info('✅ Transcript result detected in UI');
                return { success: true, message: 'Transcript result found in UI' };
            }
            
            // Check for error indicators
            if (uiDump.includes('error') || uiDump.includes('failed') || uiDump.includes('timeout')) {
                this.logger.warn('⚠️ Error indicators found in UI');
                return { success: false, error: 'Error indicators found in UI' };
            }
            
            // Wait before next check
            await this.sleep(5000);
        }
        
        this.logger.error(`❌ Timeout waiting for transcript result after ${timeoutMs}ms`);
        return { success: false, error: `Timeout waiting for transcript result after ${timeoutMs}ms` };
    }

    // Performance optimization methods
    async comprehensiveOptimization() {
        this.logger.info('🚀 Running comprehensive optimization...');
        try {
            // Clear app cache
            await this.clearAppCache();
            
            // Clear WorkManager tasks
            await this.clearWorkManagerTasks();
            
            // Optimize memory
            await this.commands.executeCommand('adb shell am force-stop app.pluct');
            await this.sleep(1000);
            
            this.logger.info('✅ Comprehensive optimization completed');
            return { success: true };
        } catch (error) {
            this.logger.error(`❌ Comprehensive optimization failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctCoreFoundation;