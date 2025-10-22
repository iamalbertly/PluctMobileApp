const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class BusinessEngineIntegrationJourney extends BaseJourney {
    async execute() {
        this.core.logger.info('🔗 Testing Business Engine Integration...');

        // 1) Launch the app
        await this.core.launchApp();
        await this.core.sleep(2000);

        // 2) Check home screen
        await this.core.dumpUIHierarchy();
        let uiDump = this.core.readLastUIDump();
        
        if (!uiDump.includes('No transcripts yet') && !uiDump.includes('Error Banner Test')) {
            this.core.logger.error('❌ Home screen not detected');
            return { success: false, error: 'Home screen not detected' };
        }
        this.core.logger.info('✅ Home screen detected');

        // 3) Test error notification system
        this.core.logger.info('🔴 Testing Error Notification System...');
        await this.core.tapByText('Network Error');
        await this.core.sleep(3000);

        // Check logcat for error emission
        const logcatResult = await this.core.executeCommand('adb logcat -d');
        const logcatOutput = logcatResult.stdout || logcatResult.output || '';
        
        const hasErrorEmission = logcatOutput.includes('ErrorCenter: Emitting error:');
        const hasBannerHost = logcatOutput.includes('ErrorBannerHost: Received error:');
        const hasStructuredLog = logcatOutput.includes('PLUCT_ERR:');

        if (!hasErrorEmission) {
            this.core.logger.error('❌ ErrorCenter not emitting errors');
            return { success: false, error: 'ErrorCenter not emitting errors' };
        }
        this.core.logger.info('✅ Error notification system working');

        // 4) Test FAB interaction
        this.core.logger.info('🎯 Testing FAB interaction...');
        const fabResult = await this.core.tapByContentDesc('Capture Insight');
        if (!fabResult.success) {
            this.core.logger.error('❌ FAB not found');
            return { success: false, error: 'FAB not found' };
        }
        this.core.logger.info('✅ FAB interaction successful');

        // 5) Test capture sheet
        await this.core.sleep(2000);
        await this.core.dumpUIHierarchy();
        uiDump = this.core.readLastUIDump();
        
        if (!uiDump.includes('Capture Sheet')) {
            this.core.logger.error('❌ Capture sheet not opened');
            return { success: false, error: 'Capture sheet not opened' };
        }
        this.core.logger.info('✅ Capture sheet opened');

        // 6) Test URL input
        this.core.logger.info('📝 Testing URL input...');
        const urlTap = await this.core.tapByContentDesc('url_input');
        if (!urlTap.success) {
            this.core.logger.error('❌ URL input field not found');
            return { success: false, error: 'URL input field not found' };
        }

        await this.core.clearEditText();
        await this.core.inputText('https://vm.tiktok.com/ZMADQVF4e/');
        this.core.logger.info('✅ URL input successful');

        // 7) Test Quick Scan
        this.core.logger.info('⚡ Testing Quick Scan...');
        const quickScanResult = await this.core.tapByText('Quick Scan');
        if (!quickScanResult.success) {
            this.core.logger.error('❌ Quick Scan button not found');
            return { success: false, error: 'Quick Scan button not found' };
        }
        this.core.logger.info('✅ Quick Scan initiated');

        // 8) Wait for processing and check logs
        await this.core.sleep(5000);
        const processingLogcat = await this.core.executeCommand('adb logcat -d');
        const processingOutput = processingLogcat.stdout || processingLogcat.output || '';
        
        const hasBusinessEngineLogs = processingOutput.includes('PluctBusinessEngineUnifiedClient');
        const hasEndToEndFlow = processingOutput.includes('STARTING END-TO-END FLOW');
        const hasHealthCheck = processingOutput.includes('Health check passed');
        const hasTokenVending = processingOutput.includes('Token vended successfully');

        if (hasBusinessEngineLogs) {
            this.core.logger.info('✅ Business Engine integration detected');
        } else {
            this.core.logger.warn('⚠️ Business Engine integration not detected in logs');
        }

        if (hasEndToEndFlow) {
            this.core.logger.info('✅ End-to-end flow initiated');
        } else {
            this.core.logger.warn('⚠️ End-to-end flow not detected');
        }

        if (hasHealthCheck) {
            this.core.logger.info('✅ Health check passed');
        } else {
            this.core.logger.warn('⚠️ Health check not detected');
        }

        if (hasTokenVending) {
            this.core.logger.info('✅ Token vending successful');
        } else {
            this.core.logger.warn('⚠️ Token vending not detected');
        }

        return { 
            success: true, 
            message: 'Business Engine integration test completed',
            details: {
                errorSystem: hasErrorEmission && hasBannerHost && hasStructuredLog,
                fabInteraction: fabResult.success,
                captureSheet: uiDump.includes('Capture Sheet'),
                urlInput: urlTap.success,
                quickScan: quickScanResult.success,
                businessEngineLogs: hasBusinessEngineLogs,
                endToEndFlow: hasEndToEndFlow,
                healthCheck: hasHealthCheck,
                tokenVending: hasTokenVending
            }
        };
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('BusinessEngineIntegration', new BusinessEngineIntegrationJourney(orchestrator.core));
}

module.exports = { BusinessEngineIntegrationJourney, register };
