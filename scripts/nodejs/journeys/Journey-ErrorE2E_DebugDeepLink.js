const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class ErrorE2EDebugDeepLinkJourney extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'ErrorE2E_DebugDeepLink';
    }

    async execute() {
        this.core.logger.info('🎯 Testing Error E2E Debug Deep Link...');

        // 1) Launch app to home
        const fg = await this.ensureAppForeground();
        if (!fg.success) return { success: false, error: 'App not in foreground' };

        // 2) Trigger debug deep link
        const t0 = Date.now();
        const deepLinkResult = await this.core.executeCommand(
            `adb shell am start -a android.intent.action.VIEW -d "pluct://debug/error?code=AUTH_401&msg=Bad%20token"`
        );
        
        if (!deepLinkResult.success) {
            return { success: false, error: 'Debug deep link failed' };
        }

        // 3) Wait up to 5s for UI banner
        const okUI = await this.core.waitFor(() => {
            return this.core.dumpUIHierarchy().then(h => h.includes('testTag="error_banner"'));
        }, 5000);
        
        if (!okUI) {
            return { success: false, error: "Banner did not appear after debug deep link" };
        }

        // 4) Extract contentDescription to verify code
        const tree = await this.core.dumpUIHierarchy();
        if (!tree.includes('content-desc="error_code:AUTH_401"') && 
            !tree.includes('contentDescription="error_code:AUTH_401"')) {
            return { success: false, error: "Banner did not expose expected error_code:AUTH_401" };
        }

        // 5) Log assertion
        const logs = await this.core.readLogcatSince(t0, "PLUCT_ERR");
        const has401 = logs.some(l => 
            l.includes('"type":"ui_error"') && l.includes('"code":"AUTH_401"')
        );
        
        if (!has401) {
            return { success: false, error: "Structured log PLUCT_ERR for AUTH_401 not found" };
        }

        this.core.logger.info('✅ Error E2E Debug Deep Link test passed');
        return { success: true };
    }
}

module.exports = ErrorE2EDebugDeepLinkJourney;