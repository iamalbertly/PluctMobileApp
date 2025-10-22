const PluctCoreUnified = require('./core/Pluct-Core-Unified-New');

class ErrorBannerTest {
    constructor(core) {
        this.core = core;
        this.name = 'ErrorBannerTest';
    }

    async run() {
        this.core.logger.info('🎯 Testing Error Banner System...');

        // 1) Launch the app
        await this.core.launchApp();
        await this.core.sleep(2000);

        // 2) Trigger debug deep link for error
        const debugErrorUrl = 'pluct://debug/error?code=TEST_ERROR&msg=This+is+a+test+error+banner';
        await this.core.executeCommand(`adb shell am start -a android.intent.action.VIEW -d "${debugErrorUrl}"`);
        await this.core.sleep(3000); // Give app time to process deep link

        // 5) Dump UI hierarchy and check for error banner
        await this.core.dumpUIHierarchy();
        const uiDump = this.core.readLastUIDump();

        const errorBannerVisible = uiDump.includes('testTag="error_banner"');
        const errorBannerContent = uiDump.includes('error_code:TEST_ERROR');

        if (errorBannerVisible) {
            this.core.logger.info('✅ Error banner detected!');
            if (errorBannerContent) {
                this.core.logger.info('✅ Error banner contains correct content');
                return { success: true, message: 'Error banner system working correctly' };
            } else {
                this.core.logger.warn('⚠️ Error banner visible but content not as expected');
                return { success: false, error: 'Error banner content mismatch' };
            }
        } else {
            this.core.logger.error('❌ No error banner detected');
            return { success: false, error: 'Error banner not visible' };
        }
    }
}

async function runErrorBannerTest() {
    const core = new PluctCoreUnified();
    const test = new ErrorBannerTest(core);

    try {
        const result = await test.run();
        console.log('Test Result:', result);
    } catch (error) {
        console.error('Test failed:', error);
    }
}

runErrorBannerTest();
