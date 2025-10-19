const { logInfo, logError } = require('../core/Logger');
const fs = require('fs');
const path = require('path');

const TestSession = {
    StartTime: 0,
    TestUrl: '',
    BuildRequired: false,
    CriticalErrors: [],
    Logs: [],
    UIActions: [],
    Artifacts: { uiDir: path.join('artifacts','ui'), logsDir: path.join('artifacts','logs'), sessionLogFile: '' },
    SmartBuildDetection: { LastBuildTime: null, ChangedFiles: [], BuildReason: '' }
};

function reportCriticalError(type, message, stage = 'Unknown') {
    logError(`\u274c CRITICAL ERROR: ${type}`, 'Report');
    logError(`Stage: ${stage}`, 'Report');
    logError(`Error Details: ${message}`, 'Report');
    TestSession.CriticalErrors.push({ type, message, stage, timestamp: Date.now() });
    
    // === FORENSIC BUNDLE ON CRITICAL ERROR ===
    try {
        const { execOk } = require('../core/Pluct-Test-Core-Exec');
        const Logcat = require('../core/Pluct-Node-Tests-Core-Logcat-LiveHttpStreamer');
        const UI = require('./UIValidator');
        
        // Screenshot
        execOk('adb exec-out screencap -p > artifacts/ui/last-failure.png');
        
        // UI XML
        UI.captureUiArtifacts(`fail-${Date.now()}`);
        
        // HTTP telemetry - log to console instead of file
        const recent = Logcat.recentHttpDetails && Logcat.recentHttpDetails(100);
        if (recent && recent.length > 0) {
            logInfo('[Forensics] Recent HTTP activity:', 'Report');
            recent.forEach(l => logInfo(`  ${l}`, 'Report'));
        }
        
        logInfo('Forensic bundle captured: screenshot + UI XML + HTTP telemetry logged to console', 'Report');
    } catch (e) {
        logError(`Forensic bundle failed: ${e.message}`, 'Report');
    }
}

function reportStepFailure(stepName, expectation, observation, extra = {}) {
    const stage = extra.stage || 'Validation';
    logError(`Step Failure: ${stepName}`, 'Report');
    logError(`Expected: ${expectation}`, 'Report');
    logError(`Observed: ${observation}`, 'Report');
    if (extra.hints) logError(`Hints: ${extra.hints}`, 'Report');
    if (extra.sample) {
        const sample = String(extra.sample);
        const truncated = sample.length > 800 ? sample.slice(0, 800) + '... [truncated]' : sample;
        logError(`Sample: ${truncated}`, 'Report');
    }
    if (!Array.isArray(TestSession.Logs)) TestSession.Logs = [];
    TestSession.Logs.push({ type: 'StepFailure', stepName, expectation, observation, stage, timestamp: Date.now(), meta: extra || {} });
}

function recordUiAction(action, meta = {}) {
    const entry = { t: Date.now(), action, meta };
    try {
        const dir = TestSession.Artifacts.logsDir;
        try { fs.mkdirSync(dir, { recursive: true }); } catch {}
        const file = path.join(dir, `ui-actions-${new Date().toISOString().replace(/[:.]/g,'_')}.log`);
        fs.appendFileSync(file, JSON.stringify(entry) + '\n');
        TestSession.Artifacts.lastUiActionsFile = file;
    } catch {}
    if (!Array.isArray(TestSession.UIActions)) TestSession.UIActions = [];
    TestSession.UIActions.push(entry);
}

function showTestReport(overallSuccess) {
    const durationSec = (Date.now() - (TestSession.StartTime || Date.now())) / 1000;
    logInfo('=== TEST REPORT ===', 'Report');
    logInfo(`Duration: ${durationSec.toFixed(2)} seconds`, 'Report');
    logInfo(`Test URL: ${TestSession.TestUrl}`, 'Report');
    logInfo(`Build Required: ${TestSession.BuildRequired}`, 'Report');
    if (TestSession.SmartBuildDetection.BuildReason) logInfo(`Build Reason: ${TestSession.SmartBuildDetection.BuildReason}`, 'Report');
    logInfo(`Artifacts: ui=${TestSession.Artifacts.uiDir} (screenshots and UI dumps)`, 'Report');
    if (TestSession.CriticalErrors.length > 0) {
        logError(`Critical Errors: ${TestSession.CriticalErrors.length}`, 'Report');
        for (const e of TestSession.CriticalErrors) logError(` - ${e.type}: ${e.message}`, 'Report');
    }
    try {
        const counters = require('./Pluct-Node-Tests-UI-01Validator').selectorCoverageCounters;
        if (counters) {
            logInfo(`Selector coverage: id=${counters.resourceIdMatches||0} desc=${counters.contentDescMatches||0} text=${counters.textMatches||0} class=${counters.classMatches||0}`, 'Report');
        }
    } catch {}
    if (overallSuccess) logInfo('✅ All tests passed successfully', 'Report');
    else logError('❌ Some tests failed', 'Report');
}

module.exports = { TestSession, reportCriticalError, reportStepFailure, showTestReport, recordUiAction };


