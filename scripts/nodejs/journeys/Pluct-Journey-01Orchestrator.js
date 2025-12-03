/**
 * Pluct-Journey-01Orchestrator - Journey orchestration functionality
 * Single source of truth for journey management
 * Adheres to 300-line limit with smart separation of concerns
 */

const PluctCoreFoundation = require('../core/Pluct-Core-01Foundation');
const fs = require('fs');
const path = require('path');

class PluctJourneyOrchestrator {
    constructor() {
        this.core = new PluctCoreFoundation();
        this.journeys = new Map();
        this.results = [];
        this.journeyExecutionOrder = [
            'Journey-AppLaunch.js',
            'Journey-ErrorNotificationValidation.js',
            'Pluct-UI-Header-01CreditBalance-Validation.js',
            'Pluct-UI-Header-02SettingsNavigation-Validation.js',
            'Pluct-UI-Header-03EndToEndIntegration-Validation.js',
            'Journey-QuickScan.js', // Updated to work with new Extract Script button
            'Journey-TikTok-Intent-01Transcription.js',
            'Journey-TikTok-Intent-Route-01Transcription.js',
            'Journey-TikTok-Manual-URL-01Transcription.js',
            'Journey-Transcript-Storage-01Display.js',
            'Pluct-Transcript-01BackgroundWorker-Validation.js',
            'Pluct-Transcript-02PerformanceTiming-Validation.js',
            'Pluct-Transcript-03EndToEndWorkflow-Validation.js',
            'Journey-FreeTier-E2E-01Validation.js', // New Smart Free Tier test
            'Journey-PremiumTier-E2E-01Validation.js', // New Smart Premium Tier test
            'Pluct-Journey-BusinessEngine-Credits-01Validation.js'
            // Add other journeys here in the desired order
        ];
        
        // Map file names to registered journey names
        this.journeyNameMapping = {
            'Journey-AppLaunch.js': 'AppLaunch',
            'Journey-ErrorNotificationValidation.js': 'ErrorNotificationValidation',
            'Journey-QuickScan.js': 'QuickScan', // Updated to work with new Start Transcription button
            'Journey-TikTok-Intent-01Transcription.js': 'TikTokIntentTranscription',
            'Journey-TikTok-Intent-Route-01Transcription.js': 'TikTok-Intent-Route-01Transcription',
            'Journey-TikTok-Manual-URL-01Transcription.js': 'TikTokManualURLTranscription',
            'Journey-Transcript-Storage-01Display.js': 'TranscriptStorageDisplay',
            'Pluct-Journey-01AppLaunch.js': 'Pluct-Journey-01AppLaunch',
            'Pluct-Node-Tests-Journey-01-AppLaunch.js': 'Pluct-Node-Tests-Journey-01-AppLaunch',
            'Pluct-UI-Header-01CreditBalance-Validation.js': 'Pluct-UI-Header-01CreditBalance-Validation',
            'Pluct-UI-Header-02SettingsNavigation-Validation.js': 'Pluct-UI-Header-02SettingsNavigation-Validation',
            'Pluct-UI-Header-03EndToEndIntegration-Validation.js': 'Pluct-UI-Header-03EndToEndIntegration-Validation',
            'Pluct-Transcript-01BackgroundWorker-Validation.js': 'Pluct-Transcript-01BackgroundWorker-Validation',
            'Pluct-Transcript-02PerformanceTiming-Validation.js': 'Pluct-Transcript-02PerformanceTiming-Validation',
            'Pluct-Transcript-03EndToEndWorkflow-Validation.js': 'Pluct-Transcript-03EndToEndWorkflow-Validation',
            'Journey-FreeTier-E2E-01Validation.js': 'FreeTier-E2E-01Validation',
            'Journey-PremiumTier-E2E-01Validation.js': 'PremiumTier-E2E-01Validation',
            'Pluct-Journey-BusinessEngine-Credits-01Validation.js': 'Pluct-Journey-BusinessEngine-Credits-01Validation'
        };
    }

