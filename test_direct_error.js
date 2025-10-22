const PluctCoreUnified = require('./scripts/nodejs/core/Pluct-Core-Unified-New');

async function testDirectError() {
    const core = new PluctCoreUnified();
    
    console.log('Launching app...');
    await core.launchApp();
    
    console.log('Triggering error via broadcast...');
    const result = await core.executeCommand('adb shell am broadcast -a app.pluct.action.TRIGGER_ERROR --es error_type VALIDATION');
    console.log('Broadcast result:', result);
    
    await core.sleep(2000);
    
    console.log('Checking UI for error banner...');
    await core.dumpUIHierarchy();
    const uiDump = core.readLastUIDump();
    
    if (uiDump.includes('testTag="error_banner"')) {
        console.log('✅ Error banner found!');
        if (uiDump.includes('content-desc="error_code:VALIDATION_ERROR"') || uiDump.includes('contentDescription="error_code:VALIDATION_ERROR"')) {
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
}

testDirectError().catch(console.error);
