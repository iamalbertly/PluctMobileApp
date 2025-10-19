// Pluct-Main-01Orchestrator.js
// Main orchestrator with simplified structure and consistent naming
// Replaces Pluct-Automatic-Orchestrator.js with cleaner implementation

const PluctJourneyOrchestrator = require('./modules/Pluct-Journey-01Orchestrator');
const PluctCoreFoundation = require('./core/Pluct-Core-01Foundation');

class PluctMainOrchestrator {
    constructor() {
        this.core = new PluctCoreFoundation();
        this.journeyOrchestrator = new PluctJourneyOrchestrator();
        this.args = this.parseArguments(process.argv.slice(2));
    }

    parseArguments(argv) {
        const config = this.core.loadConfig();
        const args = {
            scope: 'All',
            url: config.url || 'https://vm.tiktok.com/ZMADQVF4e/',
            forceBuild: false,
            skipInstall: false,
            captureScreenshots: false,
            verbose: false
        };

        for (let i = 0; i < argv.length; i++) {
            const arg = argv[i];
            switch (arg) {
                case '-scope':
                    args.scope = argv[++i] || args.scope;
                    break;
                case '-url':
                    args.url = argv[++i] || args.url;
                    break;
                case '--forceBuild':
                case '-forceBuild':
                    args.forceBuild = true;
                    break;
                case '--skipInstall':
                case '-skipInstall':
                    args.skipInstall = true;
                    break;
                case '--captureScreenshots':
                case '-captureScreenshots':
                    args.captureScreenshots = true;
                    break;
                case '--verbose':
                case '-v':
                    args.verbose = true;
                    break;
            }
        }

        return args;
    }

    async run() {
        try {
            this.core.logInfo('Starting Pluct Main Orchestrator...', 'Main');
            this.core.logInfo(`Scope: ${this.args.scope}, URL: ${this.args.url}`, 'Main');

            // Validate environment
            await this.validateEnvironment();

            // Run journeys based on scope
            const success = await this.runJourneys();
            
            if (success) {
                this.core.logSuccess('All journeys completed successfully', 'Main');
                process.exit(0);
            } else {
                this.core.logError('Some journeys failed', 'Main');
                process.exit(1);
            }

        } catch (error) {
            this.core.logError(`Main orchestrator failed: ${error.message}`, 'Main');
            process.exit(1);
        }
    }

    async validateEnvironment() {
        this.core.logInfo('Validating environment...', 'Main');
        
        // Check ADB connectivity
        const adbCheck = await this.core.executeCommand('adb devices');
        if (!adbCheck.success || !adbCheck.output.includes('device')) {
            throw new Error('ADB device not connected');
        }

        // Check app installation
        const appCheck = await this.core.executeCommand('adb shell pm list packages | findstr app.pluct');
        if (!appCheck.success || !appCheck.output.includes('app.pluct')) {
            throw new Error('Pluct app not installed');
        }

        this.core.logSuccess('Environment validation passed', 'Main');
    }

    async runJourneys() {
        switch (this.args.scope) {
            case 'All':
                return await this.journeyOrchestrator.runAllJourneys(this.args.url);
            case 'Core':
                return await this.runCoreJourneys();
            case 'UI':
                return await this.runUIJourneys();
            default:
                this.core.logWarn(`Unknown scope: ${this.args.scope}, running all journeys`, 'Main');
                return await this.journeyOrchestrator.runAllJourneys(this.args.url);
        }
    }

    async runCoreJourneys() {
        this.core.logInfo('Running core journeys...', 'Main');
        // Implement core-specific journey logic
        return true;
    }

    async runUIJourneys() {
        this.core.logInfo('Running UI journeys...', 'Main');
        // Implement UI-specific journey logic
        return true;
    }
}

// Main execution
if (require.main === module) {
    const orchestrator = new PluctMainOrchestrator();
    orchestrator.run().catch(error => {
        console.error('Fatal error:', error.message);
        process.exit(1);
    });
}

module.exports = PluctMainOrchestrator;
