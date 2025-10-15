const { execOk, execOut } = require('./Pluct-Test-Core-Exec');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const { logInfo } = require('./Logger');

function clear() { return execOk('adb shell logcat -c'); }
function dump() { return execOut('adb shell logcat -d'); }
function recent(filter, last = 50) {
    const out = dump();
    if (!out) return [];
    const re = new RegExp(filter, 'i');
    const lines = out.split(/\r?\n/).filter(l => re.test(l));
    return lines.slice(-last);
}
function waitForPattern(pattern, timeoutSec = 30, progressEvery = 5) {
    const re = new RegExp(pattern, 'i');
    const deadline = Date.now() + timeoutSec * 1000;
    let attempts = 0;
    while (Date.now() < deadline) {
        attempts++;
        const out = dump();
        if (re.test(out)) {
            const lines = out.split(/\r?\n/).filter(l => re.test(l));
            return { found: true, attempts, lines: lines.slice(-10) };
        }
        if (attempts % progressEvery === 0) {
            // heartbeat
        }
        Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 1000);
    }
    const out = dump();
    const lines = out ? out.split(/\r?\n/).filter(l => re.test(l)).slice(-10) : [];
    return { found: false, attempts, lines };
}

function saveRecent(filter, outFile, last = 200) {
    try {
        const lines = recent(filter, last);
        const dir = path.dirname(outFile);
        try { fs.mkdirSync(dir, { recursive: true }); } catch {}
        fs.writeFileSync(outFile, lines.join('\n'), 'utf8');
        return true;
    } catch { return false; }
}

let liveProc = null;
let liveWriteStream = null;
let recentHttpDetailLines = [];

function startLive(filter, outFile) {
    try {
        stopLive();
        const dir = path.dirname(outFile);
        try { fs.mkdirSync(dir, { recursive: true }); } catch {}
        liveWriteStream = fs.createWriteStream(outFile, { flags: 'a' });
        liveProc = spawn('adb', ['logcat']);
        const re = filter ? new RegExp(filter, 'i') : null;
        liveProc.stdout.on('data', (buf) => {
            const text = buf.toString();
            const lines = text.split(/\r?\n/);
            for (const line of lines) {
                if (!line) continue;
                if (!re || re.test(line)) {
                    try { liveWriteStream.write(line + '\n'); } catch {}
                    logInfo(line, 'Logcat');
                    try {
                        const redacted = line.replace(/(Authorization:\s*Bearer\s+)[A-Za-z0-9\-_.+=/]+/i, '$1<redacted>');
                        const reqMatch = /HTTP REQUEST\s+(GET|POST|PUT|PATCH|DELETE)\s+([^\s]+)(?:.*?content[- ]length[:=]\s*(\d+))?/i.exec(redacted);
                        if (reqMatch) {
                            const method = reqMatch[1];
                            const url = reqMatch[2];
                            const clen = reqMatch[3] || '';
                            logInfo(`REQUEST ${method} ${url} len=${clen}`, 'HTTP');
                            recentHttpDetailLines.push(`[REQ] ${redacted}`);
                            if (recentHttpDetailLines.length > 500) recentHttpDetailLines = recentHttpDetailLines.slice(-500);
                            continue;
                        }
                        const resMatch = /HTTP RESPONSE\s+(\d{3})(?:\s+in\s+(\d+\s*ms))?(?:.*?content[- ]length[:=]\s*(\d+))?/i.exec(redacted);
                        if (resMatch) {
                            const status = resMatch[1];
                            const dur = resMatch[2] || '';
                            const clen = resMatch[3] || '';
                            logInfo(`RESPONSE ${status}${dur ? ' ' + dur : ''} len=${clen}`, 'HTTP');
                            recentHttpDetailLines.push(`[RES] ${redacted}`);
                            if (recentHttpDetailLines.length > 500) recentHttpDetailLines = recentHttpDetailLines.slice(-500);
                            continue;
                        }
                        if (/(Headers?:|Payload:|Body:|Content[- ]Type:|Host:|User-Agent:)/i.test(redacted)) {
                            const sanitized = redacted.replace(/(Authorization:\s*Bearer\s+)[A-Za-z0-9\-_.+=/]+/i, '$1<redacted>');
                            logInfo(sanitized, 'HTTP');
                            recentHttpDetailLines.push(sanitized);
                            if (recentHttpDetailLines.length > 500) recentHttpDetailLines = recentHttpDetailLines.slice(-500);
                            continue;
                        }
                        if (/(REQUEST_SUBMITTED|ttt\/transcribe|proxy|am_start|START u0|android\.intent\.action\.SEND)/i.test(redacted)) {
                            logInfo(redacted, 'HTTP');
                            recentHttpDetailLines.push(redacted);
                            if (recentHttpDetailLines.length > 500) recentHttpDetailLines = recentHttpDetailLines.slice(-500);
                        }
                    } catch {}
                }
            }
        });
        liveProc.stderr.on('data', (buf) => { const text = buf.toString(); if (text) { try { liveWriteStream.write('[stderr] ' + text); } catch {} } });
        liveProc.on('close', () => { stopLive(); });
        return true;
    } catch { stopLive(); return false; }
}

function stopLive() {
    try { if (liveProc) { try { liveProc.kill(); } catch {} } } catch {}
    liveProc = null;
    try { if (liveWriteStream) { try { liveWriteStream.end(); } catch {} } } catch {}
    liveWriteStream = null;
}

function recentHttpDetails(limit = 100) {
    try {
        return recentHttpDetailLines.slice(-limit);
    } catch { return []; }
}

function saveRecentHttpDetails(outFile, limit = 300) {
    try {
        const lines = recentHttpDetails(limit);
        const dir = path.dirname(outFile);
        try { fs.mkdirSync(dir, { recursive: true }); } catch {}
        fs.writeFileSync(outFile, lines.join('\n'), 'utf8');
        return true;
    } catch { return false; }
}

module.exports = { clear, dump, recent, waitForPattern, saveRecent, startLive, stopLive, recentHttpDetails, saveRecentHttpDetails };


