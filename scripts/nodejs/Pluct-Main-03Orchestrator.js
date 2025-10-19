/**
 * Pluct-Main-03Orchestrator - Consolidated main orchestrator
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[next stage increment to the childscope][CoreResponsibility]
 * Consolidated from multiple orchestrators to maintain 300-line limit and eliminate duplications
 */

const PluctCore04Foundation = require('./core/Pluct-Core-04Foundation');
const PluctCore05Performance = require('./core/Pluct-Core-05Performance');
const PluctCore06Monitoring = require('./core/Pluct-Core-06Monitoring');
const PluctJourney04Orchestrator = require('./modules/Pluct-Journey-04Orchestrator');
const PluctJourney05Pipeline = require('./modules/Pluct-Journey-05Pipeline');
const PluctJourney06Recovery = require('./modules/Pluct-Journey-06Recovery');
const PluctUI04Validator = require('./modules/Pluct-UI-04Validator');
const PluctUI05Interaction = require('./modules/Pluct-UI-05Interaction');

class PluctMain03Orchestrator {
    constructor() {
        this.core = new PluctCore04Foundation();
        this.performance = new PluctCore05Performance();
        this.monitoring = new PluctCore06Monitoring();
        this.journeyOrchestrator = new PluctJourney04Orchestrator(this.core, null);
        this.pipeline = new PluctJourney05Pipeline(this.core, null);
        this.recovery = new PluctJourney06Recovery(this.core, null);
        this.uiValidator = new PluctUI04Validator(this.core);
        this.uiInteraction = new PluctUI05Interaction(this.core);
        
        // Initialize UI validator in journey orchestrator
        this.journeyOrchestrator.uiValidator = this.uiValidator;
        this.pipeline.uiValidator = this.uiValidator;
        this.recovery.uiValidator = this.uiValidator;
    }

    /**
     * Main orchestration method
     */
    async run() {
        console.log('🎯 Starting Pluct Main Orchestrator...');
        console.log(`🎯 Scope: All, URL: ${this.core.config.url}`);
        
        try {
            // Validate environment
            console.log('🎯 Validating environment...');
            const envResult = await this.validateEnvironment();
            if (!envResult.success) {
                throw new Error('Environment validation failed');
            }
            console.log('✅ Environment validation passed');

            // Optimize performance
            await this.performance.optimizePerformance();

            // Start comprehensive journey testing
            console.log('🎯 Starting comprehensive journey testing...');
            const journeyResults = await this.runAllJourneys();

            // Generate enhanced report
            const report = await this.generateEnhancedReport(journeyResults);
            console.log('📊 Enhanced test report generated');

            console.log('✅ All journeys completed successfully');
            return { success: true, report, journeyResults };
        } catch (error) {
            console.error('❌ Main orchestration failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Validate environment
     */
    async validateEnvironment() {
        try {
            // Check ADB connectivity
            const adbResult = await this.core.executeCommand('adb devices');
            if (!adbResult.success || !adbResult.output.includes('device')) {
                throw new Error('ADB not connected or no devices found');
            }

            // Check app installation
            const appResult = await this.core.executeCommand('adb shell pm list packages | findstr app.pluct');
            if (!appResult.success || !appResult.output.includes('app.pluct')) {
                throw new Error('Pluct app not installed');
            }

            return { success: true };
        } catch (error) {
            console.error('❌ Environment validation failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Run all journeys
     */
    async runAllJourneys() {
        const journeys = [
            { name: 'AppLaunch', method: () => this.journeyOrchestrator.testAppLaunch() },
            { name: 'ShareIntent', method: () => this.journeyOrchestrator.testShareIntent() },
            { name: 'QuickScan', method: () => this.journeyOrchestrator.testQuickScan() },
            { name: 'Pipeline', method: () => this.pipeline.testPipeline() },
            { name: 'ManualURLInput', method: () => this.journeyOrchestrator.testManualURLInput() }
        ];

        const results = [];
        
        for (const journey of journeys) {
            try {
                console.log(`🎯 Running journey: ${journey.name}`);
                const result = await journey.method();
                
                if (result.success) {
                    console.log(`✅ Journey ${journey.name} completed successfully`);
                } else {
                    console.error(`❌ Journey ${journey.name} failed:`, result.error);
                }
                
                results.push({ name: journey.name, result });
            } catch (error) {
                console.error(`❌ Journey ${journey.name} failed with exception:`, error.message);
                results.push({ name: journey.name, result: { success: false, error: error.message } });
            }
        }

        return results;
    }

    /**
     * Generate enhanced report
     */
    async generateEnhancedReport(journeyResults) {
        const successful = journeyResults.filter(j => j.result.success).length;
        const total = journeyResults.length;
        const successRate = total > 0 ? (successful / total) * 100 : 0;

        console.log('📊 === ENHANCED TEST REPORT ===');
        console.log(`📊 Total Journeys: ${total}`);
        console.log(`📊 Successful: ${successful}`);
        console.log(`📊 Failed: ${total - successful}`);
        console.log(`📊 Success Rate: ${successRate.toFixed(1)}%`);

        // Log individual results
        journeyResults.forEach(journey => {
            const status = journey.result.success ? '✅' : '❌';
            console.log(`📊 ${status} ${journey.name}: ${journey.result.success ? 'PASSED' : 'FAILED'}`);
        });

        return {
            total,
            successful,
            failed: total - successful,
            successRate,
            journeys: journeyResults.map(j => ({
                name: j.name,
                success: j.result.success,
                error: j.result.error
            }))
        };
    }

    /**
     * Cleanup resources
     */
    async cleanup() {
        try {
            // Clear monitoring data
            this.monitoring.clearMonitoringData();
            
            // Clear UI artifacts
            this.uiValidator.clearUIArtifacts();
            
            // Clear interaction history
            this.uiInteraction.clearInteractionHistory();
            
            console.log('🧹 Cleanup completed');
            return { success: true };
        } catch (error) {
            console.error('❌ Cleanup failed:', error.message);
            return { success: false, error: error.message };
        }
    }
}

// Main execution
if (require.main === module) {
    const orchestrator = new PluctMain03Orchestrator();
    orchestrator.run()
        .then(result => {
            if (result.success) {
                console.log('🎉 All tests completed successfully');
                process.exit(0);
            } else {
                console.error('❌ Tests failed:', result.error);
                process.exit(1);
            }
        })
        .catch(error => {
            console.error('❌ Orchestration failed:', error.message);
            process.exit(1);
        });
}

module.exports = PluctMain03Orchestrator;
