/**
 * Pluct-Test-Focused-01UXFixes
 * Focused test runner for UX fixes validation
 * Runs only: Journey-UX-10, Journey-UX-11, Journey-UX-12, Journey-UX-13
 * Terminates on first failure in dev mode
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Responsibility]
 */

const PluctCoreFoundation = require('./core/Pluct-Core-01Foundation');
const PluctTestAutoFix = require('./core/Pluct-Test-AutoFix-01CommonIssues');
const path = require('path');
const fs = require('fs');

class PluctTestFocusedUXFixes {
    constructor() {
        this.core = new PluctCoreFoundation();
        this.autoFix = new PluctTestAutoFix(this.core);
        this.isDevMode = process.env.DEV_MODE === '1';
        this.journeys = [
            'Journey-UX-10ErrorPersistence-Validation.js',
            'Journey-UX-11OneTapCopy-Validation.js',
            'Journey-UX-12QueueNotification-Validation.js',
            'Journey-UX-13PreValidationQueue-Validation.js'
        ];
        this.results = [];
    }

    /**
     * Run focused UX fixes tests
     */
    async run() {
        this.core.logger.info('🎯 Starting focused UX fixes validation...');
        this.core.logger.info(`Mode: ${this.isDevMode ? 'DEV (terminates on first error)' : 'PROD (continues on errors)'}`);
        
        // Attempt auto-fix before starting
        const fixesApplied = await this.autoFix.detectAndFix();
        if (fixesApplied) {
            this.core.logger.info(`✅ Auto-fixes applied:\n${this.autoFix.getSummary()}`);
        }

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
    const runner = new PluctTestFocusedUXFixes();
    runner.run().catch(error => {
        console.error('Fatal error:', error);
        process.exit(1);
    });
}

module.exports = PluctTestFocusedUXFixes;



