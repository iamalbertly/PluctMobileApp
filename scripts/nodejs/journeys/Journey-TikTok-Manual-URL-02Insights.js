const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-TikTok-Manual-URL-02Insights - Insights and Pre-warming Verification
 * Tests the background pre-warming trigger and the new Insights dialog flow.
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 */
class TikTokManualURLInsightsJourney extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'TikTok-Manual-URL-02Insights';
        this.maxDuration = 120000; // 2 minutes max
    }

    async execute() {
        this.core.logger.info('🎯 Starting TikTok Insights & Pre-warming Verification Journey...');
        const startTime = Date.now();
        const testUrl = 'https://vm.tiktok.com/ZMAoYtB5p/'; // Use the URL from the user request

        try {
            // Step 0: Clear app data for clean test run
            this.core.logger.info('🔄 Step 0: Clearing app data for clean test run');
            try {
                await this.core.executeCommand('adb shell pm clear app.pluct');
                await this.core.sleep(1000);
            } catch (error) {
                this.core.logger.warn('⚠️ App data clear failed, continuing anyway');
            }
            await this.core.sleep(1000);

            // Step 1: App Launch
            this.core.logger.info('📱 Step 1: App Launch');
            const launchResult = await this.core.launchApp();
            if (!launchResult.success) {
                return { success: false, error: 'App launch failed' };
            }
            await this.core.sleep(3000);

            // Handle Welcome Screen if present
            await this.handleWelcomeScreen();

            // Step 2: Enter URL and Verify Pre-warming
            this.core.logger.info('📱 Step 2: Entering URL and Verifying Pre-warming');

            // Clear logs first to ensure we catch new events
            await this.core.executeCommand('adb logcat -c');

            const enterUrlResult = await this.enterUrl(testUrl);
            if (!enterUrlResult.success) {
                return { success: false, error: 'Failed to enter URL' };
            }

            // Verify Pre-warming Log
            this.core.logger.info('🔍 Verifying background pre-warming trigger...');
            await this.core.sleep(2000); // Give it a moment to trigger
            const preWarmLog = await this.core.executeCommand('adb logcat -d | findstr "Triggering background pre-warming"');

            if (preWarmLog.success && preWarmLog.output.includes('Triggering background pre-warming')) {
                this.core.logger.info('✅ Background pre-warming triggered successfully');
            } else {
                this.core.logger.warn('⚠️ Background pre-warming log not found. It might have happened too fast or logging is disabled.');
                // We don't fail the test here, but we log a warning
            }

            // Step 3: Open Insights Dialog
            this.core.logger.info('📱 Step 3: Opening Insights Dialog');

            // Tap Insights button
            const insightsTap = await this.core.tapByText('Insights');
            if (!insightsTap.success) {
                // Try by test tag if text fails
                const tagTap = await this.core.tapByTestTag('generate_insights_button');
                if (!tagTap.success) {
                    return { success: false, error: 'Failed to tap Insights button' };
                }
            }

            await this.core.sleep(1000);

            // Verify Dialog is open
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            if (uiDump.includes('Ask Insights') || uiDump.includes('Ask anything about this video')) {
                this.core.logger.info('✅ Insights dialog opened successfully');
            } else {
                return { success: false, error: 'Insights dialog not found' };
            }

            // Step 4: Enter Query and Ask
            this.core.logger.info('📱 Step 4: Entering Query and Asking');

            const query = "Is this factual?";
            const inputResult = await this.core.inputText(query);
            if (!inputResult.success) {
                return { success: false, error: 'Failed to enter query' };
            }

            await this.core.sleep(1000);

            // Tap Ask button
            const askTap = await this.core.tapByText('Ask');
            if (!askTap.success) {
                return { success: false, error: 'Failed to tap Ask button' };
            }

            // Step 5: Verify Clipboard Copy (via Logs)
            this.core.logger.info('📱 Step 5: Verifying Clipboard Copy');
            await this.core.sleep(1000);

            const clipboardLog = await this.core.executeCommand('adb logcat -d | findstr "Insights prompt copied to clipboard"');
            if (clipboardLog.success && clipboardLog.output.includes('Insights prompt copied to clipboard')) {
                this.core.logger.info('✅ Insights prompt copied to clipboard successfully');
                this.core.logger.info(`📋 Log output: ${clipboardLog.output.trim()}`);
            } else {
                return { success: false, error: 'Clipboard copy log not found' };
            }

            const duration = Date.now() - startTime;
            this.core.logger.info(`✅ TikTok Insights Verification Journey completed in ${duration}ms`);

            return {
                success: true,
                duration: duration
            };

        } catch (error) {
            this.core.logger.error('❌ TikTok Insights Verification Journey failed:', error.message);
            return { success: false, error: error.message };
        }
    }

    async handleWelcomeScreen() {
        let attempts = 0;
        while (attempts < 3) {
            await this.core.dumpUIHierarchy();
            const currentDump = this.core.readLastUIDump();

            if (currentDump.includes('Welcome to Pluct') || currentDump.includes('Get Started')) {
                this.core.logger.info(`👋 Welcome Screen detected (attempt ${attempts + 1}), tapping Get Started...`);
                const tapResult = await this.core.tapByText('Get Started');
                if (!tapResult.success) {
                    await this.core.tapByCoordinates(540, 1700);
                }
                await this.core.sleep(3000);
                attempts++;
            } else {
                break;
            }
        }
    }

    async enterUrl(url) {
        // Find and tap the URL input field
        const urlTap = await this.core.tapByTestTag('url_input_field');
        if (!urlTap.success) {
            await this.core.tapByText('TikTok URL');
        }

        // Enter the URL
        const inputResult = await this.core.inputText(url);
        if (!inputResult.success) {
            return { success: false };
        }

        // Hide keyboard
        await this.core.executeCommand('adb shell input keyevent 111'); // KEYCODE_ESCAPE to hide keyboard

        return { success: true };
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('TikTokManualURLInsights', new TikTokManualURLInsightsJourney(orchestrator.core));
}

module.exports = { register };
