const PluctCoreUnified = require('./scripts/nodejs/core/Pluct-Core-Unified-New');

async function testDebugButton() {
    const core = new PluctCoreUnified();
    
    console.log('Launching app...');
    await core.launchApp();
    
    console.log('Looking for debug error button...');
    await core.dumpUIHierarchy();
    const uiDump = core.readLastUIDump();
    
    if (uiDump.includes('debug_error_button')) {
        console.log('✅ Debug error button found!');
        
        console.log('Tapping debug error button...');
        const tapResult = await core.tapByTestTag('debug_error_button');
        console.log('Tap result:', tapResult);
        
        await core.sleep(2000);
        
        console.log('Checking UI for error banner...');
        await core.dumpUIHierarchy();
        const finalUiDump = core.readLastUIDump();
        
        if (finalUiDump.includes('testTag="error_banner"')) {
            console.log('✅ Error banner found!');
            if (finalUiDump.includes('content-desc="error_code:VALIDATION_ERROR"') || finalUiDump.includes('contentDescription="error_code:VALIDATION_ERROR"')) {
                console.log('✅ Error code VALIDATION_ERROR found in banner!');
            } else {
                console.log('❌ Error code VALIDATION_ERROR not found in banner');
            }
        } else {
            console.log('❌ Error banner not found');
        }
        
        console.log('Checking logs...');
        const logs = await core.readLogcatSince(Date.now() - 10000, 'PLUCT_ERR');
        console.log('PLUCT_ERR logs:', logs);
        
        const hasValidation = logs.some(l => l.includes('"type":"ui_error"') && l.includes('"code":"VALIDATION_ERROR"'));
        if (hasValidation) {
            console.log('✅ Structured log for VALIDATION_ERROR found!');
        } else {
            console.log('❌ Structured log for VALIDATION_ERROR not found');
        }
    } else {
        console.log('❌ Debug error button not found');
    }
}

testDebugButton().catch(console.error);
