/**
 * Pluct-Test-Focused-02TrustFixes-01Runner
 * Focused test runner for trust fixes validation
 * Runs only: Journey-Trust-01, Journey-Trust-02, Journey-Trust-03, Journey-UX-11, Journey-UX-12, Journey-UX-13, Journey-UX-14
 * Plus edge case tests: Journey-EdgeCase-01 through 08
 * Terminates on first failure in dev mode
 * Follows naming convention: [Project]-[Test]-[Focused]-[TrustFixes]-[Runner]
 * 5 scope layers: Project, Test, Focused, TrustFixes, Runner
 */

const PluctCoreFoundation = require('./core/Pluct-Core-01Foundation');
const PluctTestAutoFixTrustFixes = require('./core/Pluct-Test-AutoFix-02TrustFixes-01Service');
const PluctBuildDeployAutoService = require('./core/Pluct-Build-Deploy-01AutoService');
const path = require('path');
const fs = require('fs');

class PluctTestFocusedTrustFixes {
    constructor() {
        this.core = new PluctCoreFoundation();
        this.autoFix = new PluctTestAutoFixTrustFixes(this.core);
        this.buildDeploy = new PluctBuildDeployAutoService(this.core);
        this.isDevMode = process.env.DEV_MODE === '1' || !process.env.CI;
        this.journeys = [
            'Journey-Trust-01TimeoutLogic-Validation.js',
            'Journey-Trust-02ErrorDeduplication-Validation.js',
            'Journey-Trust-03ADBDetection-Validation.js',
            'Journey-UX-11AutoSubmitIntent-Validation.js',
            'Journey-UX-12BackgroundProcessing-Validation.js',
            'Journey-UX-13NotificationNavigation-Validation.js',
            'Journey-UX-14CreditQueueFlow-Validation.js'
        ];
        this.edgeCaseJourneys = [
            'Journey-EdgeCase-01RapidIntentReceipt-Validation.js',
            'Journey-EdgeCase-02CreditDepletion-Validation.js',
            'Journey-EdgeCase-03NetworkLoss-Validation.js',
            'Journey-EdgeCase-04MultipleNotifications-Validation.js',
            'Journey-EdgeCase-05JWTExpiration-Validation.js',
            'Journey-EdgeCase-06ConcurrentVending-Validation.js',
            'Journey-EdgeCase-07TokenExpirationPolling-Validation.js',
            'Journey-EdgeCase-08NetworkInterruption-Validation.js'
        ];
        this.results = [];
    }

    /**
     * Run focused trust fixes tests
     */
    async run() {
        this.core.logger.info('🎯 Starting focused trust fixes validation...');
        this.core.logger.info(`Mode: ${this.isDevMode ? 'DEV (terminates on first error)' : 'PROD (continues on errors)'}`);
        
        // Attempt auto-fix before starting
        const fixesApplied = await this.autoFix.detectAndFix();
        if (fixesApplied) {
            this.core.logger.info(`✅ Auto-fixes applied:\n${this.autoFix.getSummary()}`);
        }

        // Run main trust fixes tests
        for (const journeyFile of this.journeys) {
            const result = await this.runJourney(journeyFile);
            if (!result.success && this.isDevMode) {
                // Attempt auto-fix
                const fixed = await this.autoFix.attemptFix(result);
                if (fixed) {
                    // Rebuild, redeploy, retry
                    await this.rebuildAndRetry(journeyFile);
                } else {
                    this.core.logger.error(`❌ Test failed and could not be auto-fixed: ${result.error}`);
                    break;
                }
            }
        }
        
        // Run edge case tests
        for (const journeyFile of this.edgeCaseJourneys) {
            const result = await this.runJourney(journeyFile);
            if (!result.success && this.isDevMode) {
                this.core.logger.error(`❌ Edge case test failed: ${result.error}`);
                break;
            }
        }

        this.printSummary();
        process.exit(this.getExitCode());
    }

    /**
     * Run a single journey test
     */
    async runJourney(journeyFile) {
        const journeyPath = path.join(__dirname, 'journeys', journeyFile);
        
        if (!fs.existsSync(journeyPath)) {
            this.core.logger.warn(`⚠️  Journey file not found: ${journeyFile}`);
            const result = {
                journey: journeyFile,
                success: false,
                error: 'File not found'
            };
            this.results.push(result);
            return result;
        }

        this.core.logger.info(`\n📋 Running: ${journeyFile}`);
        
        try {
            const JourneyClass = require(journeyPath);
            const journey = new JourneyClass(this.core);
            
            const startTime = Date.now();
            const result = await journey.run();
            const duration = Date.now() - startTime;

            const journeyResult = {
                journey: journeyFile,
                success: result === true,
                duration: duration,
                error: result === true ? null : result
            };
            this.results.push(journeyResult);

            if (result === true) {
                this.core.logger.info(`✅ ${journeyFile} passed (${duration}ms)`);
            } else {
                this.core.logger.error(`❌ ${journeyFile} failed: ${result}`);
            }

            return journeyResult;
        } catch (error) {
            this.core.logger.error(`❌ ${journeyFile} threw error: ${error.message}`);
            const result = {
                journey: journeyFile,
                success: false,
                error: error.message,
                stack: error.stack
            };
            this.results.push(result);
            return result;
        }
    }

    /**
     * Rebuild and retry after auto-fix
     */
    async rebuildAndRetry(journeyFile) {
        this.core.logger.info(`🔨 Rebuilding and redeploying after auto-fix...`);
        
        try {
            const buildSuccess = await this.buildDeploy.buildAndDeploy();
            if (buildSuccess) {
                this.core.logger.info(`✅ Rebuild successful, retrying: ${journeyFile}`);
                await this.runJourney(journeyFile);
            } else {
                this.core.logger.error(`❌ Rebuild failed, cannot retry`);
            }
        } catch (error) {
            this.core.logger.error(`❌ Rebuild/retry error: ${error.message}`);
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
    const runner = new PluctTestFocusedTrustFixes();
    runner.run().catch(error => {
        console.error('Fatal error:', error);
        process.exit(1);
    });
}

module.exports = PluctTestFocusedTrustFixes;

