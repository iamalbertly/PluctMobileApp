const fs = require('fs');
const path = require('path');

/**
 * Pluct-Journey-02AutoDiscoveryOrchestrator - Auto-discovers and runs test journeys
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation][CoreResponsibility]
 * Automatically finds and executes all journey files
 */
class PluctJourney02AutoDiscoveryOrchestrator {
    constructor(core) {
        this.core = core;
        this.journeysPath = path.join(__dirname, 'journeys');
        this.discoveredJourneys = [];
        this.results = [];
    }

    /**
     * Auto-discover all journey files
     */
    async discoverJourneys() {
        this.core.logger.info('🔍 Auto-discovering test journeys...');
        
        try {
            const files = fs.readdirSync(this.journeysPath);
            const journeyFiles = files.filter(file => 
                file.startsWith('Journey-') && 
                file.endsWith('.js') &&
                !file.includes('Orchestrator') // Exclude orchestrator files
            );
            
            this.core.logger.info(`Found ${journeyFiles.length} journey files:`);
            journeyFiles.forEach(file => {
                this.core.logger.info(`  - ${file}`);
            });
            
            // Load all journey classes
            for (const file of journeyFiles) {
                try {
                    const journeyPath = path.join(this.journeysPath, file);
                    const JourneyClass = require(journeyPath);
                    
                    if (JourneyClass && typeof JourneyClass === 'function') {
                        const journey = new JourneyClass(this.core);
                        this.discoveredJourneys.push({
                            name: journey.name || file.replace('.js', ''),
                            file: file,
                            instance: journey,
                            path: journeyPath
                        });
                        this.core.logger.info(`✅ Loaded journey: ${journey.name || file}`);
                    } else {
                        this.core.logger.warn(`⚠️ Skipped ${file}: Not a valid journey class`);
                    }
                } catch (error) {
                    this.core.logger.error(`❌ Failed to load ${file}: ${error.message}`);
                }
            }
            
            this.core.logger.info(`🎯 Successfully loaded ${this.discoveredJourneys.length} journeys`);
            return this.discoveredJourneys;
            
        } catch (error) {
            this.core.logger.error(`❌ Failed to discover journeys: ${error.message}`);
            throw error;
        }
    }

    /**
     * Run all discovered journeys
     */
    async runAllJourneys() {
        this.core.logger.info('🚀 Starting auto-discovery test execution...');
        this.core.logger.info(`Running ${this.discoveredJourneys.length} journeys`);
        
        const startTime = Date.now();
        let passed = 0;
        let failed = 0;
        
        for (let i = 0; i < this.discoveredJourneys.length; i++) {
            const journey = this.discoveredJourneys[i];
            this.core.logger.info(`\n📋 Running Journey ${i + 1}/${this.discoveredJourneys.length}: ${journey.name}`);
            
            try {
                const journeyStartTime = Date.now();
                const result = await journey.instance.execute();
                const journeyDuration = Date.now() - journeyStartTime;
                
                if (result.success) {
                    passed++;
                    this.core.logger.info(`✅ ${journey.name} - PASSED (${journeyDuration}ms)`);
                    if (result.message) {
                        this.core.logger.info(`   Message: ${result.message}`);
                    }
                } else {
                    failed++;
                    this.core.logger.error(`❌ ${journey.name} - FAILED (${journeyDuration}ms)`);
                    this.core.logger.error(`   Error: ${result.error || 'Unknown error'}`);
                    if (result.failedStep) {
                        this.core.logger.error(`   Failed Step: ${result.failedStep}`);
                    }
                }
                
                this.results.push({
                    name: journey.name,
                    file: journey.file,
                    success: result.success,
                    duration: journeyDuration,
                    result: result
                });
                
            } catch (error) {
                failed++;
                this.core.logger.error(`❌ ${journey.name} - EXCEPTION: ${error.message}`);
                this.results.push({
                    name: journey.name,
                    file: journey.file,
                    success: false,
                    duration: 0,
                    result: { success: false, error: error.message }
                });
            }
            
            // Brief pause between journeys
            if (i < this.discoveredJourneys.length - 1) {
                await this.core.sleep(2000);
            }
        }
        
        const totalDuration = Date.now() - startTime;
        this.core.logger.info(`\n📊 Test Execution Summary:`);
        this.core.logger.info(`Total Journeys: ${this.discoveredJourneys.length}`);
        this.core.logger.info(`Passed: ${passed}`);
        this.core.logger.info(`Failed: ${failed}`);
        this.core.logger.info(`Total Duration: ${totalDuration}ms`);
        
        return {
            total: this.discoveredJourneys.length,
            passed: passed,
            failed: failed,
            duration: totalDuration,
            results: this.results
        };
    }

