/**
 * Pluct-Journey-01Orchestrator - Journey orchestration functionality
 * Single source of truth for journey management
 * Adheres to 300-line limit with smart separation of concerns
 */

const PluctCoreUnified = require('../core/Pluct-Core-Unified-New');
const fs = require('fs');
const path = require('path');

class PluctJourneyOrchestrator {
    constructor() {
        this.core = new PluctCoreUnified();
        this.journeys = new Map();
        this.results = [];
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
            const result = { success: false, error: error.message };
            this.results.push({ name, result, success: false });
            return result;
        }
    }

    /**
     * Run all registered journeys
     */
    async runAllJourneys() {
        this.core.logger.info('🎯 Starting comprehensive journey testing...');

        // Auto-discover journey files named Journey-*.js in this directory
        try {
            const journeysDir = __dirname;
            const files = fs.readdirSync(journeysDir)
                .filter(f => f.startsWith('Journey-') && f.endsWith('.js') && f !== 'Pluct-Journey-01Orchestrator.js');
            
            for (const file of files) {
                try {
                    const mod = require(path.join(journeysDir, file));
                    if (mod && typeof mod.register === 'function') {
                        mod.register(this);
                    } else if (mod && mod.name) {
                        // Direct journey class
                        const journey = new mod(this.core);
                        this.journeys.set(journey.name, journey);
                    } else if (mod && typeof mod === 'function') {
                        // Constructor function
                        const journey = new mod(this.core);
                        this.journeys.set(journey.name, journey);
                    }
                } catch (e) {
                    this.core.logger.warn(`Failed to load journey ${file}: ${e.message}`);
                }
            }
        } catch (e) {
            this.core.logger.warn(`Journey auto-discovery failed: ${e.message}`);
        }

        for (const [name, journey] of this.journeys) {
            const result = await this.runJourney(name);
            
            // Stop on first failure in debug mode
            if (!result.success) {
                this.core.logger.error(`❌ JOURNEY FAILED: ${name}`);
                this.core.logger.error(`❌ Error: ${result.error}`);
                this.core.logger.error(`❌ Stopping test execution due to failure`);
                
                // Dump current UI state for debugging
                await this.core.dumpUIHierarchy();
                const uiDump = this.core.readLastUIDump();
                this.core.logger.info('📱 Current UI State:');
                this.core.logger.info(uiDump.substring(0, 1000) + '...');
                
            // Get recent logcat for debugging
            const logcatResult = await this.core.executeCommand('adb logcat -d | findstr -i "pluct error exception crash fatal"');
            if (logcatResult.success) {
                this.core.logger.info('📱 Recent Logcat:');
                const lines = logcatResult.output.split('\n').slice(-10).join('\n');
                this.core.logger.info(lines);
            }
                
                throw new Error(`Journey ${name} failed: ${result.error}`);
            }
        }

        return this.results;
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
}

module.exports = { PluctJourneyOrchestrator, BaseJourney };