    /**
     * Register a journey
     */
    registerJourney(name, journey) {
        this.journeys.set(name, journey);
        this.core.logger.info(`📝 Journey registered: ${name}`);
    }

    /**
     * Run a single journey
     */
    async runJourney(name) {
        const journey = this.journeys.get(name);
        if (!journey) {
            throw new Error(`Journey '${name}' not found`);
        }

        this.core.logger.info(`🎯 Running journey: ${name}`);
        
        try {
            const result = await (journey.run ? journey.run() : journey.execute());
            this.results.push({ name, result, success: result.success });
            
            if (result.success) {
                this.core.logger.info(`✅ Journey ${name} completed successfully`);
            } else {
                this.core.logger.error(`❌ Journey ${name} failed:`, result.error);
            }
            
            return result;
        } catch (error) {
            this.core.logger.error(`❌ Journey ${name} failed with exception:`, error.message);
            this.core.logger.error(`❌ TERMINATING TEST EXECUTION DUE TO JOURNEY EXCEPTION`);
            throw error; // Re-throw to terminate execution
        }
    }

    /**
     * Load and register all journeys
     */
    async loadAllJourneys() {
        this.core.logger.info('🎯 Loading all journeys...');

        const journeysDir = __dirname;

        // Load all discovered journeys first
        const discoveredJourneys = {};
        const files = fs.readdirSync(journeysDir)
            .filter(f => f.endsWith('.js') && f !== 'Pluct-Journey-01Orchestrator.js');
        
        for (const file of files) {
            try {
                const mod = require(path.join(journeysDir, file));
                discoveredJourneys[file] = mod;
            } catch (e) {
                this.core.logger.warn(`Failed to load journey ${file}: ${e.message}`);
            }
        }

        // Add remaining discovered journeys to the execution order if they are not already present
        for (const file in discoveredJourneys) {
            if (!this.journeyExecutionOrder.includes(file)) {
                this.journeyExecutionOrder.push(file);
            }
        }
        
        // Register journeys in the specified order
        for (const file of this.journeyExecutionOrder) {
            const mod = discoveredJourneys[file];
            if (!mod) {
                this.core.logger.warn(`Journey file ${file} not found in discovered journeys.`);
                continue;
            }

            let journeyName = path.basename(file, '.js');
            try {
                if (mod && typeof mod.register === 'function') {
                    mod.register(this);
                } else if (mod && typeof mod === 'function') {
                    const journey = new mod(this.core);
                    journeyName = journey.name || journeyName;
                    this.registerJourney(journeyName, journey);
                }
            } catch (e) {
                this.core.logger.warn(`Failed to register journey ${file}: ${e.message}`);
            }
        }

        this.core.logger.info(`🎯 Loaded ${this.journeys.size} journeys`);
        return this.journeys;
    }

    /**
     * Run all registered journeys
     */
    async runAllJourneys() {
        this.core.logger.info('🎯 Starting comprehensive journey testing...');

        // Load all journeys first
        await this.loadAllJourneys();

        for (const [name, journey] of this.journeys) {
            this.core.logger.info(`🎯 Running journey: ${name}`);
            const result = await this.runJourney(name);
            
            // Enhanced error handling with detailed diagnostics
            if (!result.success) {
                this.core.logger.error(`❌ JOURNEY FAILED: ${name}`);
                this.core.logger.error(`❌ Error: ${result.error}`);
                this.core.logger.error(`❌ Stopping test execution due to failure`);
                
                // Comprehensive debugging information
                await this.generateDetailedFailureReport(name, result);
                
                // Terminate immediately with detailed error
                const errorMessage = `Journey ${name} failed: ${result.error}`;
                this.core.logger.error(`❌ TERMINATING TEST EXECUTION: ${errorMessage}`);
                throw new Error(errorMessage);
            }
        }

        return this.results;
    }

