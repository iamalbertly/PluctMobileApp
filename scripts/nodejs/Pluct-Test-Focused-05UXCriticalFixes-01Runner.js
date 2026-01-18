/**
 * Pluct-Test-Focused-05UXCriticalFixes-01Runner
 * Focused test runner for UX Critical Fixes validation
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Sequence][Responsibility]
 * Terminates on first error during development phase
 */

const PluctCoreFoundation = require('./core/Pluct-Core-01Foundation');
const PluctJourneyOrchestrator = require('./journeys/Pluct-Journey-01Orchestrator');

// Import specific test journeys
const JourneyUX20NotificationConsolidationValidation = require('./journeys/Journey-UX-20NotificationConsolidation-Validation');
const JourneyUX21RedundantBadgeRemovalValidation = require('./journeys/Journey-UX-21RedundantBadgeRemoval-Validation');
const JourneyUX22VideoTitleFallbackValidation = require('./journeys/Journey-UX-22VideoTitleFallback-Validation');
const JourneyUX23ErrorLogConsolidationValidation = require('./journeys/Journey-UX-23ErrorLogConsolidation-Validation');
const JourneyUX24BatteryOptimizationRefreshValidation = require('./journeys/Journey-UX-24BatteryOptimizationRefresh-Validation');

class PluctTestFocused05UXCriticalFixes01Runner {
    constructor() {
        this.core = new PluctCoreFoundation();
        this.tests = [
            { name: 'UX-20NotificationConsolidation', journey: JourneyUX20NotificationConsolidationValidation },
            { name: 'UX-21RedundantBadgeRemoval', journey: JourneyUX21RedundantBadgeRemovalValidation },
            { name: 'UX-22VideoTitleFallback', journey: JourneyUX22VideoTitleFallbackValidation },
            { name: 'UX-23ErrorLogConsolidation', journey: JourneyUX23ErrorLogConsolidationValidation },
            { name: 'UX-24BatteryOptimizationRefresh', journey: JourneyUX24BatteryOptimizationRefreshValidation }
        ];
        this.results = [];
        this.failFast = true; // Terminate on first error during development
    }

    async run() {
        this.core.logger.info('🚀 Starting Focused UX Critical Fixes Test Runner');
        this.core.logger.info(`📋 Running ${this.tests.length} focused tests`);
        
        // Check ADB connection
        const adbCheck = await this.core.executeCommand('adb devices');
        if (!adbCheck.success || !adbCheck.output.includes('device')) {
            this.core.logger.error('❌ No ADB device connected');
            return { success: false, error: 'No ADB device connected' };
        }
        
        this.core.logger.info('✅ ADB device connected');
        
        // Check if app is installed, install if needed
        const appCheck = await this.core.executeCommand('adb shell pm list packages | findstr pluct');
        if (!appCheck.output || !appCheck.output.includes('app.pluct')) {
            this.core.logger.info('📱 App not installed, checking for APK...');
            const fs = require('fs');
            const path = require('path');
            const apkPath = path.join(__dirname, '../../app/build/outputs/apk/debug/app-debug.apk');
            if (fs.existsSync(apkPath)) {
                this.core.logger.info('📦 Installing APK...');
                const installResult = await this.core.executeCommand(`adb install -r "${apkPath}"`);
                if (!installResult.success) {
                    this.core.logger.error('❌ Failed to install APK');
                    return { success: false, error: 'Failed to install APK' };
                }
                this.core.logger.info('✅ APK installed');
            } else {
                this.core.logger.warn('⚠️ APK not found, tests may fail if app not installed');
            }
        }
        
        // Run each test
        for (const test of this.tests) {
            this.core.logger.info(`\n📝 Running: ${test.name}`);
            
            try {
                const journey = new test.journey(this.core);
                const result = await journey.execute();
                
                this.results.push({
                    name: test.name,
                    success: result.success,
                    error: result.error,
                    details: result.details
                });
                
                if (result.success) {
                    this.core.logger.info(`✅ ${test.name} passed`);
                } else {
                    this.core.logger.error(`❌ ${test.name} failed: ${result.error}`);
                    
                    // Fail fast: terminate on first error during development
                    if (this.failFast) {
                        this.core.logger.error('🛑 Terminating test run due to failure (fail-fast mode)');
                        return {
                            success: false,
                            error: `${test.name} failed: ${result.error}`,
                            results: this.results
                        };
                    }
                }
            } catch (error) {
                this.core.logger.error(`❌ ${test.name} threw exception: ${error.message}`);
                this.results.push({
                    name: test.name,
                    success: false,
                    error: error.message
                });
                
                if (this.failFast) {
                    return {
                        success: false,
                        error: `${test.name} threw exception: ${error.message}`,
                        results: this.results
                    };
                }
            }
        }
        
        // Summary
        const passed = this.results.filter(r => r.success).length;
        const failed = this.results.filter(r => !r.success).length;
        
        this.core.logger.info(`\n📊 Test Summary:`);
        this.core.logger.info(`   ✅ Passed: ${passed}/${this.tests.length}`);
        this.core.logger.info(`   ❌ Failed: ${failed}/${this.tests.length}`);
        
        return {
            success: failed === 0,
            passed,
            failed,
            results: this.results
        };
    }
}

// Run if executed directly
if (require.main === module) {
    const runner = new PluctTestFocused05UXCriticalFixes01Runner();
    runner.run().then(result => {
        process.exit(result.success ? 0 : 1);
    }).catch(error => {
        console.error('Fatal error:', error);
        process.exit(1);
    });
}

module.exports = PluctTestFocused05UXCriticalFixes01Runner;
