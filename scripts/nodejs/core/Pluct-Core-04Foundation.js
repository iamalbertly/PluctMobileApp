/**
 * Pluct-Core-04Foundation - Consolidated core foundation utilities
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[next stage increment to the childscope][CoreResponsibility]
 * Consolidated from Pluct-Core-02Foundation.js (723 lines) to maintain 300-line limit
 */

const { exec } = require('child_process');
const fs = require('fs');
const path = require('path');

class PluctCore04Foundation {
    constructor() {
        this.config = null;
        this.loadConfig();
    }

    /**
     * Load configuration from defaults
     */
    loadConfig() {
        try {
            const configPath = path.join(__dirname, 'config', 'Pluct-Test-Config-Defaults.json');
            this.config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
        } catch (error) {
            console.warn('⚠️ Could not load config, using defaults');
            this.config = {
                url: "https://vm.tiktok.com/ZMADQVF4e/",
                package: "app.pluct",
                activity: "app.pluct/.MainActivity"
            };
        }
    }

    /**
     * Execute command with proper error handling
     */
    async executeCommand(command, timeout = 30000) {
        return new Promise((resolve, reject) => {
            const child = exec(command, { timeout }, (error, stdout, stderr) => {
                if (error) {
                    reject({ success: false, error: error.message, stderr });
                } else {
                    resolve({ success: true, output: stdout, stderr });
                }
            });
        });
    }

    /**
     * Sleep utility
     */
    async sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    /**
     * Load object from file
     */
    loadObject(filePath) {
        try {
            if (fs.existsSync(filePath)) {
                const content = fs.readFileSync(filePath, 'utf8');
                return JSON.parse(content);
            }
        } catch (error) {
            console.warn(`⚠️ Could not load object from ${filePath}: ${error.message}`);
        }
        return null;
    }

    /**
     * Detect JWT generation in logs
     */
    async detectJWTGeneration() {
        try {
            const result = await this.executeCommand('adb logcat -d | findstr "JWT"');
            if (result.success && result.output.trim()) {
                return {
                    success: true,
                    logs: result.output,
                    patterns: [
                        'JWT Generated for user',
                        'JWT token generated',
                        'JWT GENERATED FOR',
                        'JWT GENERATION COMPLETED'
                    ]
                };
            }
            return { success: false, logs: '', patterns: [] };
        } catch (error) {
            return { success: false, error: error.message, logs: '', patterns: [] };
        }
    }

    /**
     * Validate API connectivity
     */
    async validateAPIConnectivity() {
        try {
            const result = await this.executeCommand('curl -s -o /dev/null -w "%{http_code}" https://business-engine.pluct.app/health');
            return {
                success: result.success && result.output.trim() === '200',
                statusCode: result.output.trim(),
                endpoint: 'https://business-engine.pluct.app/health'
            };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Validate TTTranscribe connectivity
     */
    async validateTTTranscribeConnectivity() {
        try {
            const result = await this.executeCommand('curl -s -o /dev/null -w "%{http_code}" https://ttt.pluct.app/health');
            return {
                success: result.success && result.output.trim() === '200',
                statusCode: result.output.trim(),
                endpoint: 'https://ttt.pluct.app/health'
            };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Validate complete API flow
     */
    async validateCompleteAPIFlow() {
        console.log('🎯 Validating complete API flow...');
        
        // Validate Business Engine API
        const businessEngineResult = await this.validateAPIConnectivity();
        if (!businessEngineResult.success) {
            console.warn('⚠️ Business Engine API validation failed');
            return { success: false, reason: 'Business Engine API not reachable' };
        }
        console.log('✅ Business Engine API is healthy');

        // Validate TTTranscribe API
        const tttResult = await this.validateTTTranscribeConnectivity();
        if (!tttResult.success) {
            console.warn('⚠️ TTTranscribe API validation failed');
            return { success: false, reason: 'TTTranscribe API not reachable' };
        }
        console.log('✅ TTTranscribe API endpoint is reachable');

        return { success: true, businessEngine: businessEngineResult, ttt: tttResult };
    }

    /**
     * Ensure Pluct app is in foreground
     */
    async ensurePluctAppForeground() {
        try {
            console.log('🎯 Ensuring Pluct app is in foreground...');
            
            // Check if app is in foreground
            const result = await this.executeCommand('adb shell dumpsys window windows | findstr "mCurrentFocus"');
            if (result.success && result.output.includes('app.pluct')) {
                console.log('✅ Pluct app is already in foreground');
                return { success: true, alreadyForeground: true };
            }

            // Bring app to foreground
            console.log('🎯 Pluct app not in foreground, bringing to foreground...');
            await this.executeCommand('adb shell am start -n app.pluct/.MainActivity');
            await this.sleep(3000);
            
            // Verify app is in foreground
            const verifyResult = await this.executeCommand('adb shell dumpsys window windows | findstr "mCurrentFocus"');
            if (verifyResult.success && verifyResult.output.includes('app.pluct')) {
                console.log('✅ Pluct app brought to foreground');
                return { success: true, alreadyForeground: false };
            }

            // Try alternative method
            console.log('🎯 Trying alternative method to bring app to foreground...');
            await this.executeCommand('adb shell monkey -p app.pluct -c android.intent.category.LAUNCHER 1');
            await this.sleep(2000);
            
            const finalResult = await this.executeCommand('adb shell dumpsys window windows | findstr "mCurrentFocus"');
            if (finalResult.success && finalResult.output.includes('app.pluct')) {
                console.log('✅ Pluct app brought to foreground via alternative method');
                return { success: true, alreadyForeground: false };
            }

            console.warn('⚠️ Could not bring Pluct app to foreground');
            return { success: false, reason: 'App not in foreground' };
        } catch (error) {
            console.error('❌ Error ensuring app foreground:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Validate app is in foreground
     */
    async validateAppForeground() {
        try {
            const result = await this.executeCommand('adb shell dumpsys window windows | findstr "mCurrentFocus"');
            const isForeground = result.success && result.output.includes('app.pluct');
            
            if (isForeground) {
                console.log('✅ Pluct app confirmed in foreground');
            } else {
                console.warn('⚠️ Pluct app not in foreground, attempting to bring to foreground');
                return await this.ensurePluctAppForeground();
            }
            
            return { success: true, isForeground };
        } catch (error) {
            console.error('❌ Error validating app foreground:', error.message);
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctCore04Foundation;
