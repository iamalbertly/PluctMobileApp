/**
 * Pluct Node Tests Health Checker
 * Validates Business Engine and TTTranscribe endpoints before running journeys
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[CoreResponsibility]
 */

const { execOut } = require('../core/Pluct-Test-Core-Exec');
const { logInfo, logWarn, logSuccess, logError } = require('../core/Logger');

function sleep(ms) { Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms); }

async function checkBusinessEngineHealth() {
    logInfo('Checking Business Engine health...', 'HealthChecker');
    
    try {
        // Clear logcat to get fresh logs
        execOut('adb logcat -c');
        
        // Trigger a balance check through the app
        execOut('adb shell am broadcast -a app.pluct.HEALTH_CHECK_BALANCE');
        
        // Wait for HTTP request to appear
        const deadline = Date.now() + 10000; // 10 seconds
        while (Date.now() < deadline) {
            const out = execOut('adb logcat -d | tail -n 100');
            if (/balance|credits/i.test(out) && /PLUCT_HTTP>OUT/i.test(out)) {
                logSuccess('Business Engine health check passed - balance request observed', 'HealthChecker');
                return true;
            }
            sleep(1000);
        }
        
        logWarn('Business Engine health check failed - no balance request observed', 'HealthChecker');
        return false;
    } catch (e) {
        logError(`Business Engine health check failed: ${e.message}`, 'HealthChecker');
        return false;
    }
}

async function checkTTTranscribeHealth() {
    logInfo('Checking TTTranscribe health...', 'HealthChecker');
    
    try {
        // Check if TTTranscribe work can be enqueued
        const deadline = Date.now() + 10000; // 10 seconds
        while (Date.now() < deadline) {
            const out = execOut('adb logcat -d | tail -n 100');
            if (/ENQUEUING.*TRANSCRIPTION.*WORK/i.test(out)) {
                logSuccess('TTTranscribe health check passed - work enqueueing observed', 'HealthChecker');
                return true;
            }
            sleep(1000);
        }
        
        logWarn('TTTranscribe health check failed - no work enqueueing observed', 'HealthChecker');
        return false;
    } catch (e) {
        logError(`TTTranscribe health check failed: ${e.message}`, 'HealthChecker');
        return false;
    }
}

async function performHealthChecks() {
    logInfo('=== PERFORMING HEALTH CHECKS ===', 'HealthChecker');
    
    const beHealthy = await checkBusinessEngineHealth();
    const tttHealthy = await checkTTTranscribeHealth();
    
    if (!beHealthy || !tttHealthy) {
        logError('Health checks failed - infrastructure may be down', 'HealthChecker');
        logError(`Business Engine: ${beHealthy ? 'OK' : 'FAILED'}`, 'HealthChecker');
        logError(`TTTranscribe: ${tttHealthy ? 'OK' : 'FAILED'}`, 'HealthChecker');
        return false;
    }
    
    logSuccess('All health checks passed - infrastructure is operational', 'HealthChecker');
    return true;
}

module.exports = {
    performHealthChecks,
    checkBusinessEngineHealth,
    checkTTTranscribeHealth
};
