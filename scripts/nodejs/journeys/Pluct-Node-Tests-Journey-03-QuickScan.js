/**
 * Pluct Node Tests Journey 03 - Quick Scan
 * Tests enhanced Quick Scan interaction with UI verification and HTTP signals
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */

const { logInfo, logWarn, logSuccess, logError } = require('../core/Logger');
const { execOut } = require('../core/Pluct-Test-Core-Exec');
const UI = require('../modules/Pluct-Node-Tests-UI-AndroidValidatorAndInteractor');

function sleep(ms) { Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms); }

async function waitForPattern(pattern, timeoutSec = 8) {
    const re = new RegExp(pattern, 'i');
    const deadline = Date.now() + timeoutSec * 1000;
    
    while (Date.now() < deadline) {
        const out = execOut('adb logcat -d | tail -n 200');
        if (re.test(out)) {
            return true;
        }
        sleep(500);
    }
    return false;
}

async function testQuickScan({ deviceId, artifacts, log }) {
    log.info('Testing enhanced Quick Scan interaction...', 'Journey-03');
    
    try {
        // Click Quick Scan and verify UI change
        const { before, after, target } = await UI.clickAndVerify({
            deviceId,
            artifactsDir: artifacts.ui,
            step: "QuickScan",
            want: { by: "text", q: "Quick Scan" },
            log,
            waitMs: 2000,
            deltaMin: 0
        });
        
        // Require truthy signals (any ONE is enough)
        const uiDelta = Math.abs(after.length - before.length) >= 1;
        const tierSelected = await waitForPattern(/tier_selected.*QUICK_SCAN/, 3);
        const uiClick = await waitForPattern(/ui_click.*Quick Scan/, 3);
        const httpVend = await waitForPattern(/PLUCT_HTTP.*vend-token/, 5);
        
        if (!(uiDelta || tierSelected || uiClick || httpVend)) {
            // Save failure artifacts
            const failureLog = execOut('adb logcat -d | tail -n 100');
            require('fs').writeFileSync(`${artifacts.logs}/fail-quickscan-${Date.now()}.log`, failureLog);
            throw new Error("Quick Scan click did not change UI and no HTTP signals observed");
        }
        
        log.success(`[QuickScan] passed via ${target.id || target.desc || target.text} - UI delta: ${uiDelta}, tier selected: ${tierSelected}, HTTP vend: ${httpVend}`, 'Journey-03');
        return true;
    } catch (e) {
        log.error(`Enhanced Quick Scan test failed: ${e.message}`, 'Journey-03');
        return false;
    }
}

module.exports = {
    testQuickScan,
    name: 'Quick Scan',
    description: 'Tests enhanced Quick Scan interaction with UI verification',
    order: 3
};
