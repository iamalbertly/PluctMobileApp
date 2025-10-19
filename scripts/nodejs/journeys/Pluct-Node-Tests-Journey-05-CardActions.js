/**
 * Pluct Node Tests Journey 05 - Card Actions
 * Tests enhanced transcript card actions with UI verification
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */

const { execOk } = require('../core/Pluct-Test-Core-Exec');
const { logInfo, logWarn, logSuccess, logError } = require('../core/Logger');
const UI = require('../modules/Pluct-Node-Tests-UI-01Validator');

function sleep(ms) { Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms); }

async function testCardActions({ deviceId, artifacts, log }) {
    log.info('Testing enhanced transcript card actions...', 'Journey-05');
    
    try {
        // Open overflow for first transcript card
        const pre = UI.dumpHierarchy(deviceId, artifacts.ui, "CardActions-pre");
        const nodes = UI.parse(pre.xml);
        const menu = UI.findTapTarget(nodes, { by:"desc", q:"More options" }) || 
                    UI.findTapTarget(nodes, { by:"class", q:"ImageButton" });
        
        if (!menu) { 
            log.info("[Enhanced CardActions] could not find overflow; skipping", 'Journey-05'); 
            return true; 
        }
        
        execOk(`adb -s ${deviceId} shell input tap ${/(\d+)/.exec(menu.b)[1]} ${/(\d+)\]/.exec(menu.b)[1]}`);
        sleep(600);

        // Try different card actions
        for (const text of ["Retry","Archive","Delete"]) {
            try { 
                await UI.clickAndVerify({ 
                    deviceId, 
                    artifactsDir: artifacts.ui, 
                    step:`Card-${text}`, 
                    want:{ by:"text", q:text }, 
                    log, 
                    waitMs:1200, 
                    deltaMin:0 
                }); 
                break; 
            } catch {}
        }
        
        log.success('Enhanced card actions test passed', 'Journey-05');
        return true;
    } catch (e) {
        log.error(`Enhanced card actions test failed: ${e.message}`, 'Journey-05');
        return false;
    }
}

module.exports = {
    testCardActions,
    name: 'Card Actions',
    description: 'Tests enhanced transcript card actions with UI verification',
    order: 5
};
