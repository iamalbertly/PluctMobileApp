const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class ErrorNotificationValidationJourney extends BaseJourney {
    async execute() {
        this.core.logger.info('🔴 Validating Error Notification System...');

        // 1) Launch the app
        await this.core.launchApp();
        await this.core.sleep(2000);

        // 2) Check if error test section is visible
        await this.core.dumpUIHierarchy();
        let uiDump = this.core.readLastUIDump();
        
        if (!uiDump.includes('Error Banner Test')) {
            this.core.logger.error('❌ Error test section not found');
            return { success: false, error: 'Error test section not found' };
        }
        this.core.logger.info('✅ Error test section found');

        // 3) Clear logcat to start fresh
        await this.core.executeCommand('adb logcat -c');
        this.core.logger.info('📱 Logcat cleared');

        // 4) Trigger network error and check for UI banner
        this.core.logger.info('🔴 Triggering Network Error...');
        await this.core.tapByText('Network Error');
        await this.core.sleep(3000); // Give time for error to appear

        // 5) Check UI for error banner
        await this.core.dumpUIHierarchy();
        uiDump = this.core.readLastUIDump();
        
        const errorBannerVisible = uiDump.includes('testTag="error_banner"') || 
                                 uiDump.includes('error_code:') ||
                                 uiDump.includes('Network connection failed');
        
        if (!errorBannerVisible) {
            this.core.logger.warn('⚠️ Error banner not visible in UI dump');
        } else {
            this.core.logger.info('✅ Error banner detected in UI');
        }

        // 6) Check logcat for error emission
        const logcatResult = await this.core.executeCommand('adb logcat -d');
        const logcatOutput = logcatResult.stdout || logcatResult.output || '';
        
        const hasErrorEmission = logcatOutput.includes('ErrorCenter: Emitting error:');
        const hasBannerHost = logcatOutput.includes('ErrorBannerHost: Received error:');
        const hasStructuredLog = logcatOutput.includes('PLUCT_ERR:');

        if (!hasErrorEmission) {
            this.core.logger.error('❌ ErrorCenter not emitting errors');
            return { success: false, error: 'ErrorCenter not emitting errors' };
        }
        this.core.logger.info('✅ ErrorCenter emitting errors');

        if (!hasBannerHost) {
            this.core.logger.error('❌ ErrorBannerHost not receiving errors');
            return { success: false, error: 'ErrorBannerHost not receiving errors' };
        }
        this.core.logger.info('✅ ErrorBannerHost receiving errors');

        if (!hasStructuredLog) {
            this.core.logger.error('❌ Structured logging not working');
            return { success: false, error: 'Structured logging not working' };
        }
        this.core.logger.info('✅ Structured logging working');

        // 7) Test error stacking
        this.core.logger.info('🔴 Testing error stacking...');
        await this.core.tapByText('Validation Error');
        await this.core.sleep(2000);

        const logcatResult2 = await this.core.executeCommand('adb logcat -d');
        const logcatOutput2 = logcatResult2.stdout || logcatResult2.output || '';
        const errorCount = (logcatOutput2.match(/ErrorBannerHost: Rendering with \d+ errors/g) || []).length;
        const hasMultipleErrors = logcatOutput2.includes('ErrorBannerHost: Rendering with 2 errors');

        if (hasMultipleErrors) {
            this.core.logger.info('✅ Error stacking working - multiple errors detected');
        } else {
            this.core.logger.warn('⚠️ Error stacking may not be working properly');
        }

        return { 
            success: true, 
            message: 'Error notification system validated successfully',
            details: {
                errorEmission: hasErrorEmission,
                bannerHost: hasBannerHost,
                structuredLogging: hasStructuredLog,
                errorStacking: hasMultipleErrors,
                totalErrorEvents: errorCount,
                uiBannerVisible: errorBannerVisible
            }
        };
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('ErrorNotificationValidation', new ErrorNotificationValidationJourney(orchestrator.core));
}

module.exports = { ErrorNotificationValidationJourney, register };