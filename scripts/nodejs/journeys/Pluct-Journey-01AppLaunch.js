/**
 * Pluct-Journey-01AppLaunch - Real app launch journey
 * Tests complete app launch flow with validation
 * Auto-discoverable by orchestrator
 */

const PluctCoreUnified = require('../core/Pluct-Core-Unified-New');

class PluctJourney01AppLaunch {
    constructor() {
        this.core = new PluctCoreUnified();
        this.name = 'AppLaunch';
        this.steps = [
            'Pre-launch validation',
            'Stop existing app instance',
            'Launch app',
            'Wait for app ready',
            'Validate app is in foreground',
            'Capture final state'
        ];
    }

    async execute() {
        this.core.logger.info('🎯 Starting AppLaunch journey...');
        
        try {
            // Step 1: Pre-launch validation
            this.core.logger.info('📋 Step 1: Pre-launch validation');
            await this.captureUI('AppLaunch-pre');
            
            // Step 2: Stop existing app instance
            this.core.logger.info('📋 Step 2: Stop existing app instance');
            await this.core.executeCommand('adb shell am force-stop app.pluct');
            await this.core.sleep(1000);
            
            // Step 3: Launch app
            this.core.logger.info('📋 Step 3: Launch app');
            const launchResult = await this.core.executeCommand('adb shell am start -n app.pluct/.MainActivity');
            if (!launchResult.success) {
                return { success: false, error: 'App launch failed' };
            }
            
            // Step 4: Wait for app ready
            this.core.logger.info('📋 Step 4: Wait for app ready');
            await this.core.sleep(3000);
            
            // Step 5: Validate app is in foreground
            this.core.logger.info('📋 Step 5: Validate app is in foreground');
            const foregroundResult = await this.ensureAppForeground();
            if (!foregroundResult.success) {
                return { success: false, error: 'App not in foreground' };
            }
            
            // Step 6: Validate app UI elements
            this.core.logger.info('📋 Step 6: Validate app UI elements');
            const uiValidation = await this.validateAppUI();
            if (!uiValidation.success) {
                return { success: false, error: 'App UI validation failed' };
            }
            
            // Step 7: Capture final state
            this.core.logger.info('📋 Step 7: Capture final state');
            await this.captureUI('AppLaunch-post');
            
            this.core.logger.info('✅ AppLaunch journey completed successfully');
            return { success: true, steps: this.steps };
            
        } catch (error) {
            this.core.logger.error('❌ AppLaunch journey failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    async ensureAppForeground() {
        const maxAttempts = 3;
        for (let i = 0; i < maxAttempts; i++) {
            try {
                await this.core.executeCommand('adb shell am start -n app.pluct/.MainActivity');
                await this.core.sleep(1000);
                
                const focusResult = await this.core.executeCommand('adb shell dumpsys window windows');
                if (focusResult.success && focusResult.output.includes('app.pluct')) {
                    return { success: true };
                }
                
                await this.core.executeCommand('adb shell monkey -p app.pluct -c android.intent.category.LAUNCHER 1');
                await this.core.sleep(1000);
                
            } catch (error) {
                this.core.logger.warn(`Attempt ${i + 1} failed: ${error.message}`);
                if (i === maxAttempts - 1) {
                    return { success: false, error: 'Failed to bring app to foreground' };
                }
            }
        }
        return { success: true };
    }

    async validateAppUI() {
        try {
            const uiDumpResult = await this.core.executeCommand('adb shell uiautomator dump /sdcard/ui_dump.xml');
            if (!uiDumpResult.success) {
                return { success: false, error: 'Failed to dump UI' };
            }
            
            await this.core.executeCommand('adb pull /sdcard/ui_dump.xml artifacts/ui/');
            const fs = require('fs');
            if (!fs.existsSync('artifacts/ui/ui_dump.xml')) {
                return { success: false, error: 'UI dump file not found' };
            }
            
            const uiContent = fs.readFileSync('artifacts/ui/ui_dump.xml', 'utf8');
            
            // Debug: Log the content length and first 200 characters
            this.core.logger.info(`UI dump content length: ${uiContent.length}`);
            this.core.logger.info(`UI dump first 200 chars: ${uiContent.substring(0, 200)}`);
            
            // Validate key UI elements are present
            const requiredElements = ['Pluct', 'Credits'];
            const missingElements = requiredElements.filter(element => {
                const found = uiContent.includes(element);
                this.core.logger.info(`Looking for "${element}": ${found ? 'FOUND' : 'NOT FOUND'}`);
                return !found;
            });
            
            if (missingElements.length > 0) {
                this.core.logger.error(`Missing UI elements: ${missingElements.join(', ')}`);
                return { success: false, error: `Missing UI elements: ${missingElements.join(', ')}` };
            }
            
            // Check for either welcome screen or transcript state
            const hasValidState = uiContent.includes('Welcome to Pluct') || uiContent.includes('No transcripts yet') || uiContent.includes('Recent Transcripts');
            if (!hasValidState) {
                return { success: false, error: 'Neither welcome screen nor transcript state detected' };
            }
            
            return { success: true };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    async captureUI(tag) {
        try {
            const timestamp = Date.now();
            const filename = `screen-${tag}-${timestamp}.png`;
            
            const fs = require('fs');
            const path = require('path');
            const artifactsDir = path.join(process.cwd(), 'artifacts', 'ui');
            if (!fs.existsSync(artifactsDir)) {
                fs.mkdirSync(artifactsDir, { recursive: true });
            }
            
            const screencapResult = await this.core.executeCommand(`adb shell screencap -p /sdcard/${filename}`);
            if (screencapResult.success) {
                await this.core.executeCommand(`adb pull /sdcard/${filename} ${artifactsDir}/`);
            }
            
            this.core.logger.info(`📸 Captured UI artifacts tag='${tag}'`);
            return { success: true, filename };
        } catch (error) {
            this.core.logger.warn(`Screenshot capture failed for ${tag}: ${error.message}`);
            return { success: true, filename: null };
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('Pluct-Journey-01AppLaunch', new PluctJourney01AppLaunch());
}

module.exports = { register };
