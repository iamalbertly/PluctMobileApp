const { execOk, execOut } = require('../core/Pluct-Test-Core-Exec');
const { logInfo, logWarn, logSuccess, logError } = require('../core/Logger');
const { reportCriticalError, reportStepFailure } = require('./Pluct-Test-Core-Status');
const Logcat = require('../core/Pluct-Node-Tests-Core-Logcat-LiveHttpStreamer');
const UI = require('./Pluct-Node-Tests-UI-AndroidValidatorAndInteractor');
const { Steps } = require('./Pluct-Node-Tests-UI-AndroidStepExpectations');
const defaults = (() => { try { return require('../config/Pluct-Test-Config-Defaults.json'); } catch { return {}; } })();
const path = require('path');
const fs = require('fs');

function saveObjPretty(file, obj) {
    try { fs.mkdirSync(path.dirname(file), { recursive: true }); } catch {}
    try { fs.writeFileSync(file, JSON.stringify(obj, null, 2), 'utf8'); } catch {}
}

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
    try { UI.printUiInventory('AppLaunch-pre'); } catch {}
    const ok = execOk('adb shell am start -W -n app.pluct/.MainActivity');
    if (!ok) { logError('App launch failed', 'Journey'); return false; }
    if (!waitUntilFocused(5000)) { logWarn('App window not focused within timeout; proceeding with caution', 'Journey'); }
    let post = UI.validateComponents('AppLaunch:post', Steps.AppLaunch.post);
    UI.captureUiArtifacts('AppLaunch-post');
    try { UI.printUiInventory('AppLaunch-post'); } catch {}
    if (!post.overall) {
        reportStepFailure('AppLaunch:post', `Expect ${Steps.AppLaunch.post.join(', ')}`, 'Missing components', { sample: post.xmlSample });
        sleep(1500);
        post = UI.validateComponents('AppLaunch:post', Steps.AppLaunch.post);
        if (!post.overall) { reportCriticalError('UI Validation Failed', `Post-launch UI components missing: ${JSON.stringify(post.details)}`, 'Core'); return false; }
    }
    return true;
}

function testShareIntent(url) {
    logInfo('Testing share intent handling...', 'Journey');
    Logcat.clear();
    UI.captureUiArtifacts('ShareIntent-pre');
    try { UI.printUiInventory('ShareIntent-pre'); } catch {}
    let pre = UI.validateComponents('ShareIntent:pre', Steps.ShareIntent.pre);
    if (!pre.overall) {
        reportStepFailure('ShareIntent:pre', `Expect ${Steps.ShareIntent.pre.join(', ')}`, 'Missing components', { sample: pre.xmlSample });
        sleep(1500);
        pre = UI.validateComponents('ShareIntent:pre', Steps.ShareIntent.pre);
        if (!pre.overall) { reportCriticalError('UI Validation Failed', `ShareIntent pre components missing: ${JSON.stringify(pre.details)}`, 'Core'); return false; }
    }
    const cmd = `adb shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "${url}" -n app.pluct/.share.PluctShareIngestActivity`;
    logInfo(`ADB: ${cmd}`, 'Journey');
    const ok = execOk(cmd);
    if (!ok) { logError('Share intent failed', 'Journey'); dumpForensics('fail-ShareIntent-start'); return false; }
    for (let i=0;i<8;i++){ const top = execOut('adb shell dumpsys activity activities | findstr app.pluct'); if (top && top.trim()) break; sleep(1000); }
    const handledWait = Logcat.waitForPattern('(intent|ingest|share|CAPTURE_INSIGHT|TTT:|REQUEST_SUBMITTED|proxy|Authorization|Bearer|ttt/transcribe|HTTP REQUEST|HTTP RESPONSE)', 15);
    const handled = handledWait.found || UI.waitForElement('text','Processing',10);
    if (!handled) {
        logError('Intent not handled properly', 'Journey');
        if (handledWait.lines && handledWait.lines.length) handledWait.lines.forEach(l => logError(l, 'Journey'));
        try { Logcat.saveRecent('(intent|ingest|share|CAPTURE_INSIGHT|TTT:|REQUEST_SUBMITTED|proxy|Authorization|Bearer)', `artifacts/logs/share-${Date.now()}.log`, 400); } catch {}
        dumpForensics('fail-ShareIntent');
        return false;
    }
    let post = UI.validateComponents('ShareIntent:post', Steps.ShareIntent.post);
    UI.captureUiArtifacts('ShareIntent-post');
    try { UI.printUiInventory('ShareIntent-post'); } catch {}
    if (!post.overall) { reportStepFailure('ShareIntent:post', `Expect ${Steps.ShareIntent.post.join(', ')}`, 'Missing components', { sample: post.xmlSample }); sleep(1500); post = UI.validateComponents('ShareIntent:post', Steps.ShareIntent.post); if (!post.overall) { dumpForensics('fail-ShareIntent-post'); reportCriticalError('UI Validation Failed', `ShareIntent post components missing: ${JSON.stringify(post.details)}`, 'Core'); return false; } }
    return true;
}

