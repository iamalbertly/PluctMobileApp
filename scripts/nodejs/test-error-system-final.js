const PluctCoreUnified = require('./core/Pluct-Core-Unified-New');

class ErrorSystemFinalTest {
    constructor(core) {
        this.core = core;
        this.name = 'ErrorSystemFinalTest';
    }

    async run() {
        this.core.logger.info('🎯 Testing Error System - Final Validation...');

        // 1) Launch the app
        await this.core.launchApp();
        await this.core.sleep(2000);

        // 2) Clear logcat to start fresh
        await this.core.executeCommand('adb logcat -c');
        this.core.logger.info('📱 Logcat cleared');

        // 3) Check if error test section is visible
        await this.core.dumpUIHierarchy();
        let uiDump = this.core.readLastUIDump();
        
        if (!uiDump.includes('Error Banner Test')) {
            this.core.logger.error('❌ Error test section not found');
            return { success: false, error: 'Error test section not found' };
        }
        this.core.logger.info('✅ Error test section found');

        // 4) Trigger network error and check logcat
        this.core.logger.info('🔴 Triggering Network Error...');
        await this.core.tapByText('Network Error');
        await this.core.sleep(2000);

        // Check logcat for error emission
        const logcatResult = await this.core.executeCommand('adb logcat -d');
        const logcatOutput = logcatResult.stdout || logcatResult.output || '';
        const hasErrorEmission = logcatOutput.includes('ErrorCenter: Emitting error: NET_IO');
        const hasBannerHost = logcatOutput.includes('ErrorBannerHost: Received error: NET_IO');
        const hasStructuredLog = logcatOutput.includes('PLUCT_ERR: {"type":"ui_error","code":"NET_IO"');

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

        // 5) Test validation error for stacking
        this.core.logger.info('🔴 Triggering Validation Error for stacking test...');
        await this.core.tapByText('Validation Error');
        await this.core.sleep(2000);

        // Check for multiple errors in logcat
        const logcatResult2 = await this.core.executeCommand('adb logcat -d');
        const logcatOutput2 = logcatResult2.stdout || logcatResult2.output || '';
        const errorCount = (logcatOutput2.match(/ErrorBannerHost: Rendering with \d+ errors/g) || []).length;
        const hasMultipleErrors = logcatOutput2.includes('ErrorBannerHost: Rendering with 2 errors');

        if (!hasMultipleErrors) {
            this.core.logger.warn('⚠️ Error stacking may not be working properly');
        } else {
            this.core.logger.info('✅ Error stacking working - multiple errors detected');
        }

        // 6) Test API error
        this.core.logger.info('🔴 Triggering API Error...');
        await this.core.tapByText('API Error');
        await this.core.sleep(2000);

        const logcatResult3 = await this.core.executeCommand('adb logcat -d');
        const logcatOutput3 = logcatResult3.stdout || logcatResult3.output || '';
        const hasApiError = logcatOutput3.includes('ErrorCenter: Emitting error: API_ERROR');
        const hasApiBanner = logcatOutput3.includes('ErrorBannerHost: Received error: API_ERROR');

        if (!hasApiError || !hasApiBanner) {
            this.core.logger.error('❌ API error not working');
            return { success: false, error: 'API error not working' };
        }
        this.core.logger.info('✅ API error working');

        return { 
            success: true, 
            message: 'Error system working correctly',
            details: {
                errorEmission: hasErrorEmission,
                bannerHost: hasBannerHost,
                structuredLogging: hasStructuredLog,
                errorStacking: hasMultipleErrors,
                apiError: hasApiError,
                totalErrorEvents: errorCount
            }
        };
    }
}

async function runErrorSystemFinalTest() {
    const core = new PluctCoreUnified();
    const test = new ErrorSystemFinalTest(core);

    try {
        const result = await test.run();
        console.log('Test Result:', result);
    } catch (error) {
        console.error('Test failed:', error);
    }
}

runErrorSystemFinalTest();
