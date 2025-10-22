const PluctCoreUnified = require('./core/Pluct-Core-Unified-New');

class AppFunctionalityTest {
    constructor(core) {
        this.core = core;
        this.name = 'AppFunctionalityTest';
    }

    async run() {
        this.core.logger.info('🎯 Testing App Functionality...');

        // 1) Launch the app
        await this.core.launchApp();
        await this.core.sleep(2000);

        // 2) Check if app launched successfully
        await this.core.dumpUIHierarchy();
        const uiDump = this.core.readLastUIDump();
        
        if (!uiDump.includes('app.pluct')) {
            this.core.logger.error('❌ App not launched successfully');
            return { success: false, error: 'App not launched' };
        }
        this.core.logger.info('✅ App launched successfully');

        // 3) Check for home screen elements
        const hasHomeScreen = uiDump.includes('No transcripts yet') || uiDump.includes('Pluct');
        if (!hasHomeScreen) {
            this.core.logger.error('❌ Home screen not detected');
            return { success: false, error: 'Home screen not detected' };
        }
        this.core.logger.info('✅ Home screen detected');

        // 4) Check for error test section
        const hasErrorTest = uiDump.includes('Error Banner Test');
        if (!hasErrorTest) {
            this.core.logger.error('❌ Error test section not found');
            return { success: false, error: 'Error test section not found' };
        }
        this.core.logger.info('✅ Error test section found');

        // 5) Test error system
        this.core.logger.info('🔴 Testing error system...');
        await this.core.tapByText('Network Error');
        await this.core.sleep(2000);

        // Check logcat for error emission
        const logcatResult = await this.core.executeCommand('adb logcat -d');
        const logcatOutput = logcatResult.stdout || logcatResult.output || '';
        const hasErrorEmission = logcatOutput.includes('ErrorCenter: Emitting error: NET_IO');
        const hasBannerHost = logcatOutput.includes('ErrorBannerHost: Received error: NET_IO');

        if (!hasErrorEmission || !hasBannerHost) {
            this.core.logger.error('❌ Error system not working');
            return { success: false, error: 'Error system not working' };
        }
        this.core.logger.info('✅ Error system working');

        // 6) Test FAB (Floating Action Button)
        this.core.logger.info('🔴 Testing FAB...');
        const fabResult = await this.core.tapByContentDesc('capture_fab');
        if (!fabResult.success) {
            this.core.logger.warn('⚠️ FAB not found or not clickable');
        } else {
            this.core.logger.info('✅ FAB working');
            await this.core.sleep(2000);
        }

        return { 
            success: true, 
            message: 'App functionality working correctly',
            details: {
                appLaunch: true,
                homeScreen: hasHomeScreen,
                errorSystem: hasErrorEmission && hasBannerHost,
                fabWorking: fabResult.success
            }
        };
    }
}

async function runAppFunctionalityTest() {
    const core = new PluctCoreUnified();
    const test = new AppFunctionalityTest(core);

    try {
        const result = await test.run();
        console.log('Test Result:', result);
    } catch (error) {
        console.error('Test failed:', error);
    }
}

runAppFunctionalityTest();
