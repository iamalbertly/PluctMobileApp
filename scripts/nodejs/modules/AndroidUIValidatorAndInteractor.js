const { execOut, execOutRaw, execOk } = require('../core/Pluct-Test-Core-Exec');
const { logInfo, logWarn, logError, logSuccess } = require('../core/Logger');
const { recordUiAction } = require('./Pluct-Test-Core-Status');
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

const selectorCoverageCounters = {
    resourceIdMatches: 0,
    contentDescMatches: 0,
    textMatches: 0,
    classMatches: 0,
};

function centerOf(bounds) {
    const m = /\[(\d+),(\d+)\]\[(\d+),(\d+)\]/.exec(bounds || '');
    if (!m) return null;
    const l = +m[1], t = +m[2], r = +m[3], b = +m[4];
    return { x: Math.floor((l + r) / 2), y: Math.floor((t + b) / 2) };
}

function printUiInventory(tag, limit = 40) {
    try {
        const xmlRaw = getUIHierarchy(5) || '';
        const nodes = [];
        const re = /<node[^>]*>/ig;
        let m;
        while ((m = re.exec(xmlRaw))) {
            const s = m[0];
            const cls = /class="([^"]+)"/i.exec(s);
            if (!cls) continue;
            const rid = /resource-id="([^"]*)"/i.exec(s);
            const desc = /content-desc="([^"]*)"/i.exec(s);
            const text = /text="([^"]*)"/i.exec(s);
            const b = /bounds="(\[[^\]]+\]\[[^\]]+\])"/i.exec(s);
            const clk = /clickable="(true|false)"/i.exec(s);
            nodes.push({
                class: cls[1],
                rid: rid ? rid[1] : '',
                desc: desc ? desc[1] : '',
                text: text ? text[1] : '',
                bounds: b ? b[1] : '',
                clickable: clk ? clk[1] === 'true' : false,
            });
        }
        const items = nodes.map((n, i) => ({ i, id: n.rid, desc: n.desc, text: n.text, cls: n.class, b: n.bounds, cxcy: centerOf(n.bounds), clk: n.clickable }));
        const head = `\n[UI] Inventory @ ${tag} (showing ${Math.min(limit, items.length)} of ${items.length})`;
        logInfo(head, 'UIValidator');
        items.slice(0, limit).forEach(r => {
            const clsShort = (r.cls || '').split('.').pop();
            const line = ` #${r.i} id=${r.id||'-'} desc=${r.desc||'-'} text=${(r.text||'-').slice(0,36)} cls=${clsShort} b=${r.b} clk=${r.clk}`;
            logInfo(line, 'UIValidator');
        });
        try { recordUiAction('inventory', { tag, count: items.length }); } catch {}
        return items;
    } catch (e) {
        logError(`UI inventory failed @ ${tag}: ${e.message}`, 'UIValidator');
        return [];
    }
}

function getUIHierarchy(retries = 5) {
    const backoffMs = [500, 1000, 2000, 3000, 5000];
    for (let i = 0; i < retries; i++) {
        const dump = execOut('adb exec-out uiautomator dump /dev/tty');
        if (dump && dump.includes('<?xml')) return dump;
        execOk('adb shell input keyevent 4');
        const wait = backoffMs[Math.min(i, backoffMs.length - 1)];
        Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, wait);
    }
    return '';
}

function findElements(uiXml, type, value) {
    try {
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
    logInfo(`ADB TAP via bounds clickable at (${x},${y})`, 'UIValidator');
    try { recordUiAction('tap.clickable', { x, y }); } catch {}
    return execOk(`adb shell input tap ${x} ${y}`);
}

function tapByText(text) {
    const xml = getUIHierarchy(5);
    if (!xml) return false;
    const esc = String(text).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    let pattern = new RegExp(`text="${esc}"[\s\S]*?bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"`, 'i');
    let m = pattern.exec(xml);
    if (!m) {
        // fallback: contains match, ignore case
        const contains = new RegExp(`text="[^"]*${esc}[^"]*"[\s\S]*?bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"`, 'i');
        m = contains.exec(xml);
        if (!m) return false;
    }
    const bx1 = parseInt(m[1],10), by1 = parseInt(m[2],10), bx2 = parseInt(m[3],10), by2 = parseInt(m[4],10);
    // Prefer tapping a clickable container that contains this text bounds
    const clickableRe = /<node[^>]*clickable="true"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/ig;
    let cm; let chosen = null;
    while ((cm = clickableRe.exec(xml))) {
        const cx1 = parseInt(cm[1],10), cy1 = parseInt(cm[2],10), cx2 = parseInt(cm[3],10), cy2 = parseInt(cm[4],10);
        if (cx1 <= bx1 && cy1 <= by1 && cx2 >= bx2 && cy2 >= by2) { chosen = { x1: cx1, y1: cy1, x2: cx2, y2: cy2 }; break; }
    }
    const tx = Math.floor((bx1 + bx2) / 2);
    const ty = Math.floor((by1 + by2) / 2);
    const x = chosen ? Math.floor((chosen.x1 + chosen.x2) / 2) : tx;
    const y = chosen ? Math.floor((chosen.y1 + chosen.y2) / 2) : ty;
    logInfo(`ADB TAP via text='${text}' at (${x},${y})`, 'UIValidator');
    try { recordUiAction('tap.text', { text, x, y, via: chosen ? 'container' : 'text-bounds' }); } catch {}
    return execOk(`adb shell input tap ${x} ${y}`);
}

function tapByTextLoose(text) {
    const xml = getUIHierarchy(5);
    if (!xml) return false;
    const needle = String(text);
    const idx = xml.toLowerCase().indexOf(needle.toLowerCase());
    if (idx < 0) return false;
    // find nearest bounds="[l,t][r,b]" preceding the text occurrence
    const before = xml.slice(0, idx);
    const m = /bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"(?![\s\S]*bounds=")/i.exec(before);
    if (!m) return false;
    const x = Math.floor((parseInt(m[1],10) + parseInt(m[3],10)) / 2);
    const y = Math.floor((parseInt(m[2],10) + parseInt(m[4],10)) / 2);
    logInfo(`ADB TAP via textLoose='${text}' at (${x},${y})`, 'UIValidator');
    try { recordUiAction('tap.textLoose', { text, x, y }); } catch {}
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
    logInfo(`ADB TAP via content-desc='${desc}' at (${x},${y})`, 'UIValidator');
    try { recordUiAction('tap.contentDesc', { desc, x, y }); } catch {}
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
    logInfo(`ADB TAP via resource-id='${resourceId}' at (${x},${y})`, 'UIValidator');
    try { recordUiAction('tap.resourceId', { resourceId, x, y }); } catch {}
    return execOk(`adb shell input tap ${x} ${y}`);
}

