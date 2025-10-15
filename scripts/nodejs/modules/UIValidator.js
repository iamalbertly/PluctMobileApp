const { execOut, execOutRaw, execOk } = require('../core/Pluct-Test-Core-Exec');
const { logInfo, logWarn, logError, logSuccess } = require('../core/Logger');
const fs = require('fs');
const path = require('path');

const UIElements = {
	MainActivity: {
		Selectors: [
			{ Type: 'resource-id', Value: 'app.pluct:id/main_container' },
			{ Type: 'content-desc', Value: 'Main Activity' },
			{ Type: 'class', Value: 'android.widget.FrameLayout' },
		],
	},
	Navigation: {
		Selectors: [
			{ Type: 'resource-id', Value: 'app.pluct:id/nav_host' },
			{ Type: 'content-desc', Value: 'Navigation Host' },
			{ Type: 'class', Value: 'androidx.navigation.fragment.NavHostFragment' },
		],
	},
	ShareIngestActivity: {
		Selectors: [
			{ Type: 'resource-id', Value: 'app.pluct:id/share_container' },
			{ Type: 'content-desc', Value: 'Share Ingest' },
			{ Type: 'text', Value: 'Processing' },
		],
	},
	CaptureInsightSheet: {
		Selectors: [
			{ Type: 'resource-id', Value: 'app.pluct:id/capture_sheet' },
			{ Type: 'content-desc', Value: 'Capture This Insight' },
			{ Type: 'text', Value: 'Capture This Insight' },
		],
	},
	ProcessingStatus: {
		Selectors: [
			{ Type: 'resource-id', Value: 'app.pluct:id/processing_status' },
			{ Type: 'content-desc', Value: 'Pending' },
			{ Type: 'text', Value: 'Pending' },
		],
	},
};

function getUIHierarchy(retries = 5) {
    const backoffMs = [500, 1000, 2000, 3000, 5000];
    for (let i = 0; i < retries; i++) {
        const dump = execOut('adb exec-out uiautomator dump /dev/tty');
        if (dump && dump.includes('<?xml')) return dump;
        // best-effort dismiss of overlays between attempts
        execOk('adb shell input keyevent 4');
        const wait = backoffMs[Math.min(i, backoffMs.length - 1)];
        Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, wait);
    }
    return '';
}

function findElements(uiXml, type, value) {
	try {
		const xpathByType = {
			'resource-id': `//*[@resource-id='${value}']`,
			'content-desc': `//*[@content-desc='${value}']`,
			'text': `//*[@text='${value}']`,
			'class': `//*[@class='${value}']`,
		};
		const xpath = xpathByType[type] || `//*[@*='${value}']`;
		// lightweight search without XML parser: regex
		const re = new RegExp(`${type}="${value}"`, 'g');
		const matches = uiXml.match(re) || [];
		return matches.length;
	} catch { return 0; }
}

function waitForElement(type, value, timeoutSec = 10) {
	const deadline = Date.now() + timeoutSec * 1000;
	while (Date.now() < deadline) {
		const xml = getUIHierarchy();
		if (xml && findElements(xml, type, value) > 0) return true;
		Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
	}
	return false;
}

function clickByBoundsFromXml(uiXml, type, value) {
	const pattern = new RegExp(`${type}="${value}"[\s\S]*?bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"`);
	const m = pattern.exec(uiXml);
	if (!m) return false;
	const x = Math.floor((parseInt(m[1],10) + parseInt(m[3],10)) / 2);
	const y = Math.floor((parseInt(m[2],10) + parseInt(m[4],10)) / 2);
	return execOk(`adb shell input tap ${x} ${y}`);
}

function clickFirstClickable() {
    const dump = execOut('adb exec-out uiautomator dump /dev/tty');
    const m = /bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*clickable="true"/i.exec(dump || '');
    if (!m) return false;
    const x = Math.floor((parseInt(m[1],10) + parseInt(m[3],10)) / 2);
    const y = Math.floor((parseInt(m[2],10) + parseInt(m[4],10)) / 2);
    return execOk(`adb shell input tap ${x} ${y}`);
}

function tapByText(text) {
    const xml = getUIHierarchy(3);
    if (!xml) return false;
    const esc = String(text).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const pattern = new RegExp(`text="${esc}"[\s\S]*?bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"`, 'i');
    const m = pattern.exec(xml);
    if (!m) return false;
    const x = Math.floor((parseInt(m[1],10) + parseInt(m[3],10)) / 2);
    const y = Math.floor((parseInt(m[2],10) + parseInt(m[4],10)) / 2);
    return execOk(`adb shell input tap ${x} ${y}`);
}

