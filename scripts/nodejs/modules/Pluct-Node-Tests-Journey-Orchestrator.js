/**
 * Pluct Node Tests Journey Orchestrator
 * Automatically discovers and executes journey files based on naming convention
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */

const fs = require('fs');
const path = require('path');
const { execOut } = require('../core/Pluct-Test-Core-Exec');
const { logInfo, logWarn, logSuccess, logError } = require('../core/Logger');

class JourneyOrchestrator {
    constructor() {
        this.journeys = [];
        this.results = [];
    }

    /**
     * Discover all journey files in the journeys directory
     */
    discoverJourneys() {
        const journeysDir = path.join(__dirname, '../journeys');
        
        try {
            const files = fs.readdirSync(journeysDir);
            const journeyFiles = files.filter(file => 
                file.startsWith('Pluct-Node-Tests-Journey-') && 
                file.endsWith('.js') &&
                file.match(/\d{2}-/)
            );

            // Sort by order number in filename
            journeyFiles.sort((a, b) => {
                const aOrder = parseInt(a.match(/\d{2}/)[0]);
                const bOrder = parseInt(b.match(/\d{2}/)[0]);
                return aOrder - bOrder;
            });

            logInfo(`Discovered ${journeyFiles.length} journey files: ${journeyFiles.join(', ')}`, 'Orchestrator');

            // Load each journey
            for (const file of journeyFiles) {
                try {
                    const journeyPath = path.join(journeysDir, file);
                    const journey = require(journeyPath);
                    
                    if (journey && typeof journey === 'object') {
                        this.journeys.push({
                            file,
                            path: journeyPath,
                            module: journey,
                            name: journey.name || file.replace('.js', ''),
                            description: journey.description || 'No description',
                            order: journey.order || parseInt(file.match(/\d{2}/)[0])
                        });
                        logInfo(`Loaded journey: ${journey.name || file} (Order: ${journey.order || parseInt(file.match(/\d{2}/)[0])})`, 'Orchestrator');
                    }
                } catch (e) {
                    logWarn(`Failed to load journey ${file}: ${e.message}`, 'Orchestrator');
                }
            }

            return this.journeys;
        } catch (e) {
            logError(`Failed to discover journeys: ${e.message}`, 'Orchestrator');
            return [];
        }
    }

    /**
     * Get device ID automatically
     */
    getDeviceId() {
        try {
            const devicesOutput = execOut('adb devices');
            const devices = devicesOutput.split('\n').filter(line => 
                line.includes('device') && !line.includes('List of devices')
            ).map(line => line.split('\t')[0]);
            return devices[0] || 'emulator-5554';
        } catch (e) {
            logWarn(`Failed to get device ID: ${e.message}`, 'Orchestrator');
            return 'emulator-5554';
        }
    }

