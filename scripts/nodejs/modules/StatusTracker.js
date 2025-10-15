const { logInfo, logError } = require('../core/Logger');

const TestSession = {
	StartTime: 0,
	TestUrl: '',
	BuildRequired: false,
	CriticalErrors: [],
	Logs: [],
	SmartBuildDetection: {
		LastBuildTime: null,
		ChangedFiles: [],
		BuildReason: ''
	}
};

function reportCriticalError(type, message, stage = 'Unknown') {
	logError(`\u274c CRITICAL ERROR: ${type}`, 'Report');
	logError(`Stage: ${stage}`, 'Report');
	logError(`Error Details: ${message}`, 'Report');
	TestSession.CriticalErrors.push({ type, message, stage, timestamp: Date.now() });
}

function reportStepFailure(stepName, expectation, observation, extra = {}) {
    const stage = extra.stage || 'Validation';
    logError(`Step Failure: ${stepName}`, 'Report');
    logError(`Expected: ${expectation}`, 'Report');
    logError(`Observed: ${observation}`, 'Report');
    if (extra.hints) {
        logError(`Hints: ${extra.hints}`, 'Report');
    }
    if (extra.sample) {
        const sample = String(extra.sample);
        const truncated = sample.length > 800 ? sample.slice(0, 800) + '... [truncated]' : sample;
        logError(`Sample: ${truncated}`, 'Report');
    }
    // Track non-critical step failures as part of session logs for the final report
    if (!Array.isArray(TestSession.Logs)) TestSession.Logs = [];
    TestSession.Logs.push({
        type: 'StepFailure',
        stepName,
        expectation,
        observation,
        stage,
        timestamp: Date.now(),
        meta: extra || {},
    });
}

function showTestReport(overallSuccess) {
	const durationSec = (Date.now() - (TestSession.StartTime || Date.now())) / 1000;
	logInfo('=== TEST REPORT ===', 'Report');
	logInfo(`Duration: ${durationSec.toFixed(2)} seconds`, 'Report');
	logInfo(`Test URL: ${TestSession.TestUrl}`, 'Report');
	logInfo(`Build Required: ${TestSession.BuildRequired}`, 'Report');
	if (TestSession.SmartBuildDetection.BuildReason) {
		logInfo(`Build Reason: ${TestSession.SmartBuildDetection.BuildReason}`, 'Report');
	}
	if (TestSession.CriticalErrors.length > 0) {
		logError(`Critical Errors: ${TestSession.CriticalErrors.length}`, 'Report');
		for (const e of TestSession.CriticalErrors) {
			logError(` - ${e.type}: ${e.message}`, 'Report');
		}
	}
	if (overallSuccess) {
		logInfo('✅ All tests passed successfully', 'Report');
	} else {
		logError('❌ Some tests failed', 'Report');
	}
}

module.exports = { TestSession, reportCriticalError, reportStepFailure, showTestReport };


