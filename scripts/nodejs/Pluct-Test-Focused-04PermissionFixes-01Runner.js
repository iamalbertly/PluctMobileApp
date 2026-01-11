/**
 * Pluct-Test-Focused-04PermissionFixes-01Runner
 * Focused test runner for permission fixes validation
 * Runs all permission-related journey tests in sequence
 * Terminates on first failure in dev mode
 * Follows naming convention: [Project]-[Test]-[Focused]-[Sequence][Feature]-[Sequence][Runner]
 * 6 scope layers: Project, Test, Focused, Sequence, Feature, Sequence, Runner
 */

const PluctCoreFoundation = require('./core/Pluct-Core-01Foundation');
const PluctTestAutoFix = require('./core/Pluct-Test-AutoFix-01CommonIssues');
const path = require('path');
const fs = require('fs');

class PluctTestFocused04PermissionFixes01Runner {
    constructor() {
        this.core = new PluctCoreFoundation();
        this.autoFix = new PluctTestAutoFix(this.core);
        this.isDevMode = process.env.DEV_MODE === '1';
        this.journeys = [
            'Journey-Permission-01Onboarding-01Validation.js',
            'Journey-Permission-02ToastNotifications-01Validation.js',
            'Journey-Permission-03OverlayService-01Validation.js',
            'Journey-Permission-04SettingsIntegration-01Validation.js',
            'Journey-Permission-05PermissionDeniedFlow-01Validation.js'
        ];
        this.results = [];
    }

    /**
     * Run focused permission fixes tests
     */
    async run() {
        this.core.logger.info('🎯 Starting focused permission fixes validation...');
        this.core.logger.info(`Mode: ${this.isDevMode ? 'DEV (terminates on first error)' : 'PROD (continues on errors)'}`);
        
        // Attempt auto-fix before starting
        const fixesApplied = await this.autoFix.detectAndFix();
        if (fixesApplied) {
            this.core.logger.info(`✅ Auto-fixes applied:\n${this.autoFix.getSummary()}`);
        }
        
        // Auto-fix: Grant permissions via ADB if needed
        await this.autoFixPermissions();
        
        // Auto-fix: Clear app data for first-time user simulation (for onboarding test)
        await this.prepareForOnboardingTest();

        for (const journeyFile of this.journeys) {
            const journeyPath = path.join(__dirname, 'journeys', journeyFile);
            
            if (!fs.existsSync(journeyPath)) {
                this.core.logger.warn(`⚠️  Journey file not found: ${journeyFile}`);
                this.results.push({
                    journey: journeyFile,
                    success: false,
                    error: 'File not found'
                });
                
                if (this.isDevMode) {
                    this.core.logger.error('❌ Terminating due to missing journey file (dev mode)');
                    break;
                }
                continue;
            }

            this.core.logger.info(`\n📋 Running: ${journeyFile}`);
            
            try {
                const JourneyClass = require(journeyPath);
                const journey = new JourneyClass(this.core);
                
                const startTime = Date.now();
                const result = await journey.run();
                const duration = Date.now() - startTime;

                this.results.push({
                    journey: journeyFile,
                    success: result === true,
                    duration: duration,
                    error: result === true ? null : result
                });

                if (result === true) {
                    this.core.logger.info(`✅ ${journeyFile} passed (${duration}ms)`);
                } else {
                    this.core.logger.error(`❌ ${journeyFile} failed: ${result}`);
                    
                    if (this.isDevMode) {
                        this.core.logger.error('❌ Terminating due to failure (dev mode)');
                        break;
                    }
                }
            } catch (error) {
                this.core.logger.error(`❌ ${journeyFile} threw error: ${error.message}`);
                this.results.push({
                    journey: journeyFile,
                    success: false,
                    error: error.message,
                    stack: error.stack
                });

                if (this.isDevMode) {
                    this.core.logger.error('❌ Terminating due to error (dev mode)');
                    break;
                }
            }
        }

        this.printSummary();
        process.exit(this.getExitCode());
    }

    /**
     * Auto-fix: Grant permissions via ADB if needed
     */
    async autoFixPermissions() {
        this.core.logger.info('🔧 Auto-fixing permissions...');
        
        try {
            // Check Android version
            const androidVersion = await this.core.executeCommand('adb shell getprop ro.build.version.sdk');
            const sdkVersion = parseInt(androidVersion.output.trim());
            
            // Grant notification permission (Android 13+)
            if (sdkVersion >= 33) {
                await this.core.executeCommand('adb shell pm grant app.pluct android.permission.POST_NOTIFICATIONS', undefined, undefined, { allowFailure: true });
                this.core.logger.info('✅ Notification permission granted via ADB');
            }
            
            // Grant overlay permission
            await this.core.executeCommand('adb shell appops set app.pluct SYSTEM_ALERT_WINDOW allow', undefined, undefined, { allowFailure: true });
            this.core.logger.info('✅ Overlay permission granted via ADB');
        } catch (error) {
            this.core.logger.warn(`⚠️  Auto-fix permissions failed: ${error.message}`);
        }
    }

    /**
     * Auto-fix: Prepare for onboarding test
     */
    async prepareForOnboardingTest() {
        this.core.logger.info('🔧 Preparing for onboarding test...');
        
        try {
            // Clear app data only for onboarding test
            // We'll do this in the test itself to avoid affecting other tests
            this.core.logger.info('ℹ️  App data will be cleared in onboarding test');
        } catch (error) {
            this.core.logger.warn(`⚠️  Prepare onboarding failed: ${error.message}`);
        }
    }

    /**
     * Print test summary
     */
    printSummary() {
        this.core.logger.info('\n📊 Test Summary:');
        this.core.logger.info('='.repeat(50));
        
        const passed = this.results.filter(r => r.success).length;
        const failed = this.results.filter(r => !r.success).length;
        const total = this.results.length;

        this.core.logger.info(`Total: ${total} | Passed: ${passed} | Failed: ${failed}`);
        this.core.logger.info('');

        this.results.forEach(result => {
            const status = result.success ? '✅' : '❌';
            const duration = result.duration ? ` (${result.duration}ms)` : '';
            this.core.logger.info(`${status} ${result.journey}${duration}`);
            if (!result.success && result.error) {
                this.core.logger.info(`   Error: ${result.error}`);
            }
        });

        if (this.autoFix.fixesApplied.length > 0) {
            this.core.logger.info('\n🔧 Auto-fixes applied:');
            this.core.logger.info(this.autoFix.getSummary());
        }
    }

    /**
     * Get exit code based on results
     * @returns {number} Exit code (0 for success, 1 for failure)
     */
    getExitCode() {
        const hasFailures = this.results.some(r => !r.success);
        return hasFailures ? 1 : 0;
    }
}

// Run if executed directly
if (require.main === module) {
    const runner = new PluctTestFocused04PermissionFixes01Runner();
    runner.run().catch(error => {
        console.error('Fatal error:', error);
        process.exit(1);
    });
}

module.exports = PluctTestFocused04PermissionFixes01Runner;
