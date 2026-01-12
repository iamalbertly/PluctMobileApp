/**
 * Pluct-Maestro-Test-02Focused-01UXImprovements-01Runner
 * Focused test runner for UX improvements validation
 * Follows naming: [Project]-[Maestro]-[Test]-[Focused]-[Category]-[Sequence][Runner]
 */

const PluctMaestroTestRunnerOrchestrator = require('./Pluct-Maestro-Test-01Runner-01Orchestrator');

class PluctMaestroTestFocusedUXImprovementsRunner extends PluctMaestroTestRunnerOrchestrator {
    constructor() {
        super();
        this.devMode = process.env.DEV_MODE === '1' || process.env.NODE_ENV === 'development';
    }
    
    /**
     * Override discoverFlows to only get UX improvement flows
     */
    discoverFlows() {
        const allFlows = super.discoverFlows();
        
        // Filter to only UX improvement flows (22-28)
        const focusedFlows = allFlows.filter(flow => {
            const filename = flow.file;
            return filename.includes('Flow-22UX') ||
                   filename.includes('Flow-23UX') ||
                   filename.includes('Flow-24UX') ||
                   filename.includes('Flow-25UX') ||
                   filename.includes('Flow-26UX') ||
                   filename.includes('Flow-27UX') ||
                   filename.includes('Flow-28UX');
        });
        
        return focusedFlows;
    }
    
    /**
     * Run focused tests with auto-fix on first error in dev mode
     */
    async runFocused() {
        console.log('🎯 Starting Focused UX Improvements Test Suite');
        console.log(`   Mode: ${this.devMode ? 'DEV (terminates on first error)' : 'PROD (continues on errors)'}`);
        
        const flows = this.discoverFlows();
        console.log(`   Found ${flows.length} focused flow files\n`);
        
        for (const flow of flows) {
            try {
                const result = await this.runFlow(flow);
                if (!result.success && this.devMode) {
                    console.log(`\n❌ Terminating on first error (dev mode)`);
                    this.printSummary();
                    process.exit(1);
                }
            } catch (error) {
                if (this.devMode) {
                    console.log(`\n❌ Terminating on first error (dev mode): ${error.message}`);
                    this.printSummary();
                    process.exit(1);
                }
            }
        }
        
        this.printSummary();
        return this.getExitCode() === 0;
    }
}

// Main execution
if (require.main === module) {
    const runner = new PluctMaestroTestFocusedUXImprovementsRunner();
    runner.runFocused()
        .then(success => {
            process.exit(success ? 0 : 1);
        })
        .catch(error => {
            console.error('❌ Runner failed:', error.message);
            process.exit(1);
        });
}

module.exports = PluctMaestroTestFocusedUXImprovementsRunner;
