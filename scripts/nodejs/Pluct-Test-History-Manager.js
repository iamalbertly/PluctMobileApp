/**
 * Pluct-Test-History-Manager - Test history management utilities
 * Provides utilities for managing test result history
 * Adheres to 300-line limit with smart separation of concerns
 */

const PluctTestResultsTracker = require('./core/Pluct-Test-Results-Tracker');

class PluctTestHistoryManager {
    constructor() {
        this.tracker = new PluctTestResultsTracker();
    }

    /**
     * Clear all test history
     */
    clearHistory() {
        console.log('🧹 Clearing test history...');
        const success = this.tracker.clearHistory();
        if (success) {
            console.log('✅ Test history cleared successfully');
        } else {
            console.error('❌ Failed to clear test history');
        }
        return success;
    }

    /**
     * Show test history statistics
     */
    showStatistics() {
        console.log('📊 === TEST HISTORY STATISTICS ===');
        const stats = this.tracker.getStatistics();
        
        console.log(`📊 Current Run:`);
        console.log(`📊   Total: ${stats.current.total}`);
        console.log(`📊   Passed: ${stats.current.passed}`);
        console.log(`📊   Failed: ${stats.current.failed}`);
        
        if (stats.previous.hasResults) {
            console.log(`📊 Previous Run:`);
            console.log(`📊   Failed: ${stats.previous.failedCount}`);
            console.log(`📊   Passed: ${stats.previous.passedCount}`);
            console.log(`📊   Last Run: ${stats.previous.lastRunTime}`);
        } else {
            console.log(`📊 Previous Run: No data available`);
        }
        
        return stats;
    }

    /**
     * Show failed tests from previous runs
     */
    showFailedTests() {
        console.log('❌ === FAILED TESTS FROM PREVIOUS RUNS ===');
        const failedTests = this.tracker.getFailedTestsFromPreviousRuns();
        
        if (failedTests.length === 0) {
            console.log('✅ No failed tests from previous runs');
        } else {
            console.log(`❌ Found ${failedTests.length} failed tests:`);
            failedTests.forEach((test, index) => {
                console.log(`❌ ${index + 1}. ${test}`);
            });
        }
        
        return failedTests;
    }

    /**
     * Get execution strategy preview
     */
    previewExecutionStrategy(allTests) {
        console.log('🎯 === EXECUTION STRATEGY PREVIEW ===');
        const strategy = this.tracker.getExecutionStrategy(allTests);
        
        console.log(`🎯 Strategy: ${strategy.strategy.toUpperCase()}`);
        console.log(`🎯 Reason: ${strategy.reason}`);
        console.log(`🎯 Tests to run: ${strategy.testsToRun.length}`);
        
        if (strategy.strategy === 'failed-first') {
            console.log(`🎯 Failed tests: ${strategy.testsToRun.join(', ')}`);
        }
        
        return strategy;
    }
}

// Main execution
if (require.main === module) {
    const manager = new PluctTestHistoryManager();
    const command = process.argv[2];
    
    switch (command) {
        case 'clear':
            manager.clearHistory();
            break;
        case 'stats':
            manager.showStatistics();
            break;
        case 'failed':
            manager.showFailedTests();
            break;
        case 'preview':
            const allTests = process.argv.slice(3);
            if (allTests.length === 0) {
                console.error('❌ Please provide test names for preview');
                process.exit(1);
            }
            manager.previewExecutionStrategy(allTests);
            break;
        default:
            console.log('📋 Available commands:');
            console.log('📋   clear   - Clear test history');
            console.log('📋   stats   - Show test statistics');
            console.log('📋   failed  - Show failed tests from previous runs');
            console.log('📋   preview - Preview execution strategy for given tests');
            console.log('📋 Usage: node Pluct-Test-History-Manager.js <command> [args...]');
            break;
    }
}

module.exports = PluctTestHistoryManager;
