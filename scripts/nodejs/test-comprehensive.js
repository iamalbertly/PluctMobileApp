const PluctCoreUnified = require('./core/Pluct-Core-Unified-New');

class ComprehensiveTest {
    constructor(core) {
        this.core = core;
        this.name = 'ComprehensiveTest';
    }

    async run() {
        this.core.logger.info('🎯 Running Comprehensive Test Suite...');

        const results = [];

        // Test 1: App Launch
        this.core.logger.info('📱 Testing App Launch...');
        await this.core.launchApp();
        await this.core.sleep(2000);
        
        await this.core.dumpUIHierarchy();
        const uiDump = this.core.readLastUIDump();
        const appLaunchSuccess = uiDump.includes('app.pluct');
        results.push({ test: 'AppLaunch', success: appLaunchSuccess });
        this.core.logger.info(appLaunchSuccess ? '✅ App Launch: PASS' : '❌ App Launch: FAIL');

        // Test 2: Home Screen
        this.core.logger.info('🏠 Testing Home Screen...');
        const homeScreenSuccess = uiDump.includes('No transcripts yet') || uiDump.includes('Pluct');
        results.push({ test: 'HomeScreen', success: homeScreenSuccess });
        this.core.logger.info(homeScreenSuccess ? '✅ Home Screen: PASS' : '❌ Home Screen: FAIL');

        // Test 3: Error System
        this.core.logger.info('🔴 Testing Error System...');
        const errorSystemSuccess = uiDump.includes('Error Banner Test');
        if (errorSystemSuccess) {
            await this.core.tapByText('Network Error');
            await this.core.sleep(2000);
            
            const logcatResult = await this.core.executeCommand('adb logcat -d');
            const logcatOutput = logcatResult.stdout || logcatResult.output || '';
            const errorEmissionSuccess = logcatOutput.includes('ErrorCenter: Emitting error:') && logcatOutput.includes('ErrorBannerHost: Received error:');
            results.push({ test: 'ErrorSystem', success: errorEmissionSuccess });
            this.core.logger.info(errorEmissionSuccess ? '✅ Error System: PASS' : '❌ Error System: FAIL');
        } else {
            results.push({ test: 'ErrorSystem', success: false });
            this.core.logger.info('❌ Error System: FAIL - Test section not found');
        }

        // Test 4: FAB (Floating Action Button)
        this.core.logger.info('🔘 Testing FAB...');
        const fabResult = await this.core.tapByContentDesc('capture_fab');
        results.push({ test: 'FAB', success: fabResult.success });
        this.core.logger.info(fabResult.success ? '✅ FAB: PASS' : '❌ FAB: FAIL');

        // Test 5: Manual URL Input
        this.core.logger.info('📝 Testing Manual URL Input...');
        if (fabResult.success) {
            await this.core.sleep(2000);
            await this.core.dumpUIHierarchy();
            const captureSheetDump = this.core.readLastUIDump();
            const captureSheetSuccess = captureSheetDump.includes('TikTok URL') || captureSheetDump.includes('url_input');
            results.push({ test: 'ManualURLInput', success: captureSheetSuccess });
            this.core.logger.info(captureSheetSuccess ? '✅ Manual URL Input: PASS' : '❌ Manual URL Input: FAIL');
        } else {
            results.push({ test: 'ManualURLInput', success: false });
            this.core.logger.info('❌ Manual URL Input: FAIL - FAB not working');
        }

        // Test 6: Quick Scan
        this.core.logger.info('⚡ Testing Quick Scan...');
        if (results.find(r => r.test === 'ManualURLInput').success) {
            const quickScanResult = await this.core.tapByText('Quick Scan');
            results.push({ test: 'QuickScan', success: quickScanResult.success });
            this.core.logger.info(quickScanResult.success ? '✅ Quick Scan: PASS' : '❌ Quick Scan: FAIL');
        } else {
            results.push({ test: 'QuickScan', success: false });
            this.core.logger.info('❌ Quick Scan: FAIL - Manual URL Input not working');
        }

        // Summary
        const totalTests = results.length;
        const passedTests = results.filter(r => r.success).length;
        const success = passedTests === totalTests;

        this.core.logger.info(`📊 Test Summary: ${passedTests}/${totalTests} tests passed`);
        
        if (success) {
            this.core.logger.info('🎉 ALL TESTS PASSED!');
        } else {
            this.core.logger.error('❌ Some tests failed');
        }

        return {
            success,
            message: success ? 'All tests passed' : 'Some tests failed',
            details: {
                totalTests,
                passedTests,
                results
            }
        };
    }
}

async function runComprehensiveTest() {
    const core = new PluctCoreUnified();
    const test = new ComprehensiveTest(core);

    try {
        const result = await test.run();
        console.log('Test Result:', result);
    } catch (error) {
        console.error('Test failed:', error);
    }
}

runComprehensiveTest();
