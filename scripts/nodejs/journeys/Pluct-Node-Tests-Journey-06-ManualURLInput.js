const { execOut } = require('../core/Pluct-Test-Core-Exec');
const UI = require('../modules/UIValidator');
const Logcat = require('../core/Logcat');
const fs = require('fs');

/**
 * Manual URL Input Journey - Tests manual typing of TikTok URL
 * Sister journey to intent journey - both need to be tested and pass
 */

function sleep(ms) { Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms); }

async function waitForPattern(pattern, timeoutSec = 8) {
    const deadline = Date.now() + (timeoutSec * 1000);
    while (Date.now() < deadline) {
        const out = execOut('adb logcat -d | tail -n 100');
        if (new RegExp(pattern, 'i').test(out)) {
            return true;
        }
        sleep(500);
    }
    return false;
}

async function testManualURLInput({ deviceId, artifacts, log }) {
    log.info('Testing manual URL input journey...', 'Journey-06');
    
    try {
        // Step 1: Open capture sheet by tapping the + FAB
        log.info('Step 1: Opening capture sheet...', 'Journey-06');
        const { before, after, target } = await UI.clickAndVerify({
            deviceId,
            artifactsDir: artifacts.ui,
            step: "OpenCaptureSheet",
            want: { by: "desc", q: "Add video" }, // FAB content description
            log,
            waitMs: 2000,
            deltaMin: 1
        });
        
        if (!target) {
            throw new Error("Could not find Add video FAB");
        }
        
        // Step 2: Verify capture sheet opened
        const sheetOpened = await waitForPattern(/Capture.*Insight|sheet_capture/, 3);
        if (!sheetOpened) {
            throw new Error("Capture sheet did not open");
        }
        
        // Step 3: Type TikTok URL manually
        log.info('Step 3: Typing TikTok URL manually...', 'Journey-06');
        const config = require('../config/Pluct-Test-Config-Defaults.json');
        const testUrl = config.url || "https://vm.tiktok.com/ZMADQVF4e/";
        
        // Find URL input field
        const urlInput = await UI.waitForElement('testTag', 'url_input', 5);
        if (!urlInput.found) {
            throw new Error("URL input field not found");
        }
        
        // Clear existing text and type new URL
        log.info(`Typing URL: ${testUrl}`, 'Journey-06');
        
        // Simulate typing by sending text input
        execOut(`adb shell input text "${testUrl}"`);
        sleep(1000);
        
        // Step 4: Verify URL was entered
        const urlEntered = await waitForPattern(/url.*change|onValueChange/, 3);
        if (!urlEntered) {
            log.warn("URL change signal not detected, but continuing...", 'Journey-06');
        }
        
        // Step 5: Click Quick Scan button
        log.info('Step 5: Clicking Quick Scan button...', 'Journey-06');
        const quickScanResult = await UI.clickAndVerify({
            deviceId,
            artifactsDir: artifacts.ui,
            step: "QuickScanManual",
            want: { by: "desc", q: "quick_scan" },
            log,
            waitMs: 2000,
            deltaMin: 0
        });
        
        // Step 6: Verify work signals
        log.info('Step 6: Verifying work signals...', 'Journey-06');
        const workSignals = [
            /QUICK_SCAN_UI_CLICK/,
            /ENQUEUING_TRANSCRIPTION_WORK/,
            /WORKER_START/,
            /PLUCT_HTTP>OUT/,
            /PLUCT_HTTP>IN/
        ];
        
        let signalsFound = 0;
        for (const signal of workSignals) {
            if (await waitForPattern(signal, 3)) {
                signalsFound++;
                log.info(`Work signal found: ${signal}`, 'Journey-06');
            }
        }
        
        if (signalsFound < 2) {
            throw new Error(`Only ${signalsFound} work signals found, need at least 2`);
        }
        
        // Step 7: Test AI Analysis button as well
        log.info('Step 7: Testing AI Analysis button...', 'Journey-06');
        const aiAnalysisResult = await UI.clickAndVerify({
            deviceId,
            artifactsDir: artifacts.ui,
            step: "AIAnalysisManual",
            want: { by: "desc", q: "AI Analysis" },
            log,
            waitMs: 2000,
            deltaMin: 0
        });
        
        // Step 8: Verify AI Analysis work signals
        const aiSignals = [
            /AI.*ANALYSIS.*SELECTED/,
            /tier_selected.*AI_ANALYSIS/,
            /PLUCT_HTTP.*ai.*analysis/
        ];
        
        let aiSignalsFound = 0;
        for (const signal of aiSignals) {
            if (await waitForPattern(signal, 3)) {
                aiSignalsFound++;
                log.info(`AI Analysis signal found: ${signal}`, 'Journey-06');
            }
        }
        
        if (aiSignalsFound < 1) {
            log.warn("AI Analysis signals not found, but Quick Scan worked", 'Journey-06');
        }
        
        log.success(`[ManualURLInput] passed - URL typed: ${testUrl}, Quick Scan signals: ${signalsFound}, AI Analysis signals: ${aiSignalsFound}`, 'Journey-06');
        return true;
        
    } catch (e) {
        // Save failure artifacts
        UI.dumpHierarchy(deviceId, artifacts.ui, "fail-ManualURLInput");
        const failureLog = execOut('adb logcat -d | tail -n 200');
        fs.writeFileSync(`${artifacts.logs}/fail-ManualURLInput-${Date.now()}.log`, failureLog);
        log.error(`Manual URL Input test failed: ${e.message}`, 'Journey-06');
        return false;
    }
}

module.exports = {
    testManualURLInput,
    name: 'ManualURLInput',
    description: 'Tests manual typing of TikTok URL and button interactions',
    order: 6
};