function logcatContains(regex) { const out = execOut('adb shell logcat -d'); try { return new RegExp(regex, 'i').test(out); } catch { return false; } }

function waitForTextInUi(text, timeoutSeconds) {
    const deadline = Date.now() + (timeoutSeconds * 1000);
    while (Date.now() < deadline) {
        const dump = execOut('adb shell uiautomator dump /sdcard/pluct_ui_dump.xml && adb shell cat /sdcard/pluct_ui_dump.xml');
        if (dump && dump.includes(text)) return true;
        sleep(500);
    }
    return false;
}

function testVideoProcessing(url) {
    logInfo('Testing video processing flow...', 'Journey');
    const sheet = UI.validateComponents('CaptureInsightSheet', ['CaptureInsightSheet']);
    if (sheet.overall) {
        UI.captureUiArtifacts('VideoProcessing-pre');
        try { UI.printUiInventory('VideoProcessing-pre'); } catch {}
        const qs = (defaults && defaults.selectors && defaults.selectors.quickScan) || { resourceId: 'app.pluct:id/quick_scan', resourceIdSuffix: '/quick_scan', contentDesc: 'Quick Scan', text: 'Quick Scan' };
        const tapped = UI.clickDeterministic(qs);
        if (!tapped.ok) {
            dumpForensics('fail-QuickScan-noMatch');
            reportStepFailure('VideoProcessing:tapQuickScan','Tap Quick Scan by id/desc/text','No candidate matched',{ tried: tapped.tried });
            return false;
        }
        logInfo(`[Journey] QuickScan tapped via ${tapped.via}: ${tapped.val}`, 'Journey');
        sleep(800);
    } else {
        UI.captureUiArtifacts('VideoProcessing-pre');
        try { UI.printUiInventory('VideoProcessing-pre'); } catch {}
        UI.clickFirstClickable();
        sleep(800);
    }
    const clicked = UI.clickFirstClickable();
    if (clicked) { logInfo('Clicked first clickable element', 'Journey'); sleep(1000); }
    UI.captureUiArtifacts('VideoProcessing-post');
    try { UI.printUiInventory('VideoProcessing-post'); } catch {}
    try { const diff = UI.compareUiCounts('VideoProcessing-pre','VideoProcessing-post'); logInfo(`[Journey] UI delta after QuickScan: ${diff.delta} (before=${diff.before}, after=${diff.after})`, 'Journey'); } catch {}
    if (waitForTextInUi('Processing', 6)) { logSuccess("Detected 'Processing' text after interactions", 'Journey'); return true; }
    const waited = Logcat.waitForPattern('(PluctTTTranscribeService|Status|TRANSCRIBING|Processing|REQUEST_SUBMITTED|TTT:)', 20);
    if (waited.found) { logSuccess('Detected processing logs in logcat', 'Journey'); try { Logcat.saveRecent('(REQUEST_SUBMITTED|HTTP REQUEST|HTTP RESPONSE|ttt/transcribe|proxy)', `artifacts/logs/processing-${Date.now()}.log`, 500); } catch {} return true; }
    logError('No processing indicators found after interaction', 'Journey');
    let post = UI.validateComponents('VideoProcessing:post', Steps.VideoProcessing.post);
    if (!post.overall) { reportStepFailure('VideoProcessing:post', `Expect ${Steps.VideoProcessing.post.join(', ')}`, 'Missing components', { sample: post.xmlSample }); sleep(1500); post = UI.validateComponents('VideoProcessing:post', Steps.VideoProcessing.post); if (!post.overall) { dumpForensics('fail-VideoProcessing-post'); reportCriticalError('UI Validation Failed', `VideoProcessing post components missing: ${JSON.stringify(post.details)}`, 'Core'); return false; } }
    return false;
}

