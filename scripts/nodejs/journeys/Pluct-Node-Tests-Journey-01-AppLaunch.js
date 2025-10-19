/**
 * Pluct Node Tests Journey 01 - App Launch
 * Tests enhanced app launch with UI verification
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */

const { execOk, execOut } = require('../core/Pluct-Test-Core-Exec');
const { logInfo, logWarn, logSuccess, logError } = require('../core/Logger');
const UI = require('../modules/Pluct-Node-Tests-UI-01Validator');

function sleep(ms) { Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms); }

function waitUntilFocused(timeoutMs) {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
        const win = execOut('adb shell dumpsys window windows');
        if (/mCurrentFocus.*app\.pluct/i.test(win) || /mFocusedApp.*app\.pluct/i.test(win)) return true;
        sleep(300);
    }
    return false;
}

async function testAppLaunch({ deviceId, artifacts, log }) {
    log.info("Testing enhanced app launch...", "Journey-01");
    
    // Capture UI state before launch
    const pre = UI.dumpHierarchy(deviceId, artifacts.ui, "AppLaunch-pre");
    const nodesPre = UI.parse(pre.xml);
    UI.logInventory(log, nodesPre, "AppLaunch-pre");
    
    // Launch the app
    const ok = execOk('adb shell am start -W -n app.pluct/.MainActivity');
    if (!ok) { 
        log.error('App launch failed', 'Journey-01'); 
        return false; 
    }
    
    if (!waitUntilFocused(5000)) { 
        log.warn('App window not focused within timeout; proceeding with caution', 'Journey-01'); 
    }
    
    // Capture UI state after launch
    const post = UI.dumpHierarchy(deviceId, artifacts.ui, "AppLaunch-post");
    const nodesPost = UI.parse(post.xml);
    UI.logInventory(log, nodesPost, "AppLaunch-post");
    
    // Validate that MainActivity components are present
    const hasMainActivity = nodesPost.some(node => 
        node.id.includes("MainActivity") || 
        node.desc.includes("MainActivity") ||
        node.text.includes("Pluct") ||
        node.text.includes("Summarize any TikTok") ||
        node.text.includes("Paste TikTok URL")
    );
    
    if (!hasMainActivity) {
        log.error('MainActivity components not found after launch', 'Journey-01');
        return false;
    }
    
    log.success('Enhanced app launch test passed', 'Journey-01');
    return true;
}

module.exports = {
    testAppLaunch,
    name: 'App Launch',
    description: 'Tests enhanced app launch with UI verification',
    order: 1
};