function tapByContentDesc(desc) {
    const xml = getUIHierarchy(3);
    if (!xml) return false;
    const esc = String(desc).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const pattern = new RegExp(`content-desc="${esc}"[\s\S]*?bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"`, 'i');
    const m = pattern.exec(xml);
    if (!m) return false;
    const x = Math.floor((parseInt(m[1],10) + parseInt(m[3],10)) / 2);
    const y = Math.floor((parseInt(m[2],10) + parseInt(m[4],10)) / 2);
    return execOk(`adb shell input tap ${x} ${y}`);
}

function tapByResourceId(resourceId) {
    const xml = getUIHierarchy(3);
    if (!xml) return false;
    const esc = String(resourceId).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const pattern = new RegExp(`resource-id="${esc}"[\s\S]*?bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"`, 'i');
    const m = pattern.exec(xml);
    if (!m) return false;
    const x = Math.floor((parseInt(m[1],10) + parseInt(m[3],10)) / 2);
    const y = Math.floor((parseInt(m[2],10) + parseInt(m[4],10)) / 2);
    return execOk(`adb shell input tap ${x} ${y}`);
}

function validateComponents(stepName, expectedComponents) {
    logInfo(`Validating UI components for step: ${stepName}`, 'UIValidator');
    const xml = getUIHierarchy(5);
    if (!xml) {
        logError('UI hierarchy not available', 'UIValidator');
        // capture artifacts to help debugging
        try {
            const ts = Date.now();
            const dir = path.join('artifacts', 'ui');
            try { fs.mkdirSync(dir, { recursive: true }); } catch {}
            const xmlDump = execOut('adb exec-out uiautomator dump /dev/tty') || '';
            fs.writeFileSync(path.join(dir, `dump-${ts}.xml`), xmlDump);
            const png = execOutRaw('adb exec-out screencap -p');
            if (png && png.length) fs.writeFileSync(path.join(dir, `screen-${ts}.png`), png);
        } catch {}
        return { overall: false, stepName, details: [], xmlSample: '' };
    }
    const results = [];
    let overall = true;
    for (const comp of expectedComponents) {
        const def = UIElements[comp];
        if (!def) {
            logError(`Component '${comp}' not defined`, 'UIValidator');
            overall = false;
            results.push({ component: comp, defined: false, found: false, selectors: [] });
            continue;
        }
        let found = false;
        const selectorChecks = [];
        for (const sel of def.Selectors) {
            const matches = findElements(xml, sel.Type, sel.Value);
            selectorChecks.push({ type: sel.Type, value: sel.Value, matches });
            if (matches > 0) { found = true; }
        }
        if (found) {
            logSuccess(`Component '${comp}' validation PASSED`, 'UIValidator');
        } else {
            logError(`Component '${comp}' validation FAILED`, 'UIValidator');
            overall = false;
        }
        results.push({ component: comp, defined: true, found, selectors: selectorChecks });
    }
    const xmlSample = xml.substring(0, Math.min(xml.length, 2000));
    return { overall, stepName, details: results, xmlSample };
}

function captureUiArtifacts(tag = 'state') {
    try {
        const ts = Date.now();
        const safeTag = String(tag).replace(/[^a-z0-9-_]/gi, '_');
        const dir = path.join('artifacts', 'ui');
        try { fs.mkdirSync(dir, { recursive: true }); } catch {}
        const xmlDump = execOut('adb exec-out uiautomator dump /dev/tty') || '';
        fs.writeFileSync(path.join(dir, `dump-${safeTag}-${ts}.xml`), xmlDump);
        const png = execOutRaw('adb exec-out screencap -p');
        if (png && png.length) fs.writeFileSync(path.join(dir, `screen-${safeTag}-${ts}.png`), png);
        logInfo(`Captured UI artifacts tag='${safeTag}'`, 'UIValidator');
        return true;
    } catch {
        return false;
    }
}

module.exports = { getUIHierarchy, waitForElement, validateComponents, clickByBoundsFromXml, clickFirstClickable, tapByText, tapByContentDesc, tapByResourceId, UIElements, captureUiArtifacts };