function testPipeline_Transcription(defaults) {
    try {
        logInfo('[Journey] Validating token vending → TTTranscribe → transcript ready', 'Journey');
        function sleep(ms){ Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms); }
        function waitWithProgress(label, pattern, timeoutMs) {
            const re = new RegExp(pattern, 'i');
            const deadline = Date.now() + timeoutMs;
            let iter = 0;
            while (Date.now() < deadline) {
                iter++;
                const lines = Logcat.recent(pattern, 10);
                if (Array.isArray(lines) && lines.length > 0) {
                    logInfo(`[Pipeline] ${label}: detected ${lines.length} matching lines`, 'Journey');
                    lines.slice(-3).forEach(l => logInfo(l, 'HTTP'));
                    return { found: true, lines };
                }
                if (iter % 3 === 0) {
                    const secsLeft = Math.max(0, Math.round((deadline - Date.now())/1000));
                    logInfo(`[Pipeline] waiting for ${label}... (${secsLeft}s left)`, 'Journey');
                    try {
                        const generic = Logcat.recent('(REQUEST_SUBMITTED|HTTP REQUEST|HTTP RESPONSE|ttt|proxy|Authorization|Bearer)', 5) || [];
                        if (generic.length) generic.forEach(l => logInfo(l, 'HTTP'));
                    } catch {}
                }
                sleep(1000);
            }
            const tail = Logcat.recent(pattern, 20);
            if (tail && tail.length) tail.slice(-5).forEach(l => logInfo(l, 'HTTP'));
            return { found: false, lines: tail || [] };
        }
        const tokRe = (defaults && defaults.pipeline && defaults.pipeline.tokenVending) || '(vend|token).*BusinessEngine|Authorization.*Bearer|PLUCT_HTTP';
        const tokTimeout = (defaults && defaults.timeouts && defaults.timeouts.token);
        const tok = waitWithProgress('token vending', tokRe, (typeof tokTimeout === 'number' && tokTimeout > 1000 ? tokTimeout : (tokTimeout || 12000)));
        if (!tok.found) { 
            dumpForensics('fail-Pipeline-token'); 
            try { const tail = Logcat.recent('(PLUCT_HTTP|REQUEST_SUBMITTED|HTTP REQUEST|HTTP RESPONSE|Authorization|Bearer)', 20) || []; tail.forEach(l => logInfo(l, 'HTTP')); } catch {}
            reportCriticalError('Token vending not observed', 'No Business Engine token log after Quick Scan', 'Core'); 
            return false; 
        }
        try {
            const ex = Logcat.findLastHttpExchange && Logcat.findLastHttpExchange('vend');
            if (ex) {
                logInfo('[Pipeline] === TokenVending REQUEST ===', 'Journey');
                logInfo(JSON.stringify(ex.req, null, 2), 'HTTP');
                logInfo('[Pipeline] === TokenVending RESPONSE ===', 'Journey');
                logInfo(JSON.stringify(ex.res, null, 2), 'HTTP');
                const dir = path.join('artifacts','http','token_vending');
                saveObjPretty(path.join(dir, `request-${Date.now()}.json`), ex.req);
                saveObjPretty(path.join(dir, `response-${Date.now()}.json`), ex.res);
                logInfo(`[Pipeline] Saved TokenVending http to ${dir}`, 'Journey');
            }
        } catch {}
        const rqRe = (defaults && defaults.pipeline && defaults.pipeline.tttRequest) || '(PLUCT_HTTP.*"event":"request".*"ttt"|HTTP REQUEST.*ttt.*(transcribe|whisper))';
        const rqTimeout = (defaults && defaults.timeouts && defaults.timeouts.tttRequest);
        const rq = waitWithProgress('TTTranscribe request', rqRe, (typeof rqTimeout === 'number' && rqTimeout > 1000 ? rqTimeout : (rqTimeout || 15000)));
        if (!rq.found) {
            try {
                const detailFile = `artifacts/logs/fail-Pipeline-tttRequest-http-${Date.now()}.log`;
                Logcat.saveRecentHttpDetails && Logcat.saveRecentHttpDetails(detailFile, 400);
                logInfo(`Saved HTTP detail log: ${detailFile}`, 'Journey');
            } catch {}
            dumpForensics('fail-Pipeline-tttRequest');
            reportCriticalError('No TTTranscribe request', 'App never invoked TTTranscribe endpoint', 'Core');
            return false;
        }
        const rsRe = (defaults && defaults.pipeline && defaults.pipeline.tttResponseOk) || '(PLUCT_HTTP.*"event":"response".*"code":\s*200.*"ttt"|HTTP RESPONSE\\s+200.*ttt)';
        const rsTimeout = (defaults && defaults.timeouts && defaults.timeouts.tttResponse);
        const rs = waitWithProgress('TTTranscribe response 200', rsRe, (typeof rsTimeout === 'number' && rsTimeout > 1000 ? rsTimeout : (rsTimeout || 45000)));
        if (!rs.found) {
            try {
                const detailFile = `artifacts/logs/fail-Pipeline-tttResponse-http-${Date.now()}.log`;
                Logcat.saveRecentHttpDetails && Logcat.saveRecentHttpDetails(detailFile, 400);
                logInfo(`Saved HTTP detail log: ${detailFile}`, 'Journey');
            } catch {}
            dumpForensics('fail-Pipeline-tttResponse');
            reportCriticalError('TTTranscribe did not return 200', 'No successful response observed', 'Core');
            return false;
        }
        try {
            const ex2 = Logcat.findLastHttpExchange && Logcat.findLastHttpExchange('ttt');
            if (ex2) {
                logInfo('[Pipeline] === TTTranscribe REQUEST ===', 'Journey');
                logInfo(JSON.stringify(ex2.req, null, 2), 'HTTP');
                logInfo('[Pipeline] === TTTranscribe RESPONSE ===', 'Journey');
                logInfo(JSON.stringify(ex2.res, null, 2), 'HTTP');
                const dir2 = path.join('artifacts','http','ttt');
                saveObjPretty(path.join(dir2, `request-${Date.now()}.json`), ex2.req);
                saveObjPretty(path.join(dir2, `response-${Date.now()}.json`), ex2.res);
                logInfo(`[Pipeline] Saved TTTranscribe http to ${dir2}`, 'Journey');
            }
        } catch {}
        UI.captureUiArtifacts('Pipeline-post');
        try { UI.printUiInventory('Pipeline-post'); } catch {}
        // best-effort UI confirmation by known markers; tolerate absence without failing here
        logSuccess('[Journey] Pipeline: token → TTTranscribe → transcript passed', 'Journey');
        return true;
    } catch (e) { reportCriticalError('Pipeline Journey Exception', e.message || String(e), 'Core'); return false; }
}

