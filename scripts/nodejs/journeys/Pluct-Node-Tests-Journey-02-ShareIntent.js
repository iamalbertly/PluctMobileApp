/**
 * Pluct Node Tests Journey 02 - Share Intent
 * Tests enhanced share intent handling with UI verification
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */

const { execOk, execOut } = require('../core/Pluct-Test-Core-Exec');
const { logInfo, logWarn, logSuccess, logError } = require('../core/Logger');
const UI = require('../modules/UIValidator');

function sleep(ms) { Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms); }

async function testShareIntent({ deviceId, artifacts, log, url }) {
    log.info('Testing enhanced share intent handling...', 'Journey-02');
    
    // Capture UI state before share intent
    const pre = UI.dumpHierarchy(deviceId, artifacts.ui, "ShareIntent-pre");
    const nodesPre = UI.parse(pre.xml);
    UI.logInventory(log, nodesPre, "ShareIntent-pre");
    
    // Send share intent with TikTok URL
    const cmd = `adb shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "${url}" -n app.pluct/.share.PluctShareIngestActivity`;
    log.info(`ADB: ${cmd}`, 'Journey-02');
    
    const ok = execOk(cmd);
    if (!ok) { 
        log.error('Share intent failed', 'Journey-02'); 
        return false; 
    }
    
    // Wait for intent to be handled
    for (let i = 0; i < 8; i++) { 
        const top = execOut('adb shell dumpsys activity activities | findstr app.pluct');
        if (top && top.trim()) break;
        sleep(1000);
    }
    
    // Wait for "Capture This Insight" to be visible
    const waitForElement = (text, timeoutSeconds) => {
        const deadline = Date.now() + (timeoutSeconds * 1000);
        while (Date.now() < deadline) {
            const dump = execOut('adb shell uiautomator dump /sdcard/uidump.xml && adb shell cat /sdcard/uidump.xml');
            if (dump && dump.includes(text)) return true;
            sleep(500);
        }
        return false;
    };
    
    const handled = waitForElement('Capture This Insight', 10);
    if (!handled) {
        log.error('Intent not handled properly - Capture This Insight not found', 'Journey-02');
        return false;
    }
    
    // Capture UI state after share intent
    const post = UI.dumpHierarchy(deviceId, artifacts.ui, "ShareIntent-post");
    const nodesPost = UI.parse(post.xml);
    UI.logInventory(log, nodesPost, "ShareIntent-post");
    
    // Validate that Capture This Insight is visible
    const hasCaptureSheet = nodesPost.some(node => 
        node.text.includes("Capture This Insight") ||
        node.desc.includes("Capture This Insight")
    );
    
    if (!hasCaptureSheet) {
        log.error('Capture This Insight not found after share intent', 'Journey-02');
        return false;
    }
    
    log.success('Enhanced share intent test passed', 'Journey-02');
    return true;
}

module.exports = {
    testShareIntent,
    name: 'Share Intent',
    description: 'Tests enhanced share intent handling with UI verification',
    order: 2
};
