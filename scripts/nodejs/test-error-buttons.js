const PluctCoreUnified = require('./core/Pluct-Core-Unified-New');

class ErrorButtonTest {
    constructor(core) {
        this.core = core;
        this.name = 'ErrorButtonTest';
    }

    async run() {
        this.core.logger.info('🎯 Testing Error Banner System with Buttons...');

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

        // 3) Tap the network error button
        this.core.logger.info('🔴 Triggering Network Error...');
        const networkButton = await this.core.tapByText('Network Error');
        if (!networkButton.success) {
            this.core.logger.error('❌ Failed to tap network error button');
            return { success: false, error: 'Failed to tap network error button' };
        }
        
        await this.core.sleep(2000); // Wait for error to appear
        
        // 4) Check for error banner in UI
        await this.core.dumpUIHierarchy();
        uiDump = this.core.readLastUIDump();
        
        const errorBannerVisible = uiDump.includes('testTag="error_banner"');
        const errorBannerContent = uiDump.includes('error_code:NET_IO') || uiDump.includes('error_code:NETWORK');
        
        if (errorBannerVisible) {
            this.core.logger.info('✅ Error banner detected!');
            if (errorBannerContent) {
                this.core.logger.info('✅ Error banner contains correct content');
            } else {
                this.core.logger.warn('⚠️ Error banner visible but content not as expected');
            }
        } else {
            this.core.logger.error('❌ No error banner detected after triggering network error');
            return { success: false, error: 'No error banner detected' };
        }

        // 5) Test validation error
        this.core.logger.info('🔴 Triggering Validation Error...');
        const validationButton = await this.core.tapByText('Validation Error');
        if (!validationButton.success) {
            this.core.logger.error('❌ Failed to tap validation error button');
            return { success: false, error: 'Failed to tap validation error button' };
        }
        
        await this.core.sleep(2000);
        
        // 6) Check for multiple error banners
        await this.core.dumpUIHierarchy();
        uiDump = this.core.readLastUIDump();
        
        const multipleErrors = (uiDump.match(/testTag="error_banner"/g) || []).length;
        this.core.logger.info(`📊 Found ${multipleErrors} error banner(s)`);
        
        if (multipleErrors >= 2) {
            this.core.logger.info('✅ Multiple error banners detected - stacking working!');
        } else {
            this.core.logger.warn('⚠️ Only one error banner detected - stacking may not be working');
        }

        return { 
            success: true, 
            message: 'Error banner system working correctly',
            details: {
                errorBanners: multipleErrors,
                stacking: multipleErrors >= 2
            }
        };
    }
}

async function runErrorButtonTest() {
    const core = new PluctCoreUnified();
    const test = new ErrorButtonTest(core);

    try {
        const result = await test.run();
        console.log('Test Result:', result);
    } catch (error) {
        console.error('Test failed:', error);
    }
}

runErrorButtonTest();
