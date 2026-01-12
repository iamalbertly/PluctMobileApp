/**
 * Pluct-Test-Focused-04UXReliabilityFixes-01Runner
 * Focused test runner for UX Reliability Fixes validation
 * Tests: Battery Optimization, Status Verification, Background Processing, Stale Status, Re-Processing Prevention
 */

const PluctCoreFoundation = require('./core/Pluct-Core-01Foundation');
const PluctJourneyOrchestrator = require('./journeys/Pluct-Journey-01Orchestrator');
const PluctSmartTestRunner = require('./core/Pluct-Smart-Test-Runner');

async function runUXReliabilityFixesTests() {
    const core = new PluctCoreFoundation();
    const orchestrator = new PluctJourneyOrchestrator();
    const smartRunner = new PluctSmartTestRunner(core);
    
    core.logger.info('🎯 Starting Focused UX Reliability Fixes Test Suite');
    
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
    
    // Execute tests
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