    /**
     * Run specific journey by name
     */
    async runJourneyByName(journeyName) {
        const journey = this.discoveredJourneys.find(j => j.name === journeyName);
        if (!journey) {
            throw new Error(`Journey not found: ${journeyName}`);
        }
        
        this.core.logger.info(`🎯 Running specific journey: ${journeyName}`);
        const startTime = Date.now();
        
        try {
            const result = await journey.instance.execute();
            const duration = Date.now() - startTime;
            
            this.core.logger.info(`${result.success ? '✅' : '❌'} ${journeyName} - ${result.success ? 'PASSED' : 'FAILED'} (${duration}ms)`);
            
            return {
                name: journeyName,
                success: result.success,
                duration: duration,
                result: result
            };
            
        } catch (error) {
            this.core.logger.error(`❌ ${journeyName} - EXCEPTION: ${error.message}`);
            return {
                name: journeyName,
                success: false,
                duration: Date.now() - startTime,
                result: { success: false, error: error.message }
            };
        }
    }

    /**
     * Run journeys by pattern
     */
    async runJourneysByPattern(pattern) {
        const matchingJourneys = this.discoveredJourneys.filter(j => 
            j.name.toLowerCase().includes(pattern.toLowerCase()) ||
            j.file.toLowerCase().includes(pattern.toLowerCase())
        );
        
        if (matchingJourneys.length === 0) {
            this.core.logger.warn(`No journeys found matching pattern: ${pattern}`);
            return [];
        }
        
        this.core.logger.info(`🎯 Running ${matchingJourneys.length} journeys matching pattern: ${pattern}`);
        
        const results = [];
        for (const journey of matchingJourneys) {
            const result = await this.runJourneyByName(journey.name);
            results.push(result);
        }
        
        return results;
    }

    /**
     * Get journey statistics
     */
    getJourneyStats() {
        const stats = {
            total: this.discoveredJourneys.length,
            byType: {},
            byStatus: { passed: 0, failed: 0 }
        };
        
        this.discoveredJourneys.forEach(journey => {
            // Categorize by journey type
            const type = this.categorizeJourney(journey.name);
            stats.byType[type] = (stats.byType[type] || 0) + 1;
        });
        
        this.results.forEach(result => {
            if (result.success) {
                stats.byStatus.passed++;
            } else {
                stats.byStatus.failed++;
            }
        });
        
        return stats;
    }

    /**
     * Categorize journey by name
     */
    categorizeJourney(journeyName) {
        if (journeyName.includes('Intent')) return 'Intent Route';
        if (journeyName.includes('Manual')) return 'Manual URL';
        if (journeyName.includes('Error')) return 'Error Handling';
        if (journeyName.includes('Transcription')) return 'Transcription';
        if (journeyName.includes('API')) return 'API Integration';
        return 'Other';
    }

    /**
     * Generate detailed report
     */
    generateReport() {
        const stats = this.getJourneyStats();
        const report = {
            summary: {
                totalJourneys: this.discoveredJourneys.length,
                totalExecuted: this.results.length,
                passed: this.results.filter(r => r.success).length,
                failed: this.results.filter(r => !r.success).length,
                successRate: this.results.length > 0 ? 
                    (this.results.filter(r => r.success).length / this.results.length * 100).toFixed(2) + '%' : '0%'
            },
            byType: stats.byType,
            results: this.results,
            discoveredJourneys: this.discoveredJourneys.map(j => ({
                name: j.name,
                file: j.file
            }))
        };
        
        return report;
    }
}

module.exports = PluctJourney02AutoDiscoveryOrchestrator;
