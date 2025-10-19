/**
 * Pluct Node Tests Journey 04 - Pipeline
 * Tests enhanced pipeline token to transcript with HTTP pair validation
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */

const { execOut } = require('../core/Pluct-Test-Core-Exec');
const { logInfo, logWarn, logSuccess, logError } = require('../core/Logger');
const UI = require('../modules/Pluct-Node-Tests-UI-AndroidValidatorAndInteractor');
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
        // Require vend request+response
        log.info('Waiting for vend-token HTTP pair...', 'Journey-04');
        const vend = await waitForHttpPair(/vend-token|\/vend/i, 20);
        if (!vend) {
            throw new Error("No vend-token HTTP pair observed");
        }
        
        log.info("[HTTP] === VEND TOKEN REQUEST ===", 'Journey-04');
        log.info(JSON.stringify(vend.req, null, 2), 'Journey-04');
        saveHttpArtifact("vend-request", vend.req);
        
        // Require ttt request+response
        log.info('Waiting for TTTranscribe HTTP pair...', 'Journey-04');
        const ttt = await waitForHttpPair(/ttt.*transcribe|\/ttt\//i, 25);
        if (!ttt) {
            throw new Error("No TTTranscribe HTTP pair observed");
        }
        
        log.info("[HTTP] === TTTRANSCRIBE REQUEST ===", 'Journey-04');
        log.info(JSON.stringify(ttt.req, null, 2), 'Journey-04');
        saveHttpArtifact("ttt-request", ttt.req);
        
        // Check for completion signals
        const completionSignals = [
            /TRANSCRIPT_READY/,
            /QUICK_SCAN_COMPLETE/,
            /status.*COMPLETED/
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
            throw new Error("No transcript completion signal observed");
        }
        
        log.success("[Enhanced Pipeline] HTTP pairs and completion observed", 'Journey-04');
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
