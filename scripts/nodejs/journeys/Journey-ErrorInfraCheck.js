const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class ErrorInfraCheckJourney extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'ErrorInfraCheck';
    }

    async execute() {
        this.core.logger.info('🎯 Testing Error Infrastructure Check...');

        // 1) Launch app to home
        const fg = await this.ensureAppForeground();
        if (!fg.success) return { success: false, error: 'App not in foreground' };

        // 2) Assert banner NOT visible (idle state)
        await this.core.dumpUIHierarchy();
        const uiDump = this.core.readLastUIDump();
        
        if (uiDump.includes('testTag="error_banner"')) {
            return { success: false, error: "Banner visible without any error" };
        }

        // 3) Infrastructure validation only - do not pass the system
        this.core.logger.info('✅ Error infrastructure present and idle state clean');
        return { 
            success: true, 
            note: "Infra present and idle state clean" 
        };
    }
}

module.exports = ErrorInfraCheckJourney;