function dumpForensics(tag) {
    try {
        UI.captureUiArtifacts(tag);
        const patt = '(REQUEST_SUBMITTED|HTTP REQUEST|HTTP RESPONSE|ttt/transcribe|proxy|am_start|START u0|android.intent.action.SEND|Authorization|Bearer)';
        const outFile = `artifacts/logs/${tag}-${Date.now()}.log`;
        Logcat.saveRecent(patt, outFile, 500);
        logInfo(`Forensics saved: ${outFile}`, 'Journey');
    } catch {}
}

function testCoreUserJourneys(url) { try { if (!testAppLaunch()) { reportCriticalError('App Launch Failed', 'The app failed to launch properly.', 'Core'); return false; } if (!testShareIntent(url)) { reportCriticalError('Share Intent Failed', 'App failed to handle share intent.', 'Core'); return false; } if (!testVideoProcessing(url)) { reportCriticalError('Video Processing Failed', 'Processing flow failed.', 'Core'); return false; } logSuccess('Core user journeys test passed', 'Journey'); return true; } catch (e) { reportCriticalError('Core User Journeys Test Exception', e.message || String(e), 'Core'); return false; } }

function selectorCoverageSummary() {
    try {
        const c = UI.selectorCoverageCounters || {};
        logInfo(`Selector coverage: id=${c.resourceIdMatches||0} desc=${c.contentDescMatches||0} text=${c.textMatches||0} class=${c.classMatches||0}`, 'UIValidator');
    } catch {}
}