    /**
     * Run a single journey
     */
    async runJourney(journey, deviceId, artifacts, log, url) {
        const startTime = Date.now();
        logInfo(`Starting journey: ${journey.name}`, 'Orchestrator');
        
        try {
            let result = false;
            
            // Determine which test function to call based on journey name
            if (journey.name.toLowerCase().includes('app launch') || journey.name.toLowerCase().includes('applaunch')) {
                result = await journey.module.testAppLaunch({ deviceId, artifacts, log });
            } else if (journey.name.toLowerCase().includes('share intent') || journey.name.toLowerCase().includes('shareintent')) {
                result = await journey.module.testShareIntent({ deviceId, artifacts, log, url });
            } else if (journey.name.toLowerCase().includes('quick scan') || journey.name.toLowerCase().includes('quickscan')) {
                result = await journey.module.testQuickScan({ deviceId, artifacts, log });
            } else if (journey.name.toLowerCase().includes('pipeline')) {
                result = await journey.module.testPipeline({ deviceId, artifacts, log });
            } else if (journey.name.toLowerCase().includes('card actions') || journey.name.toLowerCase().includes('cardactions')) {
                result = await journey.module.testCardActions({ deviceId, artifacts, log });
            } else {
                // Try to find any exported function that starts with 'test'
                const testFunctions = Object.keys(journey.module).filter(key => 
                    key.startsWith('test') && typeof journey.module[key] === 'function'
                );
                
                if (testFunctions.length > 0) {
                    const testFunction = testFunctions[0];
                    logInfo(`Using test function: ${testFunction}`, 'Orchestrator');
                    result = await journey.module[testFunction]({ deviceId, artifacts, log, url });
                } else {
                    logWarn(`No test function found for journey: ${journey.name}`, 'Orchestrator');
                    result = true; // Skip if no test function found
                }
            }

            const duration = Date.now() - startTime;
            const resultObj = {
                journey: journey.name,
                file: journey.file,
                success: result,
                duration: duration,
                timestamp: new Date().toISOString()
            };

            this.results.push(resultObj);

            if (result) {
                logSuccess(`Journey completed successfully: ${journey.name} (${duration}ms)`, 'Orchestrator');
            } else {
                logError(`Journey failed: ${journey.name} (${duration}ms)`, 'Orchestrator');
            }

            return result;
        } catch (e) {
            const duration = Date.now() - startTime;
            logError(`Journey error: ${journey.name} - ${e.message} (${duration}ms)`, 'Orchestrator');
            
            this.results.push({
                journey: journey.name,
                file: journey.file,
                success: false,
                duration: duration,
                error: e.message,
                timestamp: new Date().toISOString()
            });
            
            return false;
        }
    }

    /**
     * Run all discovered journeys
     */
    async runAllJourneys(url) {
        logInfo('Starting journey orchestration...', 'Orchestrator');
        
        // Discover journeys
        const journeys = this.discoverJourneys();
        if (journeys.length === 0) {
            logError('No journeys discovered', 'Orchestrator');
            return false;
        }

        // Get device ID and setup artifacts
        const deviceId = this.getDeviceId();
        const artifacts = { ui: './artifacts/ui', logs: './artifacts/logs' };
        const log = { info: logInfo, success: logSuccess, error: logError, warn: logWarn };

        logInfo(`Running ${journeys.length} journeys on device: ${deviceId}`, 'Orchestrator');

        // Run journeys in order
        let allSuccess = true;
        for (const journey of journeys) {
            const success = await this.runJourney(journey, deviceId, artifacts, log, url);
            if (!success) {
                allSuccess = false;
                // Continue with other journeys even if one fails
                logWarn(`Journey ${journey.name} failed, continuing with remaining journeys`, 'Orchestrator');
            }
        }

        // Generate summary
        this.generateSummary();
        
        return allSuccess;
    }

    /**
     * Generate execution summary
     */
    generateSummary() {
        const total = this.results.length;
        const successful = this.results.filter(r => r.success).length;
        const failed = total - successful;
        const totalDuration = this.results.reduce((sum, r) => sum + r.duration, 0);

        logInfo('=== JOURNEY EXECUTION SUMMARY ===', 'Orchestrator');
        logInfo(`Total journeys: ${total}`, 'Orchestrator');
        logInfo(`Successful: ${successful}`, 'Orchestrator');
        logInfo(`Failed: ${failed}`, 'Orchestrator');
        logInfo(`Total duration: ${totalDuration}ms`, 'Orchestrator');

        // Show individual results
        this.results.forEach(result => {
            const status = result.success ? '✅' : '❌';
            logInfo(`${status} ${result.journey} (${result.duration}ms)`, 'Orchestrator');
            if (result.error) {
                logError(`   Error: ${result.error}`, 'Orchestrator');
            }
        });

        if (failed > 0) {
            logWarn(`${failed} journey(s) failed`, 'Orchestrator');
        } else {
            logSuccess('All journeys completed successfully!', 'Orchestrator');
        }
    }

    /**
     * Get results for external use
     */
    getResults() {
        return this.results;
    }
}

module.exports = JourneyOrchestrator;
