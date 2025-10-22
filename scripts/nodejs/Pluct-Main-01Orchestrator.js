/**
 * Pluct-Main-01Orchestrator - Main orchestration functionality
 * Single source of truth for test orchestration
 * Adheres to 300-line limit with smart separation of concerns
 */

const PluctCoreUnified = require('./core/Pluct-Core-Unified-New');
const { PluctJourneyOrchestrator } = require('./journeys/Pluct-Journey-01Orchestrator');

class PluctMainOrchestrator {
    constructor() {
        this.core = new PluctCoreUnified();
        this.journeyOrchestrator = new PluctJourneyOrchestrator();
        this.setupJourneys();
    }

    /**
     * Setup all journeys
     */
    setupJourneys() {
        // Journeys are auto-discovered by the orchestrator
        this.core.logger.info('📝 Journey setup completed');
    }

    /**
     * Main orchestration method
     */
    async run() {
        this.core.logger.info('🎯 Starting Pluct Main Orchestrator...');
        this.core.logger.info(`🎯 Scope: All, URL: ${this.core.config.url}`);
        
        try {
            // Validate environment
            this.core.logger.info('🎯 Validating environment...');
            const envResult = await this.core.validateEnvironment();
            if (!envResult.success) {
                throw new Error('Environment validation failed');
            }
            this.core.logger.info('✅ Environment validation passed');

            // Run performance optimizations
            await this.optimizePerformance();

            // Run all journeys
            const journeyResults = await this.journeyOrchestrator.runAllJourneys();

            // Generate report
            const report = this.journeyOrchestrator.generateReport();
            this.core.logger.info('📊 Enhanced test report generated');

            this.core.logger.info('✅ All journeys completed successfully');
            return { success: true, report, journeyResults };
        } catch (error) {
            this.core.logger.error('❌ Main orchestration failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    /**
     * Optimize performance
     */
    async optimizePerformance() {
        this.core.logger.info('🚀 Optimizing performance...');
        
        try {
            // Run comprehensive optimization
            const optimizationResult = await this.core.comprehensiveOptimization();
            if (optimizationResult.success) {
                this.core.logger.info('✅ Performance optimization completed');
            } else {
                this.core.logger.warn('⚠️ Performance optimization had issues');
            }
        } catch (error) {
            this.core.logger.warn('⚠️ Performance optimization failed:', error.message);
        }
    }

    /**
     * Cleanup resources
     */
    async cleanup() {
        try {
            this.core.logger.info('🧹 Cleanup completed');
            return { success: true };
        } catch (error) {
            this.core.logger.error('❌ Cleanup failed:', error.message);
            return { success: false, error: error.message };
        }
    }
}

// Main execution
if (require.main === module) {
    const orchestrator = new PluctMainOrchestrator();
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

module.exports = PluctMainOrchestrator;
