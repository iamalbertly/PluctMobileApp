/**
 * Pluct-Test-Focused-06RefactoringValidation-01Runner
 * Focused test runner for refactoring validation
 * Runs refactoring validation journeys to ensure all refactoring changes work correctly
 * Terminates on first failure in dev mode
 * Follows naming convention: [Project]-[Test]-[Focused]-[RefactoringValidation]-[Runner]
 * 5 scope layers: Project, Test, Focused, RefactoringValidation, Runner
 */

const PluctCoreFoundation = require('./core/Pluct-Core-01Foundation');
const PluctBuildDeployAutoService = require('./core/Pluct-Build-Deploy-01AutoService');
const path = require('path');
const fs = require('fs');

class PluctTestFocusedRefactoringValidation {
    constructor() {
        this.core = new PluctCoreFoundation();
        this.buildDeploy = new PluctBuildDeployAutoService(this.core);
        this.isDevMode = process.env.DEV_MODE === '1' || !process.env.CI;
        this.journeys = [
            'Journey-Refactor-07FileSizeCompliance-01Validation.js',
            'Journey-Refactor-08DuplicateElimination-01Validation.js',
            'Journey-Refactor-09SubmissionHandler-01Validation.js',
            'Journey-Refactor-10PollingHandler-01Validation.js',
            'Journey-Refactor-11E2EFlow-01Validation.js',
            'Journey-Refactor-12CreditValidation-01Validation.js'
        ];
        this.results = [];
    }

    /**
     * Run focused refactoring validation tests
     */
    async run() {
        this.core.logger.info('🎯 Starting focused refactoring validation...');
        this.core.logger.info(`Mode: ${this.isDevMode ? 'DEV (terminates on first error)' : 'PROD (continues on errors)'}`);
        
        // Build and deploy latest APK before testing
        this.core.logger.info('🔨 Building and deploying latest APK...');
        const buildSuccess = await this.buildDeploy.buildAndDeploy();
        if (!buildSuccess) {
            this.core.logger.error('❌ Build/deploy failed, cannot proceed with tests');
            process.exit(1);
        }
        this.core.logger.info('✅ Build and deploy successful');

        // Run all refactoring validation journeys
        for (const journeyFile of this.journeys) {
            const result = await this.runJourney(journeyFile);
            if (!result.success && this.isDevMode) {
                this.core.logger.error(`❌ Test failed: ${result.error}`);
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
            const result = await journey.execute();
            const duration = Date.now() - startTime;

            // Handle both { success: true } and boolean true results
            const isSuccess = result === true || (result && result.success === true);
            const error = isSuccess ? null : (result?.error || result || 'Unknown error');

            const journeyResult = {
                journey: journeyFile,
                success: isSuccess,
                duration: duration,
                error: error
            };
            this.results.push(journeyResult);

            if (journeyResult.success) {
                this.core.logger.info(`✅ ${journeyFile} passed (${duration}ms)`);
            } else {
                this.core.logger.error(`❌ ${journeyFile} failed: ${journeyResult.error}`);
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
     * Print test summary
     */
    printSummary() {
        this.core.logger.info('\n📊 Refactoring Validation Test Summary:');
        this.core.logger.info('='.repeat(60));
        
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

        if (failed === 0) {
            this.core.logger.info('\n🎉 All refactoring validation tests passed!');
        } else {
            this.core.logger.error(`\n❌ ${failed} test(s) failed. Please review errors above.`);
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
    const runner = new PluctTestFocusedRefactoringValidation();
    runner.run().catch(error => {
        console.error('Fatal error:', error);
        process.exit(1);
    });
}

module.exports = PluctTestFocusedRefactoringValidation;
