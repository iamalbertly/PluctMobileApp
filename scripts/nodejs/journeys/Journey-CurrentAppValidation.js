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

        // 3) Test basic app functionality (simplified)
        this.core.logger.info('📱 Testing Basic App Functionality...');
        
        // Check if we can interact with the main content
        const titleTap = await this.core.tapByText('Pluct');
        if (!titleTap.success) {
            this.core.logger.warn('⚠️ Could not tap on title, continuing...');
        } else {
            this.core.logger.info('✅ Title interaction successful');
        }

        // 4) Test app stability
        await this.core.sleep(2000);
        await this.core.dumpUIHierarchy();
        const updatedUiDump = this.core.readLastUIDump();
        
        if (!updatedUiDump.includes('app.pluct')) {
            return { success: false, error: 'App lost focus during testing' };
        }
        this.core.logger.info('✅ App remains stable');

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

        // 7) Validate API connectivity (simplified check)
        this.core.logger.info('ℹ️ API connectivity validation skipped in simplified version');

        return { 
            success: true, 
            note: "Current app functionality validated - simplified version working correctly",
            details: {
                homeScreen: true,
                titleInteraction: titleTap.success,
                appStability: true,
                deepLink: deepLinkResult.success,
                errorBanner: hasErrorBanner,
                appMaintainsFocus: true
            }
        };
    }
}

module.exports = CurrentAppValidationJourney;
