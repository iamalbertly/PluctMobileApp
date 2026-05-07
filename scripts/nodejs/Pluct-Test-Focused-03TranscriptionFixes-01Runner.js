/**
 * Pluct-Test-Focused-03TranscriptionFixes-01Runner.js
 * Focused test runner for transcription flow fixes validation
 * Follows naming: [Project]-[Test]-[Focused]-[TranscriptionFixes]-[Runner]
 * 5 scope layers: Project, Test, Focused, TranscriptionFixes, Runner
 * Terminates on first error during development phase
 */
const { PluctJourneyOrchestrator } = require('./journeys/Pluct-Journey-01Orchestrator');
const PluctCoreFoundation = require('./core/Pluct-Core-01Foundation');

class PluctTestFocusedTranscriptionFixesRunner {
    constructor() {
        this.core = new PluctCoreFoundation();
        this.orchestrator = new PluctJourneyOrchestrator();
        this.devMode = process.env.DEV_MODE === '1' || process.env.NODE_ENV === 'development';
        this.results = [];
    }

    /**
     * Get focused test journeys for transcription fixes
     */
    getFocusedJourneys() {
        return [
            'Journey-Transcription-01IntentFlow-Complete-Validation.js',
            'Journey-Intent-01TikTok-02AutoSubmit-01Validation.js',
            'Journey-Duplicate-01ProcessingLock-Validation.js',
            'Journey-EdgeCase-04MultipleNotifications-Validation.js'
        ];
    }

    /**
     * Run focused test suite
     */
    async runFocusedTests() {
        console.log('🎯 Starting Focused Transcription Fixes Validation...');
        console.log(`   Mode: ${this.devMode ? 'DEV (terminates on first error)' : 'PROD (continues on errors)'}`);
        console.log('');

        const focusedJourneys = this.getFocusedJourneys();
        console.log(`   Found ${focusedJourneys.length} focused test journeys\n`);

        // Load all journeys first
        await this.orchestrator.loadAllJourneys();

        for (const journeyFile of focusedJourneys) {
            let attempt = 0;
            const maxAttempts = 2; // Allow one retry
            
            while (attempt < maxAttempts) {
                try {
                    attempt++;
                    console.log(`\n📋 Running: ${journeyFile} (attempt ${attempt}/${maxAttempts})`);
                    
                    // Find journey by file name
                    let journeyName = this.orchestrator.journeyNameMapping[journeyFile];
                    if (!journeyName) {
                        // Try to extract from file name
                        const baseName = journeyFile.replace(/^Journey-/, '').replace(/\.js$/, '');
                        journeyName = baseName;
                    }
                    
                    const journey = this.orchestrator.journeys.get(journeyName);
                    
                    if (!journey) {
                        console.error(`   ❌ Journey not found: ${journeyName} (from ${journeyFile})`);
                        this.results.push({
                            journey: journeyFile,
                            success: false,
                            error: `Journey not registered: ${journeyName}`
                        });
                        break;
                    }
                    
                    const result = await this.orchestrator.runJourney(journeyName);
                    
                    this.results.push({
                        journey: journeyFile,
                        success: result.success,
                        duration: result.duration,
                        error: result.error,
                        attempt: attempt
                    });

                    if (result.success) {
                        console.log(`   ✅ PASSED (${result.duration || 0}ms)`);
                        break; // Success, move to next journey
                    } else {
                        console.log(`   ❌ FAILED: ${result.error}`);
                        
                        if (attempt < maxAttempts) {
                            console.log(`   🔧 Waiting before retry...`);
                            await new Promise(resolve => setTimeout(resolve, 2000));
                        } else {
                            if (this.devMode) {
                                console.log('\n❌ Terminating on first error (dev mode)');
                                this.printSummary();
                                process.exit(1);
                            }
                        }
                    }
                } catch (error) {
                    console.error(`   ❌ ERROR: ${error.message}`);
                    this.results.push({
                        journey: journeyFile,
                        success: false,
                        error: error.message,
                        attempt: attempt
                    });

                    if (this.devMode || attempt >= maxAttempts) {
                        console.log('\n❌ Terminating on error (dev mode or max attempts reached)');
                        this.printSummary();
                        process.exit(1);
                    }
                }
            }
        }

        this.printSummary();
        return this.getExitCode() === 0;
    }

    /**
     * Print test summary
     */
    printSummary() {
        const successful = this.results.filter(r => r.success).length;
        const total = this.results.length;
        const successRate = total > 0 ? (successful / total * 100).toFixed(1) : 0;

        console.log('\n📊 === FOCUSED TEST SUMMARY ===');
        console.log(`   Total: ${total}`);
        console.log(`   Passed: ${successful}`);
        console.log(`   Failed: ${total - successful}`);
        console.log(`   Success Rate: ${successRate}%`);

        if (this.results.some(r => !r.success)) {
            console.log('\n❌ Failed Tests:');
            this.results.filter(r => !r.success).forEach(r => {
                console.log(`   - ${r.journey}: ${r.error}`);
            });
        }
    }

    /**
     * Get exit code
     */
    getExitCode() {
        return this.results.every(r => r.success) ? 0 : 1;
    }
}

// Main execution
if (require.main === module) {
    const runner = new PluctTestFocusedTranscriptionFixesRunner();
    runner.runFocusedTests()
        .then(success => {
            process.exit(success ? 0 : 1);
        })
        .catch(error => {
            console.error('❌ Runner failed:', error.message);
            process.exit(1);
        });
}

module.exports = PluctTestFocusedTranscriptionFixesRunner;
