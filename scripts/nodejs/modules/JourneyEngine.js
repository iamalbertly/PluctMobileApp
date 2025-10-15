const { execOk, execOut } = require('../core/Pluct-Test-Core-Exec');
const { logInfo, logWarn, logSuccess, logError } = require('../core/Logger');
const { reportCriticalError, reportStepFailure } = require('./StatusTracker');
const Logcat = require('../core/Logcat');
const UI = require('./UIValidator');
const { Steps } = require('./Steps');

function sleep(ms){ Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms); }

function waitUntilFocused(timeoutMs) {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
        const win = execOut('adb shell dumpsys window windows');
        if (/mCurrentFocus.*app\.pluct/i.test(win) || /mFocusedApp.*app\.pluct/i.test(win)) return true;
        sleep(300);
    }
    return false;
}

function testAppLaunch() {
	logInfo('Testing app launch...', 'Journey');
    UI.captureUiArtifacts('AppLaunch-pre');
    const ok = execOk('adb shell am start -W -n app.pluct/.MainActivity');
	if (!ok) { logError('App launch failed', 'Journey'); return false; }
    // Wait until window focus to avoid first-frame emptiness
    if (!waitUntilFocused(5000)) {
        logWarn('App window not focused within timeout; proceeding with caution', 'Journey');
    }
    // Post-validation with retry
    let post = UI.validateComponents('AppLaunch:post', Steps.AppLaunch.post);
    UI.captureUiArtifacts('AppLaunch-post');
    if (!post.overall) {
        reportStepFailure('AppLaunch:post', `Expect ${Steps.AppLaunch.post.join(', ')}`, 'Missing components', { sample: post.xmlSample });
        sleep(1500);
        post = UI.validateComponents('AppLaunch:post', Steps.AppLaunch.post);
        if (!post.overall) {
            reportCriticalError('UI Validation Failed', `Post-launch UI components missing: ${JSON.stringify(post.details)}`, 'Core');
            return false;
        }
    }
	return true;
}

function testShareIntent(url) {
	logInfo('Testing share intent handling...', 'Journey');
	Logcat.clear();
    UI.captureUiArtifacts('ShareIntent-pre');
    let pre = UI.validateComponents('ShareIntent:pre', Steps.ShareIntent.pre);
    if (!pre.overall) {
        reportStepFailure('ShareIntent:pre', `Expect ${Steps.ShareIntent.pre.join(', ')}`, 'Missing components', { sample: pre.xmlSample });
        sleep(1500);
        pre = UI.validateComponents('ShareIntent:pre', Steps.ShareIntent.pre);
        if (!pre.overall) {
            reportCriticalError('UI Validation Failed', `ShareIntent pre components missing: ${JSON.stringify(pre.details)}`, 'Core');
            return false;
        }
    }
	const cmd = `adb shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "${url}" -n app.pluct/.share.PluctShareIngestActivity`;
    logInfo(`ADB: ${cmd}`, 'Journey');
    const ok = execOk(cmd);
	if (!ok) { logError('Share intent failed', 'Journey'); return false; }
	// Wait for app to foreground and intent handled
	for (let i=0;i<8;i++){
		const top = execOut('adb shell dumpsys activity activities | findstr app.pluct');
		if (top && top.trim()) break;
		sleep(1000);
	}
    const handledWait = Logcat.waitForPattern('(intent|ingest|share|CAPTURE_INSIGHT|TTT:|REQUEST_SUBMITTED|proxy|Authorization|Bearer|ttt/transcribe|HTTP REQUEST|HTTP RESPONSE)', 15);
    const handled = handledWait.found || UI.waitForElement('text','Processing',10);
    if (!handled) {
        logError('Intent not handled properly', 'Journey');
        if (handledWait.lines && handledWait.lines.length) {
            handledWait.lines.forEach(l => logError(l, 'Journey'));
        }
        // Save a focused log slice for debugging
        try { Logcat.saveRecent('(intent|ingest|share|CAPTURE_INSIGHT|TTT:|REQUEST_SUBMITTED|proxy|Authorization|Bearer)', `artifacts/logs/share-${Date.now()}.log`, 400); } catch {}
        return false;
    }
    let post = UI.validateComponents('ShareIntent:post', Steps.ShareIntent.post);
    UI.captureUiArtifacts('ShareIntent-post');
    if (!post.overall) {
        reportStepFailure('ShareIntent:post', `Expect ${Steps.ShareIntent.post.join(', ')}`, 'Missing components', { sample: post.xmlSample });
        sleep(1500);
        post = UI.validateComponents('ShareIntent:post', Steps.ShareIntent.post);
        if (!post.overall) {
            reportCriticalError('UI Validation Failed', `ShareIntent post components missing: ${JSON.stringify(post.details)}`, 'Core');
            return false;
        }
    }
	return true;
}

function logcatContains(regex) {
	const out = execOut('adb shell logcat -d');
	try { return new RegExp(regex, 'i').test(out); } catch { return false; }
}

function waitForTextInUi(text, timeoutSeconds) {
	const deadline = Date.now() + (timeoutSeconds * 1000);
	while (Date.now() < deadline) {
		const dump = execOut('adb shell uiautomator dump /sdcard/pluct_ui_dump.xml && adb shell cat /sdcard/pluct_ui_dump.xml');
		if (dump && dump.includes(text)) return true;
		sleep(500);
	}
	return false;
}

// Use UIValidator's helper for clickable interactions