    /**
     * Generate detailed failure report
     */
    async generateDetailedFailureReport(failedJourney, result) {
        try {
            this.core.logger.info('🔍 Generating detailed failure report...');
            
            // 1. Current UI State
            const uiDumpResult = await this.core.dumpUIHierarchy();
            if (uiDumpResult.success) {
                const uiDump = this.core.readLastUIDump();
                this.core.logger.info('📱 Current UI State:');
                this.core.logger.info(uiDump.substring(0, 2000) + (uiDump.length > 2000 ? '...' : ''));
            } else {
                this.core.logger.error(`   ⚠️ UI dump failed: ${uiDumpResult.error}`);
                if (uiDumpResult.stderr) {
                    this.core.logger.error(`   ADB stderr: ${uiDumpResult.stderr}`);
                }
                if (uiDumpResult.adbConnectionIssue) {
                    this.core.logger.error(`   🔴 ADB Connection Issue Detected!`);
                }
                // Try to read last dump if available
                try {
                    const lastDump = this.core.readLastUIDump();
                    if (lastDump) {
                        this.core.logger.info('📱 Using last known UI State:');
                        this.core.logger.info(lastDump.substring(0, 2000) + (lastDump.length > 2000 ? '...' : ''));
                    }
                } catch (e) {
                    this.core.logger.warn('   No previous UI dump available');
                }
            }
            
            // 2. Recent Logcat
            const logcatResult = await this.core.executeCommand('adb logcat -d | findstr -i "pluct error exception crash fatal"');
            if (logcatResult.success && logcatResult.output.trim()) {
                this.core.logger.info('📱 Recent Logcat Errors:');
                const lines = logcatResult.output.split('\n').slice(-20).join('\n');
                this.core.logger.info(lines);
            } else {
                if (!logcatResult.success) {
                    this.core.logger.error(`   ⚠️ Logcat command failed: ${logcatResult.error}`);
                    if (logcatResult.stderr) this.core.logger.error(`   ADB stderr: ${logcatResult.stderr}`);
                } else {
                    this.core.logger.info('📱 No recent logcat errors found');
                }
            }
            
            // 3. App Status
            const appStatusResult = await this.core.executeCommand('adb shell dumpsys activity activities | findstr -i pluct');
            if (appStatusResult.success && appStatusResult.output.trim()) {
                this.core.logger.info('📱 App Status:');
                this.core.logger.info(appStatusResult.output);
            } else {
                if (!appStatusResult.success) {
                    this.core.logger.error(`   ⚠️ App status check failed: ${appStatusResult.error}`);
                    if (appStatusResult.stderr) this.core.logger.error(`   ADB stderr: ${appStatusResult.stderr}`);
                    if (appStatusResult.adbConnectionIssue) {
                        this.core.logger.error(`   🔴 ADB Connection Issue Detected!`);
                    }
                } else {
                    this.core.logger.info('📱 App not found in activity stack');
                }
            }
            
            // 4. Device Info
            const deviceInfoResult = await this.core.executeCommand('adb shell getprop ro.build.version.release');
            if (deviceInfoResult.success) {
                this.core.logger.info(`📱 Android Version: ${deviceInfoResult.output.trim()}`);
            } else {
                this.core.logger.error(`   ⚠️ Device info check failed: ${deviceInfoResult.error}`);
                if (deviceInfoResult.stderr) this.core.logger.error(`   ADB stderr: ${deviceInfoResult.stderr}`);
            }
            
            // 5. Network Status
            const networkResult = await this.core.executeCommand('adb shell dumpsys connectivity | findstr -i "active network"');
            if (networkResult.success && networkResult.output.trim()) {
                this.core.logger.info('📱 Network Status:');
                this.core.logger.info(networkResult.output);
            } else {
                if (!networkResult.success) {
                    this.core.logger.error(`   ⚠️ Network status check failed: ${networkResult.error}`);
                    if (networkResult.stderr) this.core.logger.error(`   ADB stderr: ${networkResult.stderr}`);
                } else {
                    this.core.logger.info('📱 Network status unavailable');
                }
            }
            
            // 6. Memory Status
            const memoryResult = await this.core.executeCommand('adb shell dumpsys meminfo app.pluct');
            if (memoryResult.success && memoryResult.output.trim()) {
                this.core.logger.info('📱 Memory Status:');
                this.core.logger.info(memoryResult.output.substring(0, 500) + '...');
            } else {
                if (!memoryResult.success) {
                    this.core.logger.error(`   ⚠️ Memory status check failed: ${memoryResult.error}`);
                    if (memoryResult.stderr) this.core.logger.error(`   ADB stderr: ${memoryResult.stderr}`);
                } else {
                    this.core.logger.info('📱 Memory status unavailable');
                }
            }
            
            this.core.logger.error(`❌ FAILURE ANALYSIS COMPLETE FOR: ${failedJourney}`);
            this.core.logger.error(`❌ FAILURE REASON: ${result.error}`);
            this.core.logger.error(`❌ FAILURE TIME: ${new Date().toISOString()}`);
            
        } catch (error) {
            this.core.logger.error('❌ Failed to generate detailed failure report:', error.message);
        }
    }

