/**
 * Pluct Node Tests Journey 03 - Quick Scan
 * Tests enhanced Quick Scan interaction with UI verification and HTTP signals
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */

const { logInfo, logWarn, logSuccess, logError } = require('../core/Logger');
const { execOut } = require('../core/Pluct-Test-Core-Exec');
const UI = require('../modules/UIValidator');

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
            want: { by: "desc", q: "quick_scan" }, // Use content description for reliable targeting
            log,
            waitMs: 2000,
            deltaMin: 0
        });
        
                    // Require BOTH UI delta AND work signals for Quick Scan
                    const uiDelta = Math.abs(after.length - before.length) >= 1;
                    const uiClick = await waitForPattern(/QUICK_SCAN_UI_CLICK/, 3);
                    const enqueuing = await waitForPattern(/ENQUEUING_TRANSCRIPTION_WORK/, 3);
                    const workerStart = await waitForPattern(/WORKER_START/, 3);
                    const quickScanStart = await waitForPattern(/QUICK_SCAN_START/, 5);
                    const quickScanComplete = await waitForPattern(/QUICK_SCAN_COMPLETE/, 5);
                    const httpOut = await waitForPattern(/PLUCT_HTTP>OUT/, 3);
                    const httpIn = await waitForPattern(/PLUCT_HTTP>IN/, 3);
                    
                    // Quick Scan passes only if:
                    // 1. UI delta > 0 OR a visible progress/snackbar/test tag appears, AND
                    // 2. At least one PLUCT_HTTP>OUT/IN pair (balance or ttt) appears in logcat
                    const hasUiChange = uiDelta || await waitForPattern(/status_pill|progress|snackbar/, 2);
                    const hasHttpActivity = httpOut && httpIn;
                    const hasWorkSignals = uiClick || enqueuing || workerStart || quickScanStart || quickScanComplete;
                    
                    if (!hasUiChange || !hasHttpActivity || !hasWorkSignals) {
                        // Save failure artifacts
                        const failureLog = execOut('adb logcat -d | tail -n 100');
                        require('fs').writeFileSync(`${artifacts.logs}/fail-quickscan-${Date.now()}.log`, failureLog);
                        throw new Error(`Quick Scan failed: uiChange=${hasUiChange} httpActivity=${hasHttpActivity} workSignals=${hasWorkSignals}`);
                    }
        
        log.success(`[QuickScan] passed via ${target.id || target.desc || target.text} - UI delta: ${uiDelta}, tier selected: ${tierSelected}, UI click: ${uiClick}, scan start: ${quickScanStart}, scan complete: ${quickScanComplete}`, 'Journey-03');
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
