const PluctCoreUnified = require('./scripts/nodejs/core/Pluct-Core-Unified-New');

async function testDebugDeepLink() {
    const core = new PluctCoreUnified();
    
    console.log('Launching app...');
    await core.launchApp();
    
    console.log('Triggering debug deep link...');
    const result = await core.executeCommand('adb shell am start -a android.intent.action.VIEW -d "pluct://debug/error?code=AUTH_401&msg=Bad%20token"');
    console.log('Deep link result:', result);
    
    await core.sleep(2000);
    
    console.log('Checking UI for error banner...');
    await core.dumpUIHierarchy();
    const uiDump = core.readLastUIDump();
    
    if (uiDump.includes('testTag="error_banner"')) {
        console.log('✅ Error banner found!');
        if (uiDump.includes('content-desc="error_code:AUTH_401"') || uiDump.includes('contentDescription="error_code:AUTH_401"')) {
            console.log('✅ Error code AUTH_401 found in banner!');
        } else {
            console.log('❌ Error code AUTH_401 not found in banner');
        }
    } else {
        console.log('❌ Error banner not found');
    }
    
    console.log('Checking logs...');
    const logs = await core.readLogcatSince(Date.now() - 10000, 'PLUCT_ERR');
    console.log('PLUCT_ERR logs:', logs);
    
    const has401 = logs.some(l => l.includes('"type":"ui_error"') && l.includes('"code":"AUTH_401"'));
    if (has401) {
        console.log('✅ Structured log for AUTH_401 found!');
    } else {
        console.log('❌ Structured log for AUTH_401 not found');
    }
}

testDebugDeepLink().catch(console.error);
