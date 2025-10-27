/**
 * Pluct-Smart-Test-Runner - Intelligent test execution with failure prioritization
 * Prioritizes failed tests and provides detailed error reporting
 * Adheres to 300-line limit with smart separation of concerns
 */

const PluctTestResultsTracker = require('./Pluct-Test-Results-Tracker');

class PluctSmartTestRunner {
    constructor(core) {
        this.core = core;
        this.tracker = new PluctTestResultsTracker();
        this.executionStrategy = null;
        this.startTime = null;
        this.forceFullRun = false;
    }

    /**
     * Initialize smart test runner
     */
    async initialize(options = {}) {
        this.forceFullRun = options.forceFull || false;
        this.startTime = Date.now();
        
        this.core.logger.info('🧠 Initializing Smart Test Runner...');
        
        // Load previous results
        const previousResults = this.tracker.loadPreviousResults();
        
        if (previousResults.hasPreviousResults) {
            this.core.logger.info(`📊 Previous run found: ${previousResults.failedCount} failed, ${previousResults.passedCount} passed`);
            this.core.logger.info(`📊 Last run: ${previousResults.lastRunTime}`);
        } else {
            this.core.logger.info('📊 No previous test results found - running full test suite');
        }
        
        return previousResults;
    }

    /**
     * Determine execution strategy for given tests
     */
    determineExecutionStrategy(allTests) {
        if (this.forceFullRun) {
            this.executionStrategy = {
                strategy: 'full',
                reason: 'Force full run requested',
                testsToRun: allTests,
                failedTestsCount: 0
            };
            return this.executionStrategy;
        }
        
        this.executionStrategy = this.tracker.getExecutionStrategy(allTests);
        return this.executionStrategy;
    }

    /**
     * Execute tests with smart prioritization
     */
    async executeTests(journeyOrchestrator, allTests) {
        const strategy = this.determineExecutionStrategy(allTests);
        
        this.core.logger.info('🎯 === SMART TEST EXECUTION STRATEGY ===');
        this.core.logger.info(`🎯 Strategy: ${strategy.strategy.toUpperCase()}`);
        this.core.logger.info(`🎯 Reason: ${strategy.reason}`);
        this.core.logger.info(`🎯 Tests to run: ${strategy.testsToRun.length}`);
        
        if (strategy.strategy === 'failed-first') {
            this.core.logger.info(`🎯 Prioritizing ${strategy.failedTestsCount} failed tests from previous runs`);
            this.core.logger.info(`🎯 Failed tests: ${strategy.testsToRun.join(', ')}`);
        }
        
        const results = [];
        let hasFailures = false;
        
        try {
            for (const testName of strategy.testsToRun) {
                this.core.logger.info(`🎯 Running test: ${testName}`);
                const testStartTime = Date.now();
                
                try {
                    // Map file names to registered journey names
                    let journeyName = testName;
                    if (testName.endsWith('.js')) {
                        // Use the journey name mapping if available
                        if (journeyOrchestrator.journeyNameMapping && journeyOrchestrator.journeyNameMapping[testName]) {
                            journeyName = journeyOrchestrator.journeyNameMapping[testName];
                        } else {
                            // Fallback to extracting journey name from file name
                            const fileName = testName.replace('.js', '');
                            if (fileName.startsWith('Journey-')) {
                                journeyName = fileName.replace('Journey-', '');
                            } else if (fileName.startsWith('Pluct-')) {
                                journeyName = fileName;
                            } else {
                                journeyName = fileName;
                            }
                        }
                    }
                    
                    this.core.logger.info(`🎯 Mapped ${testName} to journey: ${journeyName}`);
                    
                    // Execute the test
                    const result = await journeyOrchestrator.runJourney(journeyName);
                    const duration = Date.now() - testStartTime;
                    
                    // Record result
                    this.tracker.recordTestResult(testName, result.success, result.error, duration);
                    
                    if (result.success) {
                        this.core.logger.info(`✅ ${testName} PASSED (${duration}ms)`);
                        results.push({ testName, success: true, duration });
                    } else {
                        this.core.logger.error(`❌ ${testName} FAILED (${duration}ms): ${result.error}`);
                        results.push({ testName, success: false, error: result.error, duration });
                        hasFailures = true;
                        
                        // Generate detailed failure report
                        await this.generateDetailedFailureReport(testName, result);
                        
                        // Terminate immediately on failure in development mode
                        this.core.logger.error('❌ TERMINATING TEST EXECUTION DUE TO FAILURE');
                        this.core.logger.error('❌ This is the expected behavior in development mode');
                        this.core.logger.error('❌ Fix the failing test before running again');
                        
                        throw new Error(`Test ${testName} failed: ${result.error}`);
                    }
                } catch (error) {
                    const duration = Date.now() - testStartTime;
                    this.tracker.recordTestResult(testName, false, error.message, duration);
                    
                    this.core.logger.error(`❌ ${testName} FAILED WITH EXCEPTION (${duration}ms): ${error.message}`);
                    results.push({ testName, success: false, error: error.message, duration });
                    hasFailures = true;
                    
                    // Generate detailed failure report
                    await this.generateDetailedFailureReport(testName, { success: false, error: error.message });
                    
                    // Terminate immediately on failure
                    this.core.logger.error('❌ TERMINATING TEST EXECUTION DUE TO EXCEPTION');
                    throw error;
                }
            }
            
            // Save results
            this.tracker.saveCurrentRunResults();
            
            return {
                success: !hasFailures,
                results,
                strategy: this.executionStrategy,
                statistics: this.tracker.getStatistics()
            };
            
        } catch (error) {
            // Save results even on failure
            this.tracker.saveCurrentRunResults();
            
            this.core.logger.error('❌ Test execution terminated due to failure');
            this.core.logger.error(`❌ Error: ${error.message}`);
            
            return {
                success: false,
                results,
                strategy: this.executionStrategy,
                statistics: this.tracker.getStatistics(),
                error: error.message
            };
        }
    }

