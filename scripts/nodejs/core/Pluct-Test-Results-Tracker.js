/**
 * Pluct-Test-Results-Tracker - Failed test tracking and prioritization
 * Tracks test results across runs and prioritizes failed tests
 * Adheres to 300-line limit with smart separation of concerns
 */

const fs = require('fs');
const path = require('path');

class PluctTestResultsTracker {
    constructor() {
        this.resultsFile = path.join(__dirname, '../../artifacts/test-results-history.json');
        this.currentRunId = this.generateRunId();
        this.currentRunResults = new Map();
        this.previousRunResults = new Map();
        this.failedTests = new Set();
        this.passedTests = new Set();
    }

    /**
     * Generate unique run ID
     */
    generateRunId() {
        return `run_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    }

    /**
     * Load previous test results
     */
    loadPreviousResults() {
        try {
            if (fs.existsSync(this.resultsFile)) {
                const data = JSON.parse(fs.readFileSync(this.resultsFile, 'utf8'));
                this.previousRunResults = new Map(data.failedTests || []);
                
                // Extract failed tests from previous runs
                for (const [testName, results] of this.previousRunResults) {
                    if (results.failed) {
                        this.failedTests.add(testName);
                    } else {
                        this.passedTests.add(testName);
                    }
                }
                
                return {
                    hasPreviousResults: true,
                    failedCount: this.failedTests.size,
                    passedCount: this.passedTests.size,
                    lastRunTime: data.lastRunTime
                };
            }
        } catch (error) {
            console.warn('⚠️ Failed to load previous test results:', error.message);
        }
        
        return {
            hasPreviousResults: false,
            failedCount: 0,
            passedCount: 0,
            lastRunTime: null
        };
    }

    /**
     * Record test result for current run
     */
    recordTestResult(testName, success, error = null, duration = 0) {
        const result = {
            testName,
            success,
            error,
            duration,
            timestamp: new Date().toISOString(),
            runId: this.currentRunId
        };
        
        this.currentRunResults.set(testName, result);
        
        if (success) {
            this.passedTests.add(testName);
            this.failedTests.delete(testName);
        } else {
            this.failedTests.add(testName);
            this.passedTests.delete(testName);
        }
        
        return result;
    }

    /**
     * Get prioritized test execution order
     */
    getPrioritizedTestOrder(allTests) {
        const prioritized = [];
        
        // 1. First priority: Tests that failed in previous runs
        const previouslyFailed = allTests.filter(test => this.failedTests.has(test));
        prioritized.push(...previouslyFailed);
        
        // 2. Second priority: New tests (not in previous results)
        const newTests = allTests.filter(test => 
            !this.previousRunResults.has(test) && 
            !previouslyFailed.includes(test)
        );
        prioritized.push(...newTests);
        
        // 3. Third priority: Tests that passed in previous runs
        const previouslyPassed = allTests.filter(test => 
            this.passedTests.has(test) && 
            !previouslyFailed.includes(test) && 
            !newTests.includes(test)
        );
        prioritized.push(...previouslyPassed);
        
        return prioritized;
    }

    /**
     * Check if we should run only failed tests
     */
    shouldRunOnlyFailedTests() {
        return this.failedTests.size > 0;
    }

    /**
     * Get failed tests from previous runs
     */
    getFailedTestsFromPreviousRuns() {
        return Array.from(this.failedTests);
    }

    /**
     * Get test execution strategy
     */
    getExecutionStrategy(allTests) {
        const previousResults = this.loadPreviousResults();
        
        if (!previousResults.hasPreviousResults) {
            return {
                strategy: 'full',
                reason: 'No previous test results found',
                testsToRun: allTests,
                failedTestsCount: 0
            };
        }
        
        if (this.failedTests.size === 0) {
            return {
                strategy: 'full',
                reason: 'No failed tests from previous runs',
                testsToRun: allTests,
                failedTestsCount: 0
            };
        }
        
        const failedTests = this.getFailedTestsFromPreviousRuns();
        const availableFailedTests = failedTests.filter(test => allTests.includes(test));
        
        if (availableFailedTests.length === 0) {
            return {
                strategy: 'full',
                reason: 'No failed tests available in current test suite',
                testsToRun: allTests,
                failedTestsCount: 0
            };
        }
        
        // Find the first failed test in the test order
        const firstFailedTestIndex = allTests.findIndex(test => availableFailedTests.includes(test));
        
        if (firstFailedTestIndex === -1) {
            // Should not happen, but fallback to full run
            return {
                strategy: 'full',
                reason: 'Failed tests not found in test order',
                testsToRun: allTests,
                failedTestsCount: 0
            };
        }
        
        // Start from first failed test and continue with all remaining tests
        const testsToRun = allTests.slice(firstFailedTestIndex);
        
        return {
            strategy: 'resume-from-failed',
            reason: `Resuming from first failed test (${allTests[firstFailedTestIndex]}) - running ${testsToRun.length} tests`,
            testsToRun: testsToRun,
            failedTestsCount: availableFailedTests.length,
            allTests: allTests,
            resumeFromIndex: firstFailedTestIndex
        };
    }

    /**
     * Save current run results
     */
    saveCurrentRunResults() {
        try {
            const resultsData = {
                lastRunTime: new Date().toISOString(),
                runId: this.currentRunId,
                failedTests: Array.from(this.currentRunResults.entries()),
                summary: {
                    total: this.currentRunResults.size,
                    passed: this.passedTests.size,
                    failed: this.failedTests.size,
                    successRate: this.currentRunResults.size > 0 ? 
                        (this.passedTests.size / this.currentRunResults.size) * 100 : 0
                }
            };
            
            // Ensure artifacts directory exists
            const artifactsDir = path.dirname(this.resultsFile);
            if (!fs.existsSync(artifactsDir)) {
                fs.mkdirSync(artifactsDir, { recursive: true });
            }
            
            fs.writeFileSync(this.resultsFile, JSON.stringify(resultsData, null, 2));
            return true;
        } catch (error) {
            console.error('❌ Failed to save test results:', error.message);
            return false;
        }
    }

    /**
     * Get detailed failure report
     */
    getDetailedFailureReport() {
        const failedTests = Array.from(this.failedTests);
        const report = {
            runId: this.currentRunId,
            timestamp: new Date().toISOString(),
            failedTests: failedTests.map(testName => {
                const result = this.currentRunResults.get(testName);
                return {
                    testName,
                    error: result?.error || 'Unknown error',
                    duration: result?.duration || 0,
                    timestamp: result?.timestamp
                };
            }),
            summary: {
                totalFailed: failedTests.length,
                totalTests: this.currentRunResults.size,
                successRate: this.currentRunResults.size > 0 ? 
                    ((this.currentRunResults.size - failedTests.length) / this.currentRunResults.size) * 100 : 0
            }
        };
        
        return report;
    }

    /**
     * Clear all test results history
     */
    clearHistory() {
        try {
            if (fs.existsSync(this.resultsFile)) {
                fs.unlinkSync(this.resultsFile);
            }
            this.currentRunResults.clear();
            this.previousRunResults.clear();
            this.failedTests.clear();
            this.passedTests.clear();
            return true;
        } catch (error) {
            console.error('❌ Failed to clear test history:', error.message);
            return false;
        }
    }

    /**
     * Get test statistics
     */
    getStatistics() {
        const previousResults = this.loadPreviousResults();
        return {
            current: {
                total: this.currentRunResults.size,
                passed: this.passedTests.size,
                failed: this.failedTests.size
            },
            previous: {
                hasResults: previousResults.hasPreviousResults,
                failedCount: previousResults.failedCount,
                passedCount: previousResults.passedCount,
                lastRunTime: previousResults.lastRunTime
            }
        };
    }
}

module.exports = PluctTestResultsTracker;