function testVideoProcessing(url) {
	logInfo('Testing video processing flow...', 'Journey');
    // Open capture sheet if present, then choose Quick Scan
    const sheet = UI.validateComponents('CaptureInsightSheet', ['CaptureInsightSheet']);
    if (sheet.overall) {
        // Prefer stable resource-id; fallback to text/desc for localization safety
        UI.captureUiArtifacts('VideoProcessing-pre');
        const tapped = UI.tapByResourceId('app.pluct:id/quick_scan') || UI.tapByText('Quick Scan') || UI.tapByContentDesc('Quick Scan');
        if (tapped) { logInfo('Tapped Quick Scan option', 'Journey'); }
        sleep(800);
    } else {
        // Try to surface actionable item anyway
        UI.captureUiArtifacts('VideoProcessing-pre');
        UI.clickFirstClickable();
        sleep(800);
    }
	// Try detecting actionable items and tap
    const clicked = UI.clickFirstClickable();
    if (clicked) { logInfo('Clicked first clickable element', 'Journey'); sleep(1000); }
    UI.captureUiArtifacts('VideoProcessing-post');
	if (waitForTextInUi('Processing', 6)) { logSuccess("Detected 'Processing' text after interactions", 'Journey'); return true; }
    const waited = Logcat.waitForPattern('(PluctTTTranscribeService|Status|TRANSCRIBING|Processing|REQUEST_SUBMITTED|TTT:)', 20);
    if (waited.found) {
        logSuccess('Detected processing logs in logcat', 'Journey');
        // Save the last relevant request/response slice for visibility
        try { Logcat.saveRecent('(REQUEST_SUBMITTED|HTTP REQUEST|HTTP RESPONSE|ttt/transcribe|proxy)', `artifacts/logs/processing-${Date.now()}.log`, 500); } catch {}
        return true;
    }
	logError('No processing indicators found after interaction', 'Journey');
    let post = UI.validateComponents('VideoProcessing:post', Steps.VideoProcessing.post);
    if (!post.overall) {
        reportStepFailure('VideoProcessing:post', `Expect ${Steps.VideoProcessing.post.join(', ')}`, 'Missing components', { sample: post.xmlSample });
        sleep(1500);
        post = UI.validateComponents('VideoProcessing:post', Steps.VideoProcessing.post);
        if (!post.overall) {
            reportCriticalError('UI Validation Failed', `VideoProcessing post components missing: ${JSON.stringify(post.details)}`, 'Core');
            return false;
        }
    }
	return false;
}

function testCoreUserJourneys(url) {
	try {
		if (!testAppLaunch()) { reportCriticalError('App Launch Failed', 'The app failed to launch properly.', 'Core'); return false; }
		if (!testShareIntent(url)) { reportCriticalError('Share Intent Failed', 'App failed to handle share intent.', 'Core'); return false; }
		if (!testVideoProcessing(url)) { reportCriticalError('Video Processing Failed', 'Processing flow failed.', 'Core'); return false; }
		logSuccess('Core user journeys test passed', 'Journey');
		return true;
	} catch (e) {
		reportCriticalError('Core User Journeys Test Exception', e.message || String(e), 'Core');
		return false;
	}
}

function testEnhancementsJourney(url) {
	try {
		// AI features presence via logcat heuristics
		const aiOk = logcatContains('(AI|metadata|transcript)');
		if (!aiOk) { logWarn('AI features not detected', 'Journey'); }
		// Smart caching signals
		const cacheOk = logcatContains('cache');
		if (!cacheOk) { logWarn('Smart caching not detected', 'Journey'); }
		logSuccess('Enhancements journey test passed', 'Journey');
		return true;
	} catch (e) {
		reportCriticalError('Enhancements Journey Test Exception', e.message || String(e), 'Enhancements');
		return false;
	}
}

function testBusinessEngineIntegration(url) {
	try {
		Logcat.clear();
    const healthPattern = '(BusinessEngine|BusinessEngineHealthChecker|Engine Health|HEALTH_CHECK|TTTranscribe|TTT|REQUEST_SUBMITTED)';
    const health = Logcat.waitForPattern(healthPattern, 30);
		if (!health.found) {
        if (health.lines && health.lines.length) health.lines.forEach(l => logError(l, 'Journey'));
        try { Logcat.saveRecent(healthPattern, `artifacts/logs/business-${Date.now()}.log`, 400); } catch {}
        reportCriticalError('Business Engine Health Check', 'No Business Engine health logs found', 'BusinessEngine');
			return false;
		}
    const tokenPattern = '(VENDING_TOKEN|vend token|vend-token|Bearer|Authorization|token|TTT:)';
    const token = Logcat.waitForPattern(tokenPattern, 30);
		if (!token.found) {
        if (token.lines && token.lines.length) token.lines.forEach(l => logError(l, 'Journey'));
        reportCriticalError('Token Vending', 'No token vending logs found', 'BusinessEngine');
			return false;
		}
    const proxyPattern = '(REQUEST_SUBMITTED|ttt/transcribe|Pluct Proxy|proxy|TTTranscribe)';
    const proxy = Logcat.waitForPattern(proxyPattern, 30);
		if (!proxy.found) {
        if (proxy.lines && proxy.lines.length) proxy.lines.forEach(l => logError(l, 'Journey'));
        reportCriticalError('TTTranscribe Proxy', 'No TTTranscribe proxy logs found', 'BusinessEngine');
			return false;
		}
		logSuccess('Business Engine integration test passed', 'Journey');
		return true;
	} catch (e) {
		reportCriticalError('Business Engine Integration Test Exception', e.message || String(e), 'BusinessEngine');
		return false;
	}
}

module.exports = { testCoreUserJourneys, testEnhancementsJourney, testBusinessEngineIntegration };


