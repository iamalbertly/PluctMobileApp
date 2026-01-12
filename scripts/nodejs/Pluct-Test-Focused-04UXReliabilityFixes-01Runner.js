/**
 * Pluct-Test-Focused-04UXReliabilityFixes-01Runner
 * Focused test runner for UX Reliability Fixes validation
 * Tests: Battery Optimization, Status Verification, Background Processing, Stale Status, Re-Processing Prevention
 */

const PluctCoreFoundation = require('./core/Pluct-Core-01Foundation');
const PluctJourneyOrchestrator = require('./journeys/Pluct-Journey-01Orchestrator');
const PluctSmartTestRunner = require('./core/Pluct-Smart-Test-Runner');
const PluctTestAutoFix = require('./core/Pluct-Test-AutoFix-01CommonIssues');

async function runUXReliabilityFixesTests() {
    const core = new PluctCoreFoundation();
    const orchestrator = new PluctJourneyOrchestrator();
    const smartRunner = new PluctSmartTestRunner(core);
    const autoFix = new PluctTestAutoFix(core);
    
    core.logger.info('🎯 Starting Focused UX Reliability Fixes Test Suite');
    
    // Auto-fix common issues before starting
    const fixesApplied = await autoFix.detectAndFix();
    if (fixesApplied && autoFix.getSummary) {
        core.logger.info(`✅ Auto-fixes applied: ${autoFix.getSummary()}`);
    } else if (fixesApplied) {
        core.logger.info('✅ Auto-fixes applied');
    }
    
    // Initialize smart test runner
    await smartRunner.initialize({ forceFull: false });
    
    // Focused test list for UX Reliability Fixes
    const focusedTests = [
        'Journey-UX-15BatteryOptimization-01Validation.js',
        'Journey-UX-16StatusVerification-01Validation.js',
        'Journey-UX-17BackgroundProcessing-01Validation.js',
        'Journey-UX-18StaleStatus-01Validation.js',
        'Journey-UX-19ReProcessingPrevention-01Validation.js'
    ];
    
    // Determine execution strategy
    const strategy = smartRunner.determineExecutionStrategy(focusedTests);
    core.logger.info(`🎯 Execution Strategy: ${strategy.strategy}`);
    core.logger.info(`🎯 Tests to run: ${strategy.testsToRun.length}`);
    
    // Execute tests with auto-fix on failures (dev mode)
    const isDevMode = process.env.DEV_MODE === '1';
    const results = await smartRunner.executeTests(orchestrator, strategy.testsToRun);
    
    // Print summary
    const passed = results.filter(r => r.success).length;
    const failed = results.filter(r => !r.success).length;
    
    core.logger.info('📊 === TEST EXECUTION SUMMARY ===');
    core.logger.info(`✅ Passed: ${passed}`);
    core.logger.info(`❌ Failed: ${failed}`);
    core.logger.info(`📊 Total: ${results.length}`);
    
    if (failed > 0) {
        core.logger.error('❌ Some tests failed. Review logcat and UI dumps above.');
        
        // In dev mode, attempt auto-fix and retry failed tests once
        if (isDevMode && failed <= 2) {
            core.logger.info('🔧 Dev mode: Attempting auto-fix and retry...');
            const retryFixes = await autoFix.detectAndFix();
            if (retryFixes) {
                core.logger.info('✅ Auto-fixes applied, retrying failed tests...');
                // Retry only failed tests
                const failedTestNames = results.filter(r => !r.success).map(r => r.name);
                if (failedTestNames.length > 0) {
                    const retryStrategy = smartRunner.determineExecutionStrategy(failedTestNames);
                    const retryResults = await smartRunner.executeTests(orchestrator, retryStrategy.testsToRun);
                    const retryPassed = retryResults.filter(r => r.success).length;
                    if (retryPassed === retryResults.length) {
                        core.logger.info('✅ All tests passed after auto-fix!');
                        process.exit(0);
                    }
                }
            }
        }
        
        process.exit(1);
    } else {
        core.logger.info('✅ All UX Reliability Fixes tests passed!');
        process.exit(0);
    }
}

// Run if executed directly
if (require.main === module) {
    runUXReliabilityFixesTests().catch(error => {
        console.error('Fatal error:', error);
        process.exit(1);
    });
}

module.exports = { runUXReliabilityFixesTests };
