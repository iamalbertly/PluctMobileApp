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
        this.latestChangedTests = [
            'Journey-UX-25DirectToValue-Readiness-01Validation',
            'Journey-UX-25DirectToValue-Readiness-01Validation.js',
            'Journey-Intent-03TikTok-04BalanceRace-01Validation',
            'Journey-Intent-03TikTok-04BalanceRace-01Validation.js',
            'Journey-Fix-03HealthMonitoring-Validation',
            'Journey-Fix-03HealthMonitoring-Validation.js',
            'Journey-User-Identification-01Validation',
            'Journey-User-Identification-01Validation.js',
            'Journey-APIConnectivity',
            'Journey-APIConnectivity.js',
            'Journey-TTTranscribeIntegration',
            'Journey-TTTranscribeIntegration.js',
            'Journey-UX-20NotificationConsolidation-Validation',
            'Journey-UX-20NotificationConsolidation-Validation.js',
            'Journey-Permission-04SettingsIntegration-01Validation',
            'Journey-Permission-04SettingsIntegration-01Validation.js',
            'Journey-UX-24BatteryOptimizationRefresh-Validation',
            'Journey-UX-24BatteryOptimizationRefresh-Validation.js',
            'Journey-UX-17BackgroundProcessing-01Validation',
            'Journey-UX-17BackgroundProcessing-01Validation.js',
            'Journey-UX-13NotificationNavigation-Validation',
            'Journey-UX-13NotificationNavigation-Validation.js',
            'Journey-UX-12BackgroundProcessing-Validation',
            'Journey-UX-12BackgroundProcessing-Validation.js',
            'Journey-UX-11AutoSubmitIntent-Validation',
            'Journey-UX-11AutoSubmitIntent-Validation.js',
            'Pluct-Test-Validation-10ErrorHandling',
            'Pluct-Test-Validation-10ErrorHandling.js',
            'Journey-UX-22VideoTitleFallback-Validation',
            'Journey-UX-22VideoTitleFallback-Validation.js',
            'Journey-UX-05RedundantVisuals-Validation',
            'Journey-UX-05RedundantVisuals-Validation.js',
            'Journey-EdgeCase-04MultipleNotifications-Validation',
            'Journey-EdgeCase-04MultipleNotifications-Validation.js',
            'Journey-EdgeCase-03NetworkLoss-Validation',
            'Journey-EdgeCase-03NetworkLoss-Validation.js',
            'Journey-QuickScan',
            'Journey-QuickScan.js'
        ];
        this.highPriorityTests = [
            'Journey-TTTranscribeIntegration',
            'Journey-TTTranscribeIntegration.js',
            'Journey-APIConnectivity',
            'Journey-APIConnectivity.js',
            'Journey-TokenVendingValidation.js',
            'TokenVendingValidation',
            'Pluct-Test-Validation-04APIConnectivity',
            'Pluct-Test-Validation-04APIConnectivity.js',
            'Pluct-Test-Validation-06TokenVending',
            'Pluct-Test-Validation-06TokenVending.js'
        ];
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
        const orderedTests = this.orderTestsForCustomerRisk(allTests);
        if (this.forceFullRun) {
            this.executionStrategy = {
                strategy: 'full',
                reason: 'Force full run requested',
                testsToRun: orderedTests,
                failedTestsCount: 0
            };
            return this.executionStrategy;
        }
        
        this.executionStrategy = this.tracker.getExecutionStrategy(orderedTests);
        return this.executionStrategy;
    }

    orderTestsForCustomerRisk(allTests) {
        const uniqueTests = Array.from(new Set(allTests));
        const failed = new Set(this.tracker.getFailedTestsFromPreviousRuns());
        const take = (predicate) => uniqueTests.filter(predicate);
        const latest = take(test => this.latestChangedTests.includes(test));
        const failedTests = take(test => failed.has(test) && !latest.includes(test));
        const priority = take(test =>
            this.highPriorityTests.includes(test) &&
            !latest.includes(test) &&
            !failedTests.includes(test)
        );
        const remaining = uniqueTests.filter(test =>
            !latest.includes(test) &&
            !failedTests.includes(test) &&
            !priority.includes(test)
        );
        const ordered = [...latest, ...failedTests, ...priority, ...remaining];
        this.core.logger.info(`🎯 Customer-risk order: ${ordered.join(' -> ')}`);
        return ordered;
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
        
        if (strategy.strategy === 'resume-from-failed') {
            this.core.logger.info(`🎯 Resuming from first failed test: ${strategy.testsToRun[0]}`);
            this.core.logger.info(`🎯 Found ${strategy.failedTestsCount} failed tests from previous runs`);
            this.core.logger.info(`🎯 Running ${strategy.testsToRun.length} tests starting from index ${strategy.resumeFromIndex || 0}`);
        } else if (strategy.strategy === 'failed-first') {
            this.core.logger.info(`🎯 Prioritizing ${strategy.failedTestsCount} failed tests from previous runs`);
            this.core.logger.info(`🎯 Failed tests: ${strategy.testsToRun.join(', ')}`);
        }
        
        const results = [];
        let hasFailures = false;
        
        try {
            for (const testName of strategy.testsToRun) {
                this.core.logger.info(`🎯 Running test: ${testName}`);
                const testStartTime = Date.now();
                
                // Reset logcat noise and capture pre-flight telemetry for realtime validation
                await this.core.clearLogcat();
                await this.captureStageTelemetry(`${testName}-pre`);

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
                        await this.captureStageTelemetry(`${testName}-post-success`);
                    } else {
                        this.core.logger.error(`❌ ${testName} FAILED (${duration}ms): ${result.error}`);
                        results.push({ testName, success: false, error: result.error, duration });
                        await this.captureStageTelemetry(`${testName}-post-fail`);
                        hasFailures = true;
                        
                        // Generate detailed failure report
                        if (this.core.logger.isVerbose && this.core.logger.isVerbose()) {
                            await this.generateDetailedFailureReport(testName, result);
                        }
                        
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
                    await this.captureStageTelemetry(`${testName}-post-exception`);
                    hasFailures = true;
                    
                    // Generate detailed failure report
                    if (this.core.logger.isVerbose && this.core.logger.isVerbose()) {
                        await this.generateDetailedFailureReport(testName, { success: false, error: error.message });
                    }
                    
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
        if (!this.core.logger.isVerbose || !this.core.logger.isVerbose()) {
            return;
        }

        try {
            this.core.logger.info('🔍 === DETAILED FAILURE ANALYSIS ===');
            this.core.logger.info(`🔍 Failed Test: ${failedTest}`);
            this.core.logger.info(`🔍 Error: ${result.error}`);
            this.core.logger.info(`🔍 Timestamp: ${new Date().toISOString()}`);
            
            // 1. Current UI State
            this.core.logger.info('🔍 Capturing current UI state...');
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
            
            // 2. Recent Logcat Errors
            this.core.logger.info('🔍 Checking recent logcat errors...');
            const logcatResult = await this.core.executeCommand('adb logcat -d | findstr -i "pluct error exception crash fatal"', undefined, undefined, { allowFailure: true });
            if (logcatResult.success && logcatResult.output.trim()) {
                this.core.logger.info('📱 Recent Logcat Errors:');
                const lines = logcatResult.output.split('\n').slice(-20).join('\n');
                this.core.logger.info(lines);
            } else {
                this.core.logger.info('📱 No recent logcat errors found');
                if (!logcatResult.success) {
                    this.core.logger.error(`   ⚠️ Logcat command failed: ${logcatResult.error}`);
                    if (logcatResult.stderr) this.core.logger.error(`   ADB stderr: ${logcatResult.stderr}`);
                }
            }
            
            // 3. App Status
            this.core.logger.info('🔍 Checking app status...');
            const appStatusResult = await this.core.executeCommand('adb shell dumpsys activity activities | findstr -i pluct', undefined, undefined, { allowFailure: true });
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
            
            // 4. Device Information
            this.core.logger.info('🔍 Checking device information...');
            const deviceInfoResult = await this.core.executeCommand('adb shell getprop ro.build.version.release', undefined, undefined, { allowFailure: true });
            if (deviceInfoResult.success) {
                this.core.logger.info(`📱 Android Version: ${deviceInfoResult.output.trim()}`);
            } else {
                this.core.logger.error(`   ⚠️ Device info check failed: ${deviceInfoResult.error}`);
                if (deviceInfoResult.stderr) this.core.logger.error(`   ADB stderr: ${deviceInfoResult.stderr}`);
            }
            
            // 5. Network Status
            this.core.logger.info('🔍 Checking network status...');
            const networkResult = await this.core.executeCommand('adb shell dumpsys connectivity | findstr -i "active network"', undefined, undefined, { allowFailure: true });
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
            this.core.logger.info('🔍 Checking memory status...');
            const memoryResult = await this.core.executeCommand('adb shell dumpsys meminfo app.pluct', undefined, undefined, { allowFailure: true });
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
     * Capture logcat + UI snapshots around a stage to validate realtime behavior.
     */
    async captureStageTelemetry(label) {
        try {
            this.core.logger.info(`dY"? Telemetry checkpoint: ${label}`);
            
            // Capture UI snapshot for runtime state validation
            const uiResult = await this.core.dumpUIHierarchy();
            if (uiResult.success) {
                const uiDump = (this.core.readLastUIDump() || "").trim();
                if (uiDump) {
                    const preview = uiDump.substring(0, 800);
                    this.core.logger.info(`dY"ñ UI snapshot (${label}): ${preview}${uiDump.length > 800 ? "..." : ""}`);
                }
            }
            
            // Capture recent logcat lines for auth/submit/poll visibility
            const logcatResult = await this.core.executeCommand("adb logcat -d -t 200", undefined, undefined, { allowFailure: true });
            if (logcatResult.success && logcatResult.output) {
                const tail = logcatResult.output.split('\n').slice(-120).join('\n');
                this.core.logger.info(`dY"ñ Logcat tail (${label}):`);
                this.core.logger.info(tail);
            }
        } catch (error) {
            this.core.logger.warn(`dY"¡ Telemetry capture skipped: ${error.message}`);
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
