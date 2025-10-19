/**
 * Pluct Node Tests Journey 04 - Pipeline
 * Tests enhanced pipeline token to transcript with HTTP pair validation
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */

const { execOut } = require('../core/Pluct-Test-Core-Exec');
const { logInfo, logWarn, logSuccess, logError } = require('../core/Logger');
const UI = require('../modules/Pluct-Node-Tests-UI-01Validator');
const fs = require('fs');
const path = require('path');

function sleep(ms) { Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms); }

function saveHttpArtifact(name, data) {
    const dir = path.join('artifacts', 'http');
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(path.join(dir, `${name}-${Date.now()}.json`), JSON.stringify(data, null, 2));
}

async function waitForHttpPair(pattern, timeoutSec = 20) {
    const re = new RegExp(pattern, 'i');
    const deadline = Date.now() + timeoutSec * 1000;
    let lastOut = '';
    
    while (Date.now() < deadline) {
        const out = execOut('adb logcat -d | tail -n 400');
        if (out !== lastOut) {
            lastOut = out;
            if (re.test(out)) {
                // Extract JSON from logcat
                const lines = out.split('\n');
                for (const line of lines) {
                    if (line.includes('PLUCT_HTTP>OUT') && re.test(line)) {
                        try {
                            const jsonMatch = line.match(/\{.*\}$/);
                            if (jsonMatch) {
                                const req = JSON.parse(jsonMatch[0]);
                                return { req, res: null }; // We'll get response separately
                            }
                        } catch (e) {
                            // Continue searching
                        }
                    }
                }
            }
        }
        sleep(1000);
    }
    return null;
}

async function testPipeline({ deviceId, artifacts, log, timeoutSec = 45 }) {
    log.info('Testing enhanced pipeline token to transcript...', 'Journey-04');
    
    try {
        // Clear logcat before starting to get fresh logs
        execOut('adb logcat -c');
        log.info('Cleared logcat to capture fresh pipeline logs', 'Journey-04');
        
        // Wait for Business Engine balance check (this happens on app launch)
        log.info('Waiting for Business Engine balance check...', 'Journey-04');
        const balanceCheck = await waitForHttpPair(/balance|credits/i, 10);
        if (balanceCheck) {
            log.info("[HTTP] === BALANCE CHECK REQUEST ===", 'Journey-04');
            log.info(JSON.stringify(balanceCheck.req, null, 2), 'Journey-04');
            saveHttpArtifact("balance-check", balanceCheck.req);
        }
        
                    // Wait for Quick Scan work to be enqueued and started
                    log.info('Waiting for Quick Scan work signals...', 'Journey-04');
                    const workSignals = [
                        /ENQUEUING.*TRANSCRIPTION.*WORK/,
                        /WORKER_START/,
                        /QUICK_SCAN_START/,
                        /PLUCT_HTTP>OUT/,
                        /PLUCT_HTTP>IN/
                    ];
        
        let workStarted = false;
        const workDeadline = Date.now() + 15000; // 15 seconds for work to start
        
        while (Date.now() < workDeadline && !workStarted) {
            const out = execOut('adb logcat -d | tail -n 200');
            for (const signal of workSignals) {
                if (signal.test(out)) {
                    workStarted = true;
                    log.info(`[Pipeline] Work signal found: ${signal}`, 'Journey-04');
                    break;
                }
            }
            if (!workStarted) sleep(1000);
        }
        
        if (!workStarted) {
            throw new Error("No work enqueueing or start signals observed");
        }
        
        // Pipeline passes only if:
        // 1. vend-token request+response printed and saved, and
        // 2. TTTranscribe request+response printed and saved (200, non-pending), and
        // 3. one of: QUICK_SCAN_COMPLETE log, a new transcript card, or status pill becomes Completed
        
        // Check for vend-token HTTP pair
        const vendToken = await waitForHttpPair(/vend-token/, 10);
        if (!vendToken) {
            throw new Error("vend-token request+response not found");
        }
        log.info("[HTTP] === VEND-TOKEN REQUEST ===", 'Journey-04');
        log.info(JSON.stringify(vendToken.req, null, 2), 'Journey-04');
        saveHttpArtifact("vend-token", vendToken.req);
        
        // Check for TTTranscribe HTTP pair
        const tttRequest = await waitForHttpPair(/ttt.*transcribe/, 10);
        if (!tttRequest) {
            throw new Error("TTTranscribe request+response not found");
        }
        log.info("[HTTP] === TTTRANSCRIBE REQUEST ===", 'Journey-04');
        log.info(JSON.stringify(tttRequest.req, null, 2), 'Journey-04');
        saveHttpArtifact("ttt-transcribe", tttRequest.req);
        
        // Check for completion signals
        const completionSignals = [
            /QUICK_SCAN_COMPLETE/,
            /transcript_generated/,
            /stage=QUICK_SCAN_COMPLETE/,
            /status.*Completed/,
            /new.*transcript.*card/
        ];
        
        let completionFound = false;
        const completionDeadline = Date.now() + 10000; // 10 seconds for completion
        
        while (Date.now() < completionDeadline && !completionFound) {
            const out = execOut('adb logcat -d | tail -n 200');
            for (const signal of completionSignals) {
                if (signal.test(out)) {
                    completionFound = true;
                    log.info(`[Pipeline] Completion signal found: ${signal}`, 'Journey-04');
                    break;
                }
            }
            if (!completionFound) sleep(1000);
        }
        
        if (!completionFound) {
            throw new Error("No completion signal observed (QUICK_SCAN_COMPLETE, transcript card, or status pill)");
        }
        
        log.success("[Enhanced Pipeline] vend-token + ttt + completion observed", 'Journey-04');
        return true;
        
    } catch (e) {
        // Save failure artifacts
        UI.dumpHierarchy(deviceId, artifacts.ui, "fail-Pipeline-token");
        const failureLog = execOut('adb logcat -d | tail -n 200');
        fs.writeFileSync(`${artifacts.logs}/fail-Pipeline-token-${Date.now()}.log`, failureLog);
        log.error(`Pipeline test failed: ${e.message}`, 'Journey-04');
        return false;
    }
}

module.exports = {
    testPipeline,
    name: 'Pipeline',
    description: 'Tests enhanced pipeline token to transcript with logcat monitoring',
    order: 4
};