    /**
     * Generate detailed failure report
     */
    async generateDetailedFailureReport(failedTest, result) {
        try {
            this.core.logger.info('🔍 === DETAILED FAILURE ANALYSIS ===');
            this.core.logger.info(`🔍 Failed Test: ${failedTest}`);
            this.core.logger.info(`🔍 Error: ${result.error}`);
            this.core.logger.info(`🔍 Timestamp: ${new Date().toISOString()}`);
            
            // 1. Current UI State
            this.core.logger.info('🔍 Capturing current UI state...');
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            this.core.logger.info('📱 Current UI State:');
            this.core.logger.info(uiDump.substring(0, 2000) + (uiDump.length > 2000 ? '...' : ''));
            
            // 2. Recent Logcat Errors
            this.core.logger.info('🔍 Checking recent logcat errors...');
            const logcatResult = await this.core.executeCommand('adb logcat -d | findstr -i "pluct error exception crash fatal"');
            if (logcatResult.success && logcatResult.output.trim()) {
                this.core.logger.info('📱 Recent Logcat Errors:');
                const lines = logcatResult.output.split('\n').slice(-20).join('\n');
                this.core.logger.info(lines);
            } else {
                this.core.logger.info('📱 No recent logcat errors found');
            }
            
            // 3. App Status
            this.core.logger.info('🔍 Checking app status...');
            const appStatusResult = await this.core.executeCommand('adb shell dumpsys activity activities | findstr -i pluct');
            if (appStatusResult.success && appStatusResult.output.trim()) {
                this.core.logger.info('📱 App Status:');
                this.core.logger.info(appStatusResult.output);
            }
            
            // 4. Device Information
            this.core.logger.info('🔍 Checking device information...');
            const deviceInfoResult = await this.core.executeCommand('adb shell getprop ro.build.version.release');
            if (deviceInfoResult.success) {
                this.core.logger.info(`📱 Android Version: ${deviceInfoResult.output.trim()}`);
            }
            
            // 5. Network Status
            this.core.logger.info('🔍 Checking network status...');
            const networkResult = await this.core.executeCommand('adb shell dumpsys connectivity | findstr -i "active network"');
            if (networkResult.success && networkResult.output.trim()) {
                this.core.logger.info('📱 Network Status:');
                this.core.logger.info(networkResult.output);
            }
            
            // 6. Memory Status
            this.core.logger.info('🔍 Checking memory status...');
            const memoryResult = await this.core.executeCommand('adb shell dumpsys meminfo app.pluct');
            if (memoryResult.success && memoryResult.output.trim()) {
                this.core.logger.info('📱 Memory Status:');
                this.core.logger.info(memoryResult.output.substring(0, 500) + '...');
            }
            
            // 7. Test Statistics
            const stats = this.tracker.getStatistics();
            this.core.logger.info('📊 Test Statistics:');
            this.core.logger.info(`📊 Current Run: ${stats.current.passed} passed, ${stats.current.failed} failed`);
            this.core.logger.info(`📊 Previous Run: ${stats.previous.failedCount} failed, ${stats.previous.passedCount} passed`);
            
            this.core.logger.error('❌ === FAILURE ANALYSIS COMPLETE ===');
            this.core.logger.error(`❌ Failed Test: ${failedTest}`);
            this.core.logger.error(`❌ Error: ${result.error}`);
            this.core.logger.error(`❌ Time: ${new Date().toISOString()}`);
            
        } catch (error) {
            this.core.logger.error('❌ Failed to generate detailed failure report:', error.message);
        }
    }

    /**
     * Get execution summary
     */
    getExecutionSummary() {
        const duration = Date.now() - this.startTime;
        const stats = this.tracker.getStatistics();
        
        return {
            duration,
            strategy: this.executionStrategy,
            statistics: stats,
            runId: this.tracker.currentRunId
        };
    }

    /**
     * Clear test history
     */
    clearHistory() {
        return this.tracker.clearHistory();
    }

    /**
     * Get failed tests from previous runs
     */
    getFailedTestsFromPreviousRuns() {
        return this.tracker.getFailedTestsFromPreviousRuns();
    }
}

module.exports = PluctSmartTestRunner;
