/**
 * Pluct-Test-Focused-07OnboardingTutorial-01Runner
 * Focused test runner for onboarding tutorial validation
 * Validates the new onboarding tutorial flow implementation
 *
 * Usage:
 *   node scripts/nodejs/Pluct-Test-Focused-07OnboardingTutorial-01Runner.js
 *
 * Tests included:
 *   1. Journey-Onboarding-01Tutorial-01Validation - Full tutorial flow
 *   2. Journey-Onboarding-02FirstTranscript-01Validation - First transcript milestone
 *   3. Journey-Onboarding-03Skip-01Validation - Skip flow validation
 */

const PluctCoreFoundation = require('./core/Pluct-Core-01Foundation');
const path = require('path');
const fs = require('fs');

class PluctTestFocusedOnboardingTutorial {
    constructor() {
        this.core = new PluctCoreFoundation();
        this.journeys = [
            'Journey-Onboarding-01Tutorial-01Validation.js',
            'Journey-Onboarding-03Skip-01Validation.js'
            // Note: Journey-Onboarding-02FirstTranscript-01Validation requires actual API
            // and can be run separately when needed
        ];
        this.results = [];
        this.startTime = null;
    }

    async initialize() {
        this.core.logger.info('='.repeat(60));
        this.core.logger.info('Pluct Onboarding Tutorial Test Runner');
        this.core.logger.info('='.repeat(60));

        // Verify ADB connection
        const devices = await this.core.executeCommand('adb devices');
        if (!devices.output.includes('device')) {
            throw new Error('No ADB devices connected');
        }
        this.core.logger.info('ADB device connected');

        // Verify app is installed
        const packages = await this.core.executeCommand('adb shell pm list packages | findstr pluct');
        if (!packages.output.includes('app.pluct')) {
            throw new Error('app.pluct not installed on device');
        }
        this.core.logger.info('app.pluct is installed');

        return true;
    }

    async runJourney(journeyFile) {
        const journeyPath = path.join(__dirname, 'journeys', journeyFile);

        if (!fs.existsSync(journeyPath)) {
            this.core.logger.warn(`Journey file not found: ${journeyFile}`);
            return { journey: journeyFile, success: false, error: 'File not found' };
        }

        this.core.logger.info('');
        this.core.logger.info('-'.repeat(50));
        this.core.logger.info(`Running: ${journeyFile}`);
        this.core.logger.info('-'.repeat(50));

        try {
            const JourneyClass = require(journeyPath);
            const journey = new JourneyClass(this.core);
            const result = await journey.run();

            return {
                journey: journeyFile,
                success: result,
                error: result ? null : 'Journey returned false'
            };

        } catch (error) {
            this.core.logger.error(`Journey threw error: ${error.message}`);
            return {
                journey: journeyFile,
                success: false,
                error: error.message
            };
        }
    }

    async run() {
        this.startTime = Date.now();

        try {
            await this.initialize();

            this.core.logger.info('');
            this.core.logger.info(`Running ${this.journeys.length} onboarding tests...`);
            this.core.logger.info('');

            for (const journeyFile of this.journeys) {
                const result = await this.runJourney(journeyFile);
                this.results.push(result);

                // Small delay between tests
                await this.core.sleep(2000);
            }

            this.printSummary();

            const exitCode = this.getExitCode();
            process.exit(exitCode);

        } catch (error) {
            this.core.logger.error(`Fatal error: ${error.message}`);
            process.exit(1);
        }
    }

    printSummary() {
        const elapsed = ((Date.now() - this.startTime) / 1000).toFixed(1);
        const passed = this.results.filter(r => r.success).length;
        const failed = this.results.filter(r => !r.success).length;

        this.core.logger.info('');
        this.core.logger.info('='.repeat(60));
        this.core.logger.info('ONBOARDING TUTORIAL TEST RESULTS');
        this.core.logger.info('='.repeat(60));
        this.core.logger.info(`Total: ${this.results.length} | Passed: ${passed} | Failed: ${failed}`);
        this.core.logger.info(`Time: ${elapsed}s`);
        this.core.logger.info('');

        this.results.forEach(r => {
            const status = r.success ? 'PASS' : 'FAIL';
            const statusIcon = r.success ? '[OK]' : '[X]';
            const journeyName = r.journey.replace('.js', '').replace('Journey-', '');
            this.core.logger.info(`${statusIcon} ${journeyName}`);
            if (!r.success && r.error) {
                this.core.logger.info(`    Error: ${r.error}`);
            }
        });

        this.core.logger.info('');
        this.core.logger.info('='.repeat(60));

        if (failed === 0) {
            this.core.logger.info('All onboarding tests passed!');
        } else {
            this.core.logger.info(`${failed} test(s) failed. Review output above.`);
        }
    }

    getExitCode() {
        return this.results.some(r => !r.success) ? 1 : 0;
    }
}

// Main execution
if (require.main === module) {
    const runner = new PluctTestFocusedOnboardingTutorial();
    runner.run().catch(error => {
        console.error('Fatal error:', error);
        process.exit(1);
    });
}

module.exports = PluctTestFocusedOnboardingTutorial;
