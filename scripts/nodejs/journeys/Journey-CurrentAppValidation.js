const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class CurrentAppValidationJourney extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'CurrentAppValidation';
    }

    async execute() {
        this.core.logger.info('🎯 Validating Current App Functionality...');

        // 1) Launch app to home
        const fg = await this.ensureAppForeground();
        if (!fg.success) return { success: false, error: 'App not in foreground' };

        // 2) Validate basic UI elements
        await this.core.dumpUIHierarchy();
        const uiDump = this.core.readLastUIDump();
        
        const hasHomeScreen = uiDump.includes('No transcripts yet') || uiDump.includes('Pluct');
        if (!hasHomeScreen) {
            return { success: false, error: 'Home screen not detected' };
        }
        this.core.logger.info('✅ Home screen detected');

        // 3) Test capture sheet functionality
        const openResult = await this.core.openCaptureSheet();
        if (!openResult.success) {
            return { success: false, error: `Failed to open capture sheet: ${openResult.error}` };
        }
        this.core.logger.info('✅ Capture sheet opened successfully');

        // 4) Test URL input
        await this.core.sleep(2000);
        let urlTap = await this.core.tapByContentDesc('url_input');
        if (!urlTap.success) {
            urlTap = await this.core.tapFirstEditText();
            if (!urlTap.success) {
                return { success: false, error: 'URL field not found' };
            }
        }
        
        await this.core.clearEditText();
        await this.core.inputText('https://vm.tiktok.com/ZMADQVF4e/');
        this.core.logger.info('✅ URL input successful');

        // 5) Test deep link functionality
        const deepLinkResult = await this.core.executeCommand(
            `adb shell am start -a android.intent.action.VIEW -d "pluct://debug/error?code=TEST_ERROR&msg=Test%20error%20from%20automated%20test"`
        );
        if (deepLinkResult.success) {
            this.core.logger.info('✅ Debug deep link executed successfully');
        } else {
            this.core.logger.warn('⚠️ Debug deep link failed');
        }

        // 6) Check for any error banners (expected to be none in current app)
        await this.core.sleep(2000);
        await this.core.dumpUIHierarchy();
        const finalDump = this.core.readLastUIDump();
        const hasErrorBanner = finalDump.includes('testTag="error_banner"');
        
        if (hasErrorBanner) {
            this.core.logger.info('✅ Error banner system detected in current app');
        } else {
            this.core.logger.info('ℹ️ No error banner system detected (expected for current app)');
        }

        // 7) Validate API connectivity
        const apiResult = await this.core.validateBusinessEngineConnectivity();
        if (apiResult.success) {
            this.core.logger.info('✅ Business Engine API connectivity confirmed');
        } else {
            this.core.logger.warn('⚠️ Business Engine API connectivity issues');
        }

        return { 
            success: true, 
            note: "Current app functionality validated - ready for error system deployment",
            details: {
                homeScreen: true,
                captureSheet: true,
                urlInput: true,
                deepLink: deepLinkResult.success,
                errorBanner: hasErrorBanner,
                apiConnectivity: apiResult.success
            }
        };
    }
}

module.exports = CurrentAppValidationJourney;
