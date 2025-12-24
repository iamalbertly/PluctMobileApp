/**
 * Pluct-Main-01Orchestrator - Main orchestration functionality
 * Single source of truth for test orchestration
 * Adheres to 300-line limit with smart separation of concerns
 */

const PluctCoreFoundation = require('./core/Pluct-Core-01Foundation');
const { PluctJourneyOrchestrator } = require('./journeys/Pluct-Journey-01Orchestrator');
const PluctSmartTestRunner = require('./core/Pluct-Smart-Test-Runner');

class PluctMainOrchestrator {
    constructor() {
        this.core = new PluctCoreFoundation();
        this.journeyOrchestrator = new PluctJourneyOrchestrator();
        this.smartTestRunner = new PluctSmartTestRunner(this.core);
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
    async run(options = {}) {
        this.core.logger.info('🎯 Starting Pluct Main Orchestrator...');
        this.core.logger.info(`🎯 Scope: All, URL: ${this.core.config.url}`);
        if (options && Array.isArray(options.tests) && options.tests.length > 0) {
            this.core.logger.info(`🎯 Test filter active: ${options.tests.join(', ')}`);
        }
        
        try {
            // Initialize smart test runner
            await this.smartTestRunner.initialize(options);
            
            // Validate environment
            this.core.logger.info('🎯 Validating environment...');
            const envResult = await this.core.validateEnvironment();
            if (!envResult.success) {
                throw new Error('Environment validation failed');
            }
            this.core.logger.info('✅ Environment validation passed');

            // Load all journeys first
            await this.journeyOrchestrator.loadAllJourneys();

            // Build test list with filter support
            const allJourneys = this.journeyOrchestrator.journeyExecutionOrder;
            const requested = Array.isArray(options.tests) && options.tests.length > 0
                ? options.tests
                : [];

            const testsToRun = requested.length > 0 ? requested : allJourneys;

            // Decide strategy early to preserve cached tokens when resuming
            const preStrategy = this.smartTestRunner.determineExecutionStrategy(testsToRun);
            if (preStrategy.strategy === 'resume-from-failed') {
                this.core.config.skipAppDataClear = true;
                this.core.logger.info('dYZ_ Preserving app data to reuse cached tokens while resuming');
            }

            // Run performance optimizations
            await this.optimizePerformance();
            
            // Execute tests with smart prioritization
            const testResults = await this.smartTestRunner.executeTests(this.journeyOrchestrator, testsToRun);
            
            if (!testResults.success) {
                this.core.logger.error('❌ Test execution failed');
                this.core.logger.error(`❌ Error: ${testResults.error}`);
                return { success: false, error: testResults.error, testResults };
            }

            // Generate enhanced report
            const report = this.generateEnhancedReport(testResults);
            this.core.logger.info('📊 Enhanced test report generated');

            this.core.logger.info('✅ All journeys completed successfully');
            return { success: true, report, testResults };
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
     * Generate enhanced report with smart test results
     */
    generateEnhancedReport(testResults) {
        const summary = this.smartTestRunner.getExecutionSummary();
        const successful = testResults.results.filter(r => r.success).length;
        const total = testResults.results.length;
        const successRate = total > 0 ? (successful / total) * 100 : 0;

        this.core.logger.info('📊 === ENHANCED TEST REPORT ===');
        this.core.logger.info(`📊 Execution Strategy: ${summary.strategy.strategy.toUpperCase()}`);
        this.core.logger.info(`📊 Strategy Reason: ${summary.strategy.reason}`);
        this.core.logger.info(`📊 Total Tests: ${total}`);
        this.core.logger.info(`📊 Successful: ${successful}`);
        this.core.logger.info(`📊 Failed: ${total - successful}`);
        this.core.logger.info(`📊 Success Rate: ${successRate.toFixed(1)}%`);
        this.core.logger.info(`📊 Duration: ${summary.duration}ms`);
        this.core.logger.info(`📊 Run ID: ${summary.runId}`);

        // Log individual results
        testResults.results.forEach(result => {
            const status = result.success ? '✅' : '❌';
            this.core.logger.info(`📊 ${status} ${result.testName}: ${result.success ? 'PASSED' : 'FAILED'} (${result.duration}ms)`);
            if (!result.success && result.error) {
                this.core.logger.info(`📊   Error: ${result.error}`);
            }
        });

        return {
            execution: summary,
            results: testResults.results,
            statistics: {
                total,
                successful,
                failed: total - successful,
                successRate,
                duration: summary.duration
            },
            strategy: summary.strategy
        };
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
    
    // Parse command line arguments
    const args = process.argv.slice(2);
    const options = {
        forceFull: args.includes('--force-full') || args.includes('-f'),
        tests: []
    };
    
    // Support --test=Name and --test Name (multiple allowed)
    args.forEach((arg, idx) => {
        if (arg.startsWith('--test=')) {
            const name = arg.split('=')[1];
            if (name) options.tests.push(name.endsWith('.js') ? name : `${name}.js`);
        }
        if (arg === '--test' && args[idx + 1] && !args[idx + 1].startsWith('--')) {
            const name = args[idx + 1];
            options.tests.push(name.endsWith('.js') ? name : `${name}.js`);
        }
    });

    // Also support NPM env passthrough: npm_config_test, TEST_FILTER, TESTS
    const envFilter = process.env.npm_config_test || process.env.TEST_FILTER || process.env.TESTS || '';
    if (envFilter) {
        envFilter.split(',').map(s => s.trim()).filter(Boolean).forEach(name => {
            options.tests.push(name.endsWith('.js') ? name : `${name}.js`);
        });
    }

    if (options.forceFull) {
        console.log('🔄 Force full test run requested - ignoring previous results');
    }
    if (options.tests.length > 0) {
        console.log(`🎯 Filtered test run: ${options.tests.join(', ')}`);
    }
    
    orchestrator.run(options)
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
