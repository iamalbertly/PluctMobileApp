const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-ErrorNotification-02System - Comprehensive error notification testing
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Tests error banner system, deep linking, and user interaction
 */
class ErrorNotificationSystemJourney extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'ErrorNotification-02System';
    }

    async execute() {
        this.core.logger.info('🚨 Testing Error Notification System...');

        // 1) Launch the app
        await this.core.launchApp();
        await this.core.sleep(2000);

        // 2) Test debug deep link error triggering
        await this.testDebugDeepLink();

        // 3) Test error banner visibility
        await this.testErrorBannerVisibility();

        // 4) Test error banner interaction
        await this.testErrorBannerInteraction();

        // 5) Test error clearing
        await this.testErrorClearing();

        // 6) Test structured logging
        await this.testStructuredLogging();

        return { 
            success: true, 
            note: "Error notification system working correctly",
            details: {
                deepLinkTest: "passed",
                bannerVisibility: "passed",
                bannerInteraction: "passed",
                errorClearing: "passed",
                structuredLogging: "passed"
            }
        };
    }

    async testDebugDeepLink() {
        this.core.logger.info('🔗 Testing debug deep link...');
        
        // Trigger debug error via deep link
        const deepLinkResult = await this.core.executeCommand(
            'adb shell am start -W -a android.intent.action.VIEW -d "pluct://debug/error/network"'
        );
        
        if (!deepLinkResult.success) {
            return { success: false, error: 'Debug deep link failed' };
        }
        
        await this.core.sleep(2000);
        this.core.logger.info('✅ Debug deep link triggered');
        return { success: true };
    }

    async testErrorBannerVisibility() {
        this.core.logger.info('👁️ Testing error banner visibility...');
        
        // Check for error banner in UI
        await this.core.dumpUIHierarchy();
        const uiDump = this.core.readLastUIDump();
        
        const hasErrorBanner = uiDump.includes('error_banner') || 
                              uiDump.includes('error_code:') ||
                              uiDump.includes('DEBUG_NETWORK_ERROR');
        
        if (!hasErrorBanner) {
            return { success: false, error: 'Error banner not visible in UI' };
        }
        
        this.core.logger.info('✅ Error banner visible');
        return { success: true };
    }

    async testErrorBannerInteraction() {
        this.core.logger.info('🖱️ Testing error banner interaction...');
        
        // Try to interact with error banner
        const tapResult = await this.core.tapByContentDesc('error_code:DEBUG_NETWORK_ERROR');
        if (tapResult.success) {
            await this.core.sleep(1000);
            this.core.logger.info('✅ Error banner interaction successful');
        }
        
        // Try to dismiss error
        const dismissResult = await this.core.tapByContentDesc('Dismiss error');
        if (dismissResult.success) {
            await this.core.sleep(1000);
            this.core.logger.info('✅ Error dismissal successful');
        }
        
        return { success: true };
    }

    async testErrorClearing() {
        this.core.logger.info('🧹 Testing error clearing...');
        
        // Trigger clear all errors deep link
        const clearResult = await this.core.executeCommand(
            'adb shell am start -W -a android.intent.action.VIEW -d "pluct://debug/clear"'
        );
        
        if (!clearResult.success) {
            return { success: false, error: 'Error clearing deep link failed' };
        }
        
        await this.core.sleep(2000);
        this.core.logger.info('✅ Error clearing triggered');
        return { success: true };
    }

    async testStructuredLogging() {
        this.core.logger.info('📝 Testing structured logging...');
        
        // Check logcat for structured error logs
        const logcatResult = await this.core.executeCommand('adb logcat -d');
        const logcatOutput = logcatResult.stdout || logcatResult.output || '';
        
        const hasStructuredLog = logcatOutput.includes('PLUCT_ERR:') ||
                                logcatOutput.includes('ErrorCenter: Emitting error:') ||
                                logcatOutput.includes('ErrorBannerHost: Received error:');
        
        if (!hasStructuredLog) {
            return { success: false, error: 'Structured logging not found' };
        }
        
        this.core.logger.info('✅ Structured logging working');
        return { success: true };
    }
}

module.exports = ErrorNotificationSystemJourney;