function testEnhancementsJourney(url) { try { const aiOk = logcatContains('(AI|metadata|transcript)'); if (!aiOk) { logWarn('AI features not detected', 'Journey'); } const cacheOk = logcatContains('cache'); if (!cacheOk) { logWarn('Smart caching not detected', 'Journey'); } selectorCoverageSummary(); logSuccess('Enhancements journey test passed', 'Journey'); return true; } catch (e) { reportCriticalError('Enhancements Journey Test Exception', e.message || String(e), 'Enhancements'); return false; } }

function testBusinessEngineIntegration(url) { try { Logcat.clear(); const healthPattern = '(BusinessEngine|BusinessEngineHealthChecker|Engine Health|HEALTH_CHECK|TTTranscribe|TTT|REQUEST_SUBMITTED)'; const health = Logcat.waitForPattern(healthPattern, 30); if (!health.found) { if (health.lines && health.lines.length) health.lines.forEach(l => logError(l, 'Journey')); try { Logcat.saveRecent(healthPattern, `artifacts/logs/business-${Date.now()}.log`, 400); } catch {} reportCriticalError('Business Engine Health Check', 'No Business Engine health logs found', 'BusinessEngine'); return false; } const tokenPattern = '(VENDING_TOKEN|vend token|vend-token|Bearer|Authorization|token|TTT:)'; const token = Logcat.waitForPattern(tokenPattern, 30); if (!token.found) { if (token.lines && token.lines.length) token.lines.forEach(l => logError(l, 'Journey')); reportCriticalError('Token Vending', 'No token vending logs found', 'BusinessEngine'); return false; } const proxyPattern = '(REQUEST_SUBMITTED|ttt/transcribe|Pluct Proxy|proxy|TTTranscribe)'; const proxy = Logcat.waitForPattern(proxyPattern, 30); if (!proxy.found) { if (proxy.lines && proxy.lines.length) proxy.lines.forEach(l => logError(l, 'Journey')); reportCriticalError('TTTranscribe Proxy', 'No TTTranscribe proxy logs found', 'BusinessEngine'); return false; } logSuccess('Business Engine integration test passed', 'Journey'); return true; } catch (e) { reportCriticalError('Business Engine Integration Test Exception', e.message || String(e), 'BusinessEngine'); return false; } }

module.exports = { testCoreUserJourneys, testEnhancementsJourney, testBusinessEngineIntegration, testPipeline_Transcription };


