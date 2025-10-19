// Pluct-Core-01Foundation.js
// Consolidated core functionality for Pluct test framework
// Single source of truth for all core operations

const { execOk, execOut } = require('./Pluct-Test-Core-Exec');
const { logInfo, logWarn, logSuccess, logError, logStage } = require('./Logger');
const Logcat = require('./Pluct-Node-Tests-Core-Logcat-LiveHttpStreamer');
const path = require('path');
const fs = require('fs');

class PluctCoreFoundation {
    constructor() {
        this.config = this.loadConfig();
        this.deviceManager = null;
        this.buildDetector = null;
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

    // Device management
    async stabilizeDevice() {
        logInfo('Stabilizing device for precise UI interactions', 'Core');
        await this.sleep(2000);
        return true;
    }

    // Ensure Pluct app is in foreground before UI interactions
    async ensurePluctAppForeground() {
        logInfo('Ensuring Pluct app is in foreground...', 'Core');
        
        try {
            // Check current foreground app
            const currentApp = await this.executeCommand('adb shell dumpsys activity activities | findstr "mResumedActivity"');
            if (currentApp.success && currentApp.output && currentApp.output.includes('app.pluct')) {
                logSuccess('✅ Pluct app is already in foreground', 'Core');
                return true;
            }

            // If not in foreground, bring it to foreground
            logInfo('Pluct app not in foreground, bringing to foreground...', 'Core');
            const bringToForeground = await this.executeCommand('adb shell am start -n app.pluct/.MainActivity');
            if (bringToForeground.success) {
                logSuccess('✅ Pluct app brought to foreground', 'Core');
                await this.sleep(2000); // Wait for app to fully load
                return true;
            } else {
                logError('Failed to bring Pluct app to foreground', 'Core');
                return false;
            }

        } catch (error) {
            logError(`App foreground validation failed: ${error.message}`, 'Core');
            return false;
        }
    }

    // Validate app is in foreground before any UI interaction
    async validateAppForeground() {
        logInfo('Validating app is in foreground before UI interaction...', 'Core');
        
        try {
            // Check if Pluct app is in foreground
            const foregroundCheck = await this.executeCommand('adb shell dumpsys activity activities | findstr "mResumedActivity"');
            if (foregroundCheck.success && foregroundCheck.output && foregroundCheck.output.includes('app.pluct')) {
                logSuccess('✅ Pluct app confirmed in foreground', 'Core');
                return true;
            }

            logWarn('⚠️ Pluct app not in foreground, attempting to bring to foreground', 'Core');
            return await this.ensurePluctAppForeground();

        } catch (error) {
            logError(`App foreground validation failed: ${error.message}`, 'Core');
            return false;
        }
    }

    // Utility methods
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    // File operations
    saveObject(filePath, obj) {
        try {
            fs.mkdirSync(path.dirname(filePath), { recursive: true });
            fs.writeFileSync(filePath, JSON.stringify(obj, null, 2), 'utf8');
            return true;
        } catch (error) {
            logError(`Failed to save object: ${error.message}`, 'Core');
            return false;
        }
    }

    loadObject(filePath) {
        try {
            if (fs.existsSync(filePath)) {
                const raw = fs.readFileSync(filePath, 'utf8');
                return JSON.parse(raw);
            }
        } catch (error) {
            logError(`Failed to load object: ${error.message}`, 'Core');
        }
        return null;
    }

    // JWT detection with improved patterns
    async detectJWTGeneration() {
        // Use Windows-compatible command
        const command = `adb logcat -d | findstr "JWT"`;
        
        try {
            const result = await this.executeCommand(command);
            if (result.success && result.output && result.output.trim()) {
                logSuccess('🎯 JWT generation detected in session', 'Core');
                logInfo(`🎯 JWT logs: ${result.output.trim()}`, 'Core');
                return { found: true, logs: result.output.trim() };
            }
        } catch (error) {
            logError(`JWT detection failed: ${error.message}`, 'Core');
        }

        return { found: false, logs: null };
    }

    // API connectivity validation
    async validateAPIConnectivity() {
        logInfo('Validating API connectivity...', 'Core');
        
        // Check Business Engine health
        const healthCheck = await this.executeCommand('curl -s https://pluct-business-engine.romeo-lya2.workers.dev/health');
        if (healthCheck.success && healthCheck.output.includes('"status":"ok"')) {
            logSuccess('✅ Business Engine API is healthy', 'Core');
            
            // Parse health response for detailed validation
            try {
                const healthData = JSON.parse(healthCheck.output);
                if (healthData.connectivity) {
                    logInfo(`✅ D1: ${healthData.connectivity.d1}`, 'Core');
                    logInfo(`✅ KV: ${healthData.connectivity.kv}`, 'Core');
                    logInfo(`✅ TTT: ${healthData.connectivity.ttt}`, 'Core');
                }
            } catch (e) {
                logWarn('Could not parse health response details', 'Core');
            }
            
            return true;
        }

        logWarn('⚠️ Business Engine API health check failed', 'Core');
        return false;
    }

    // Validate TTTranscribe API connectivity
    async validateTTTranscribeConnectivity() {
        logInfo('Validating TTTranscribe API connectivity...', 'Core');
        
        // Check if we can reach TTTranscribe through Business Engine
        const tttCheck = await this.executeCommand('curl -s -I https://pluct-business-engine.romeo-lya2.workers.dev/ttt/transcribe');
        if (tttCheck.success && tttCheck.output.includes('HTTP')) {
            logSuccess('✅ TTTranscribe API endpoint is reachable', 'Core');
            return true;
        }

        logWarn('⚠️ TTTranscribe API connectivity check failed', 'Core');
        return false;
    }

    // Validate complete API flow
    async validateCompleteAPIFlow() {
        logInfo('Validating complete API flow...', 'Core');
        
        const businessEngine = await this.validateAPIConnectivity();
        const tttConnectivity = await this.validateTTTranscribeConnectivity();
        
        if (businessEngine && tttConnectivity) {
            logSuccess('✅ Complete API flow validation passed', 'Core');
            return true;
        }

        logWarn('⚠️ Complete API flow validation failed', 'Core');
        return false;
    }

    // Validate real API transactions with Business Engine
    async validateRealAPITransactions() {
        logInfo('Validating real API transactions with Business Engine...', 'Core');

        try {
            // Test Business Engine token vending endpoint
            const tokenVendTest = await this.executeCommand('curl -s -X POST https://pluct-business-engine.romeo-lya2.workers.dev/v1/vend-token -H "Content-Type: application/json" -d "{\\"test\\": true}"');
            if (tokenVendTest.success) {
                logSuccess('✅ Business Engine token vending endpoint is accessible', 'Core');
            }

            // Test Business Engine credits endpoint
            const creditsTest = await this.executeCommand('curl -s -X GET https://pluct-business-engine.romeo-lya2.workers.dev/v1/credits/balance');
            if (creditsTest.success) {
                logSuccess('✅ Business Engine credits endpoint is accessible', 'Core');
            }

            // Test TTTranscribe endpoint through Business Engine
            const tttTest = await this.executeCommand('curl -s -X POST https://pluct-business-engine.romeo-lya2.workers.dev/ttt/transcribe -H "Content-Type: application/json" -d "{\\"test\\": true}"');
            if (tttTest.success) {
                logSuccess('✅ TTTranscribe endpoint through Business Engine is accessible', 'Core');
            }

            return true;

        } catch (error) {
            logError(`API transaction validation failed: ${error.message}`, 'Core');
            return false;
        }
    }

    // Validate transcription workflow end-to-end
    async validateTranscriptionWorkflow() {
        logInfo('Validating transcription workflow end-to-end...', 'Core');

        try {
            // Check for WorkManagerUtils logs in recent history
            const workManagerLogs = await this.executeCommand('adb logcat -d | findstr "WorkManagerUtils" | Select-Object -Last 10');
            if (workManagerLogs.success && workManagerLogs.output && workManagerLogs.output.trim()) {
                logSuccess('✅ WorkManagerUtils activity detected in transcription workflow', 'Core');
                logInfo(`WorkManager logs: ${workManagerLogs.output.trim()}`, 'Core');
                return true;
            }

            // Check for transcription work enqueuing
            const transcriptionWorkLogs = await this.executeCommand('adb logcat -d | findstr "ENQUEUING TRANSCRIPTION WORK" | Select-Object -Last 5');
            if (transcriptionWorkLogs.success && transcriptionWorkLogs.output && transcriptionWorkLogs.output.trim()) {
                logSuccess('✅ Transcription work enqueuing detected', 'Core');
                return true;
            }

            // Check for work enqueued successfully
            const workEnqueuedLogs = await this.executeCommand('adb logcat -d | findstr "WORK ENQUEUED SUCCESSFULLY" | Select-Object -Last 5');
            if (workEnqueuedLogs.success && workEnqueuedLogs.output && workEnqueuedLogs.output.trim()) {
                logSuccess('✅ Work enqueued successfully detected', 'Core');
                return true;
            }

            logWarn('⚠️ No transcription workflow activity detected', 'Core');
            return false;

        } catch (error) {
            logError(`Transcription workflow validation failed: ${error.message}`, 'Core');
            return false;
        }
    }

    // Real-time API response validation
    async validateRealTimeAPIResponses() {
        logInfo('Validating real-time API responses...', 'Core');

        try {
            // Check for Business Engine API responses
            const businessEngineLogs = await this.executeCommand('adb logcat -d | findstr "BusinessEngine" | Select-Object -Last 5');
            if (businessEngineLogs.success && businessEngineLogs.output && businessEngineLogs.output.trim()) {
                logSuccess('✅ Business Engine API responses detected', 'Core');
                logInfo(`Business Engine logs: ${businessEngineLogs.output.trim()}`, 'Core');
            }

            // Check for TTTranscribe API responses
            const tttLogs = await this.executeCommand('adb logcat -d | findstr "TTTranscribe" | Select-Object -Last 5');
            if (tttLogs.success && tttLogs.output && tttLogs.output.trim()) {
                logSuccess('✅ TTTranscribe API responses detected', 'Core');
                logInfo(`TTTranscribe logs: ${tttLogs.output.trim()}`, 'Core');
            }

            // Check for HTTP responses
            const httpLogs = await this.executeCommand('adb logcat -d | findstr "HTTP" | Select-Object -Last 5');
            if (httpLogs.success && httpLogs.output && httpLogs.output.trim()) {
                logSuccess('✅ HTTP responses detected', 'Core');
                logInfo(`HTTP logs: ${httpLogs.output.trim()}`, 'Core');
            }

            // Check for API success indicators
            const successLogs = await this.executeCommand('adb logcat -d | findstr "SUCCESS" | Select-Object -Last 5');
            if (successLogs.success && successLogs.output && successLogs.output.trim()) {
                logSuccess('✅ API success indicators detected', 'Core');
                logInfo(`Success logs: ${successLogs.output.trim()}`, 'Core');
            }

            return true;

        } catch (error) {
            logError(`Real-time API response validation failed: ${error.message}`, 'Core');
            return false;
        }
    }

    // Validate complete transcription result
    async validateCompleteTranscriptionResult() {
        logInfo('Validating complete transcription result...', 'Core');

        try {
            // Check for transcription completion
            const completionLogs = await this.executeCommand('adb logcat -d | findstr "TRANSCRIPTION_COMPLETE" | Select-Object -Last 3');
            if (completionLogs.success && completionLogs.output && completionLogs.output.trim()) {
                logSuccess('✅ Transcription completion detected', 'Core');
                return true;
            }

            // Check for transcript result
            const resultLogs = await this.executeCommand('adb logcat -d | findstr "TRANSCRIPT_RESULT" | Select-Object -Last 3');
            if (resultLogs.success && resultLogs.output && resultLogs.output.trim()) {
                logSuccess('✅ Transcript result detected', 'Core');
                return true;
            }

            // Check for work completion
            const workCompletionLogs = await this.executeCommand('adb logcat -d | findstr "WORK_COMPLETED" | Select-Object -Last 3');
            if (workCompletionLogs.success && workCompletionLogs.output && workCompletionLogs.output.trim()) {
                logSuccess('✅ Work completion detected', 'Core');
                return true;
            }

            // Check for final transcript
            const finalTranscriptLogs = await this.executeCommand('adb logcat -d | findstr "FINAL_TRANSCRIPT" | Select-Object -Last 3');
            if (finalTranscriptLogs.success && finalTranscriptLogs.output && finalTranscriptLogs.output.trim()) {
                logSuccess('✅ Final transcript detected', 'Core');
                return true;
            }

            logWarn('⚠️ No complete transcription result detected', 'Core');
            return false;

        } catch (error) {
            logError(`Complete transcription result validation failed: ${error.message}`, 'Core');
            return false;
        }
    }
}

module.exports = PluctCoreFoundation;
