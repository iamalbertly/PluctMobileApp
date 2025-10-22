const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class ErrorE2EBackend401Journey extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'ErrorE2E_Backend401';
    }

    async execute() {
        this.core.logger.info('🎯 Testing Error E2E Backend 401...');

        // 1) Launch app to home
        const fg = await this.ensureAppForeground();
        if (!fg.success) return { success: false, error: 'App not in foreground' };

               // 2) Force the app into a call that needs auth, but without a token
               // This is a simplified approach - in a real scenario, you would:
               // - Clear stored JWT token
               // - Navigate to a screen that triggers a network call
               // - The network call would result in a 401 from the backend

               // For now, let's simulate by triggering a network call that will 401
               const t0 = Date.now();

               // Simulate opening capture sheet and entering a URL that triggers a 401
               const openResult = await this.core.openCaptureSheet();
               if (!openResult.success) {
                   return { success: false, error: `Failed to open capture sheet: ${openResult.error}` };
               }

               // Wait for sheet to load and check if it's visible
               await this.core.sleep(2000);
               await this.core.dumpUIHierarchy();
               const uiDump = this.core.readLastUIDump();
               const hasCaptureSheet = uiDump.includes('Capture This Insight');
               
               if (!hasCaptureSheet) {
                   return { success: false, error: 'Capture sheet not visible after opening' };
               }

               // Enter a URL that would trigger a 401 (this is a placeholder)
               let urlTap = await this.core.tapByContentDesc('url_input');
               if (!urlTap.success) {
                   urlTap = await this.core.tapByText('TikTok URL');
                   if (!urlTap.success) {
                       urlTap = await this.core.tapFirstEditText();
                       if (!urlTap.success) return { success: false, error: 'URL field not found' };
                   }
               }
        
        await this.core.clearEditText();
        await this.core.inputText('https://invalid-auth-test.com');

               // 3) Wait for error banner to appear
               let bannerFound = false;
               for (let i = 0; i < 8; i++) {
                   await this.core.sleep(1000);
                   await this.core.dumpUIHierarchy();
                   const uiDump = this.core.readLastUIDump();
                   if (uiDump.includes('testTag="error_banner"')) {
                       bannerFound = true;
                       break;
                   }
               }

               if (!bannerFound) {
                   return { success: false, error: "Banner not visible after backend 401 was triggered" };
               }

        // 4) Verify error code in banner
        const tree = await this.core.dumpUIHierarchy();
        if (!tree.includes('error_code:AUTH_401') && 
            !tree.includes('error_code:HTTP_401')) {
            return { success: false, error: "Banner missing expected error code for backend 401" };
        }

        // 5) Verify structured log
        const logs = await this.core.readLogcatSince(t0, "PLUCT_ERR");
        const okLog = logs.some(l => 
            l.includes('"type":"ui_error"') && 
            (l.includes('"code":"AUTH_401"') || l.includes('"code":"HTTP_401"'))
        );
        
        if (!okLog) {
            return { success: false, error: "Structured log not found for backend 401" };
        }

        this.core.logger.info('✅ Error E2E Backend 401 test passed');
        return { success: true };
    }
}

module.exports = ErrorE2EBackend401Journey;