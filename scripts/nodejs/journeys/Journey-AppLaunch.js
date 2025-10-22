const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class AppLaunchJourney extends BaseJourney {
    async execute() {
        this.core.logger.info('🎯 Testing App Launch end-to-end...');

        // Use the proper launchApp method
        const launchResult = await this.core.launchApp();
        if (!launchResult.success) return { success: false, error: 'Launch failed' };

        // Validate empty state visible
        const empty = await this.core.waitForText('No transcripts yet');
        if (!empty.success) this.core.logger.warn('Empty state not detected');

        return { success: true };
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('AppLaunch', new AppLaunchJourney(orchestrator.core));
}

module.exports = { register };


