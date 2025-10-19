// Pluct-Core-03Foundation.js
// Consolidated core functionality - Single source of truth
// Format: [Project]-[ParentScope]-[ChildScope]-[increment][CoreResponsibility]

const { execOk, execOut } = require('./Pluct-Test-Core-Exec');
const { logInfo, logWarn, logSuccess, logError } = require('./Logger');
const path = require('path');
const fs = require('fs');

class PluctCoreFoundation {
    constructor() {
        this.config = this.loadConfig();
    }

    loadConfig() {
        try {
            const configPath = path.join(__dirname, '../config/Pluct-Test-Config-Defaults.json');
            if (fs.existsSync(configPath)) {
                const raw = fs.readFileSync(configPath, 'utf8');
                return JSON.parse(raw);
            }
        } catch (error) {
            logWarn(`Failed to load config: ${error.message}`, 'Core');
        }
        return {};
    }

    // Unified execution methods
    async executeCommand(command, timeout = 30000) {
        try {
            const result = execOut(command);
            return { success: true, output: result, error: null };
        } catch (error) {
            return { success: false, output: null, error: error.message };
        }
    }

    // Unified logging methods
    logInfo(message, component = 'Core') {
        logInfo(message, component);
    }

    logSuccess(message, component = 'Core') {
        logSuccess(message, component);
    }

    logWarn(message, component = 'Core') {
        logWarn(message, component);
    }

    logError(message, component = 'Core') {
        logError(message, component);
    }

    // Utility methods
    async sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    // Environment validation
    async validateEnvironment() {
        this.logInfo('Validating environment...', 'Core');
        
        try {
            // Check ADB connection
            const adbResult = await this.executeCommand('adb devices');
            if (!adbResult.success || !adbResult.output.includes('device')) {
                return { success: false, error: 'ADB device not connected' };
            }

            // Check app installation
            const appResult = await this.executeCommand('adb shell pm list packages | findstr app.pluct');
            if (!appResult.success || !appResult.output.includes('app.pluct')) {
                return { success: false, error: 'Pluct app not installed' };
            }

            this.logSuccess('Environment validation passed', 'Core');
            return { success: true };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    // JWT detection
    async detectJWTGeneration() {
        this.logInfo('Detecting JWT generation...', 'Core');
        
        try {
            const result = await this.executeCommand('adb logcat -d | findstr "JWT"');
            if (result.success && result.output && result.output.trim()) {
                this.logSuccess('JWT generation detected in session', 'Core');
                return { found: true, logs: result.output };
            }
            
            this.logWarn('No JWT generation detected', 'Core');
            return { found: false, logs: null };
        } catch (error) {
            this.logError(`JWT detection failed: ${error.message}`, 'Core');
            return { found: false, logs: null };
        }
    }

    // API connectivity validation
    async validateAPIConnectivity() {
        this.logInfo('Validating API connectivity...', 'Core');
        
        try {
            const healthResult = await this.executeCommand('curl -s https://pluct-business-engine.romeo-lya2.workers.dev/health');
            if (healthResult.success && healthResult.output) {
                const healthData = JSON.parse(healthResult.output);
                if (healthData.status === 'ok') {
                    this.logSuccess('Business Engine API is healthy', 'Core');
                    return { success: true, data: healthData };
                }
            }
            
            this.logWarn('API connectivity validation failed', 'Core');
            return { success: false };
        } catch (error) {
            this.logError(`API connectivity validation failed: ${error.message}`, 'Core');
            return { success: false };
        }
    }

    // TTTranscribe connectivity validation
    async validateTTTranscribeConnectivity() {
        this.logInfo('Validating TTTranscribe API connectivity...', 'Core');
        
        try {
            const tttResult = await this.executeCommand('curl -s -I https://pluct-business-engine.romeo-lya2.workers.dev/ttt/transcribe');
            if (tttResult.success && tttResult.output && tttResult.output.includes('HTTP')) {
                this.logSuccess('TTTranscribe API endpoint is reachable', 'Core');
                return { success: true };
            }
            
            this.logWarn('TTTranscribe connectivity validation failed', 'Core');
            return { success: false };
        } catch (error) {
            this.logError(`TTTranscribe connectivity validation failed: ${error.message}`, 'Core');
            return { success: false };
        }
    }

    // Complete API flow validation
    async validateCompleteAPIFlow() {
        this.logInfo('Validating complete API flow...', 'Core');
        
        try {
            const apiResult = await this.validateAPIConnectivity();
            if (!apiResult.success) {
                return { success: false, error: 'API connectivity failed' };
            }

            const tttResult = await this.validateTTTranscribeConnectivity();
            if (!tttResult.success) {
                return { success: false, error: 'TTTranscribe connectivity failed' };
            }

            this.logSuccess('Complete API flow validation passed', 'Core');
            return { success: true };
        } catch (error) {
            this.logError(`Complete API flow validation failed: ${error.message}`, 'Core');
            return { success: false, error: error.message };
        }
    }

    // App foreground validation
    async validateAppForeground() {
        this.logInfo('Validating app is in foreground before UI interaction...', 'Core');
        
        try {
            const result = await this.executeCommand('adb shell dumpsys window windows | findstr "mCurrentFocus"');
            if (result.success && result.output && result.output.includes('app.pluct')) {
                this.logSuccess('Pluct app confirmed in foreground', 'Core');
                return { success: true };
            }
            
            this.logWarn('Pluct app not in foreground, attempting to bring to foreground', 'Core');
            return await this.ensurePluctAppForeground();
        } catch (error) {
            this.logError(`App foreground validation failed: ${error.message}`, 'Core');
            return { success: false, error: error.message };
        }
    }

    // Ensure Pluct app is in foreground
    async ensurePluctAppForeground() {
        this.logInfo('Ensuring Pluct app is in foreground...', 'Core');
        
        try {
            const result = await this.executeCommand('adb shell am start -n app.pluct/.MainActivity');
            if (result.success) {
                await this.sleep(2000);
                this.logSuccess('Pluct app brought to foreground', 'Core');
                return { success: true };
            }
            
            this.logWarn('Failed to bring Pluct app to foreground', 'Core');
            return { success: false };
        } catch (error) {
            this.logError(`Failed to bring Pluct app to foreground: ${error.message}`, 'Core');
            return { success: false, error: error.message };
        }
    }

    // Device stabilization
    async stabilizeDevice() {
        this.logInfo('Stabilizing device for precise UI interactions', 'Core');
        
        try {
            // Clear app state
            await this.executeCommand('adb shell am force-stop app.pluct');
            await this.sleep(1000);
            
            // Clear logcat
            await this.executeCommand('adb logcat -c');
            
            // Restart app
            await this.executeCommand('adb shell am start -n app.pluct/.MainActivity');
            await this.sleep(3000);
            
            this.logSuccess('Device stabilized successfully', 'Core');
            return { success: true };
        } catch (error) {
            this.logError(`Device stabilization failed: ${error.message}`, 'Core');
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctCoreFoundation;
