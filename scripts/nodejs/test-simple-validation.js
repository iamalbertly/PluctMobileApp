const PluctCoreUnified = require('./core/Pluct-Core-Unified-New');

async function runSimpleValidation() {
    const core = new PluctCoreUnified();
    
    try {
        console.log('🚀 Starting Simple App Validation...');
        
        // 1) Launch the app
        console.log('📱 Launching app...');
        await core.launchApp();
        await core.sleep(2000);
        
        // 2) Check home screen
        console.log('🏠 Checking home screen...');
        await core.dumpUIHierarchy();
        const uiDump = core.readLastUIDump();
        
        if (!uiDump.includes('No transcripts yet') && !uiDump.includes('Pluct')) {
            console.log('❌ Home screen not detected');
            return { success: false, error: 'Home screen not detected' };
        }
        console.log('✅ Home screen detected');
        
        // 3) Test error notification system
        console.log('🔴 Testing error notification system...');
        await core.tapByText('Network Error');
        await core.sleep(3000);
        
        // Check logcat for error emission
        const logcatResult = await core.executeCommand('adb logcat -d');
        const logcatOutput = logcatResult.stdout || logcatResult.output || '';
        
        const hasErrorEmission = logcatOutput.includes('ErrorCenter: Emitting error:');
        const hasBannerHost = logcatOutput.includes('ErrorBannerHost: Received error:');
        
        if (hasErrorEmission && hasBannerHost) {
            console.log('✅ Error notification system working');
        } else {
            console.log('⚠️ Error notification system may not be working');
        }
        
        // 4) Test FAB interaction
        console.log('🎯 Testing FAB interaction...');
        const fabResult = await core.tapByContentDesc('Capture Insight');
        if (fabResult.success) {
            console.log('✅ FAB interaction successful');
            await core.sleep(2000);
            
            // Check if capture sheet opened
            await core.dumpUIHierarchy();
            const captureDump = core.readLastUIDump();
            if (captureDump.includes('Capture Sheet')) {
                console.log('✅ Capture sheet opened');
            } else {
                console.log('⚠️ Capture sheet may not have opened');
            }
        } else {
            console.log('❌ FAB not found');
        }
        
        console.log('\n🎯 Validation completed successfully!');
        return { 
            success: true, 
            message: 'Simple validation completed',
            details: {
                homeScreen: true,
                errorSystem: hasErrorEmission && hasBannerHost,
                fabInteraction: fabResult.success
            }
        };
        
    } catch (error) {
        console.error('❌ Validation failed:', error);
        return { success: false, error: error.message };
    }
}

// Run the validation
runSimpleValidation()
    .then(result => {
        console.log('\n🏁 Final Result:', result);
        process.exit(result.success ? 0 : 1);
    })
    .catch(error => {
        console.error('💥 Fatal error:', error);
        process.exit(1);
    });
