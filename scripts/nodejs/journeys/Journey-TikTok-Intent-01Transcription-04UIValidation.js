const PluctCoreFoundation = require('../core/Pluct-Core-01Foundation');

class ChildUIValidation {
    constructor() {
        this.core = new PluctCoreFoundation();
    }

    async execute() {
        try {
            this.core.logger.info('🧪 [Child-04] Validating UI indicators (processing/error/completion)...');
            // Ensure the app is launched and in foreground so UI can be inspected
            const launch = await this.core.launchApp();
            if (!launch.success) {
                return { success: false, error: 'Failed to launch app for UI validation' };
            }
            await this.core.sleep(1500);
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            const hasIndicators = uiDump.includes('Processing') ||
                                  uiDump.includes('content-desc="Error message"') ||
                                  uiDump.includes('API Error') ||
                                  uiDump.includes('transcript') ||
                                  uiDump.includes('completed');
            if (hasIndicators) return { success: true };
            return { success: false, error: 'No UI indicators found' };
        } catch (err) {
            return { success: false, error: err.message };
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('TikTok-Intent-01Transcription-04UIValidation', new ChildUIValidation());
}

module.exports = { ChildUIValidation, register };