function tapByResourceIdSuffix(suffix) {
    const xml = getUIHierarchy(3);
    if (!xml) return false;
    const esc = String(suffix).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const re = new RegExp(`resource-id="([^\"]*${esc})"[\s\S]*?bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"`, 'i');
    const m = re.exec(xml);
    if (!m) return false;
    const rid = m[1];
    const x = Math.floor((parseInt(m[2],10) + parseInt(m[4],10)) / 2);
    const y = Math.floor((parseInt(m[3],10) + parseInt(m[5],10)) / 2);
    try { recordUiAction('tap.resourceIdSuffix', { suffix, resourceId: rid, x, y }); } catch {}
    return execOk(`adb shell input tap ${x} ${y}`);
}

function clickDeterministic(sel) {
    const tried = [];
    if (sel && sel.resourceId) { if (tapByResourceId(sel.resourceId)) return { ok: true, via: 'resourceId', val: sel.resourceId }; tried.push('resourceId'); }
    if (sel && sel.resourceIdSuffix) { if (tapByResourceIdSuffix(sel.resourceIdSuffix)) return { ok: true, via: 'resourceIdSuffix', val: sel.resourceIdSuffix }; tried.push('resourceIdSuffix'); }
    if (sel && sel.contentDesc) { if (tapByContentDesc(sel.contentDesc)) return { ok: true, via: 'contentDesc', val: sel.contentDesc }; tried.push('contentDesc'); }
    if (sel && sel.text) { if (tapByText(sel.text) || tapByTextLoose(sel.text)) return { ok: true, via: 'text', val: sel.text }; tried.push('text'); }
    if (clickFirstClickable()) return { ok: true, via: 'fallbackClickable', val: '(first clickable)' };
    return { ok: false, via: 'none', tried };
}

function compareUiCounts(tagA, tagB) {
    const a = printUiInventory(tagA, 0) || [];
    const b = printUiInventory(tagB, 0) || [];
    return { before: a.length, after: b.length, delta: (b.length - a.length) };
}

function validateComponents(stepName, expectedComponents) {
    logInfo(`Validating UI components for step: ${stepName}`, 'UIValidator');
    const xml = getUIHierarchy(5);
    if (!xml) {
        logError('UI hierarchy not available', 'UIValidator');
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
            if (matches > 0) {
                found = true;
                if (sel.Type === 'resource-id') selectorCoverageCounters.resourceIdMatches++;
                else if (sel.Type === 'content-desc') selectorCoverageCounters.contentDescMatches++;
                else if (sel.Type === 'text') selectorCoverageCounters.textMatches++;
                else if (sel.Type === 'class') selectorCoverageCounters.classMatches++;
            }
        }
        if (found) { logSuccess(`Component '${comp}' validation PASSED`, 'UIValidator'); }
        else {
            logError(`Component '${comp}' validation FAILED`, 'UIValidator');
            // Heuristic drift detection: look for near matches
            try {
                const candidates = [];
                const navHost = /class="androidx\.navigation\.fragment\.NavHostFragment"[\s\S]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/i.exec(xml);
                if (navHost) candidates.push({ hint: 'NavHostFragment', bounds: navHost.slice(1,5).join(',') });
                const topClickable = /bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*clickable="true"/ig;
                let m; let count = 0;
                while ((m = topClickable.exec(xml)) && count < 3) { candidates.push({ hint: 'Clickable', bounds: m.slice(1,5).join(',') }); count++; }
                if (candidates.length) {
                    const ts = Date.now();
                    const driftLog = `DRIFT SUSPECTED for '${comp}'\nCandidates: ${JSON.stringify(candidates)}\n`;
                    const dir = path.join('artifacts','logs');
                    try { fs.mkdirSync(dir, { recursive: true }); } catch {}
                    fs.writeFileSync(path.join(dir, `drift-${stepName}-${ts}.log`), driftLog, 'utf8');
                    logWarn(`DRIFT SUSPECTED for '${comp}': ${JSON.stringify(candidates)}`, 'UIValidator');
                }
            } catch {}
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
        try { recordUiAction('capture.ui', { tag: safeTag }); } catch {}
        return true;
    } catch { return false; }
}

module.exports = {
    getUIHierarchy,
    waitForElement,
    validateComponents,
    clickByBoundsFromXml,
    clickFirstClickable,
    tapByText,
    tapByContentDesc,
    tapByResourceId,
    tapByResourceIdSuffix,
    clickDeterministic,
    printUiInventory,
    centerOf,
    tapByTextLoose,
    compareUiCounts,
    UIElements,
    captureUiArtifacts,
    selectorCoverageCounters,
};