    /**
     * Generate journey report
     */
    generateReport() {
        const successful = this.results.filter(r => r.success).length;
        const total = this.results.length;
        const successRate = total > 0 ? (successful / total) * 100 : 0;

        this.core.logger.info('📊 === JOURNEY TEST REPORT ===');
        this.core.logger.info(`📊 Total Journeys: ${total}`);
        this.core.logger.info(`📊 Successful: ${successful}`);
        this.core.logger.info(`📊 Failed: ${total - successful}`);
        this.core.logger.info(`📊 Success Rate: ${successRate.toFixed(1)}%`);

        // Log individual results
        this.results.forEach(journey => {
            const status = journey.success ? '✅' : '❌';
            this.core.logger.info(`📊 ${status} ${journey.name}: ${journey.success ? 'PASSED' : 'FAILED'}`);
        });

        return {
            total,
            successful,
            failed: total - successful,
            successRate,
            journeys: this.results.map(j => ({
                name: j.name,
                success: j.success,
                error: j.result.error
            }))
        };
    }
}

/**
 * Base Journey Class
 */
class BaseJourney {
    constructor(core) {
        this.core = core;
        this.logger = core.logger; // Add logger reference for convenience
    }

    async execute() {
        throw new Error('Journey execute method must be implemented');
    }

    async ensureAppForeground() {
        return await this.core.ensureAppForeground();
    }

    async captureUI(tag) {
        return await this.core.captureUIArtifacts(tag);
    }

    async dumpUI() {
        return await this.core.dumpUIHierarchy();
    }

    /**
     * Fail with logcat output, API logs, and UI dump for debugging
     */
    async failWithDiagnostics(errorMessage) {
        this.core.logger.error(`❌ ${errorMessage}`);
        
        // Capture API logs first (most important for debugging API issues)
        try {
            this.core.logger.error('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            this.core.logger.error('🔍 CAPTURING API LOGS FOR DIAGNOSTICS');
            this.core.logger.error('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            const apiLogs = await this.core.captureAPILogs(500);
            if (apiLogs.success) {
                this.core.displayAPILogs(apiLogs);
            } else {
                this.core.logger.error(`Failed to capture API logs: ${apiLogs.error}`);
            }
        } catch (e) {
            this.core.logger.error(`Exception capturing API logs: ${e.message}`);
        }
        
        // Capture general logcat for debugging
        try {
            const logcatResult = await this.core.executeCommand('adb logcat -d -t 100');
            this.core.logger.error('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            this.core.logger.error('📱 RECENT LOGCAT OUTPUT:');
            this.core.logger.error('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            this.core.logger.error(logcatResult.output || 'No logcat output available');
        } catch (e) {
            this.core.logger.error(`Failed to capture logcat: ${e.message}`);
        }
        
        // Dump UI for debugging
        try {
            await this.dumpUI();
            const uiDump = this.core.readLastUIDump();
            this.core.logger.error('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            this.core.logger.error('📱 CURRENT UI STATE:');
            this.core.logger.error('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            this.core.logger.error(uiDump.substring(0, 2000));
        } catch (e) {
            this.core.logger.error(`Failed to capture UI dump: ${e.message}`);
        }
        
        throw new Error(errorMessage);
    }
}

module.exports = { PluctJourneyOrchestrator, BaseJourney };
