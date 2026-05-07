/**
 * Compatibility entrypoint for older commands.
 * The active validation path is the Node journey orchestrator under scripts/nodejs.
 */

const PluctMainOrchestrator = require('../scripts/nodejs/Pluct-Main-01Orchestrator');

class PluctNodeJourneyCompatibilityRunner {
    constructor() {
        this.orchestrator = new PluctMainOrchestrator();
    }

    async runAll() {
        const args = process.argv.slice(2);
        const options = {
            forceFull: args.includes('--force-full') || args.includes('-f'),
            tests: []
        };

        args.forEach((arg, index) => {
            if (arg.startsWith('--test=')) {
                const name = arg.split('=')[1];
                if (name) options.tests.push(name.endsWith('.js') ? name : `${name}.js`);
            }
            if (arg === '--test' && args[index + 1] && !args[index + 1].startsWith('--')) {
                const name = args[index + 1];
                options.tests.push(name.endsWith('.js') ? name : `${name}.js`);
            }
        });

        const result = await this.orchestrator.run(options);
        return result.success;
    }
}

if (require.main === module) {
    const runner = new PluctNodeJourneyCompatibilityRunner();
    runner.runAll()
        .then(success => process.exit(success ? 0 : 1))
        .catch(error => {
            console.error('Node journey runner failed:', error.message);
            process.exit(1);
        });
}

module.exports = PluctNodeJourneyCompatibilityRunner;
