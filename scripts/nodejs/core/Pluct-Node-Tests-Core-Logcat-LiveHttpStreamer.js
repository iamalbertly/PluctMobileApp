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

// HTTP telemetry parsing
const HTTP_TAG = 'PLUCT_HTTP';
let _buf = [];
function clear(){ _buf = []; execOk('adb logcat -c'); }
function ingest() {
  const out = execOut('adb logcat -d');
  out.split('\n').forEach(l => {
    if (l.includes(HTTP_TAG)) {
      const jsonStr = l.substring(l.indexOf(HTTP_TAG)+HTTP_TAG.length+1).trim();
      try { _buf.push(JSON.parse(jsonStr)); } catch {}
    }
  });
}
function recent(pattern, limit=50){
  ingest();
  const re = new RegExp(pattern,'i');
  return _buf.filter(x =>
    (x.url && re.test(x.url)) ||
    (x.event && re.test(x.event)) ||
    (x.body && typeof x.body==='string' && re.test(x.body))
  ).slice(-limit);
}
function findLastHttpExchange(hint){
  ingest();
  // pair request/response by reqId
  const reqs = {};
  for (const x of _buf) {
    if (x.event==='request') reqs[x.reqId] = x;
    if (x.event==='response' && reqs[x.reqId]) {
      const pair = { req: reqs[x.reqId], res: x };
      if (!hint) return pair;
      const h = hint.toLowerCase();
      const url = (pair.req.url||'').toLowerCase();
      if (url.includes(h)) return pair;
    }
  }
  return null;
}
function saveRecentHttpDetails(file, limit=400){
  ingest();
  const lines = _buf.slice(-limit).map(o=>JSON.stringify(o)).join('\n');
  fs.mkdirSync(path.dirname(file), {recursive:true});
  fs.writeFileSync(file, lines, 'utf8');
}

function startLive(filter, outFile) {
    try {
        stopLive();
        // Only create file stream if outFile is provided
        if (outFile) {
            const dir = path.dirname(outFile);
            try { fs.mkdirSync(dir, { recursive: true }); } catch {}
            liveWriteStream = fs.createWriteStream(outFile, { flags: 'a' });
        }
        liveProc = spawn('adb', ['logcat']);
        const re = filter ? new RegExp(filter, 'i') : null;
        liveProc.stdout.on('data', (buf) => {
            const text = buf.toString();
            const lines = text.split(/\r?\n/);
            for (const line of lines) {
                if (!line) continue;
                if (!re || re.test(line)) {
                    // Only write to file if outFile was provided
                    if (liveWriteStream) {
                        try { liveWriteStream.write(line + '\n'); } catch {}
                    }
                    logInfo(line, 'Logcat');
                    try {
                        const redacted = line.replace(/(Authorization:\s*Bearer\s+)[A-Za-z0-9\-_.+=/]+/i, '$1<redacted>');
                                const reqMatch = /HTTP REQUEST\s+(GET|POST|PUT|PATCH|DELETE)\s+([^\s]+)(?:.*?content[- ]length[:=]\s*(\d+))?/i.exec(redacted);
                                if (reqMatch) {
                                    const method = reqMatch[1];
                                    const url = reqMatch[2];
                                    const clen = reqMatch[3] || '';
                                    logInfo(`[HTTP REQUEST] ${method} ${url} len=${clen}`, 'HTTP');
                                    logInfo(`[HTTP DETAIL] ${redacted}`, 'HTTP');
                                    recentHttpDetailLines.push(`[REQ] ${redacted}`);
                                    if (recentHttpDetailLines.length > 500) recentHttpDetailLines = recentHttpDetailLines.slice(-500);
                                    continue;
                                }
                                const resMatch = /HTTP RESPONSE\s+(\d{3})(?:\s+in\s+(\d+\s*ms))?(?:.*?content[- ]length[:=]\s*(\d+))?/i.exec(redacted);
                                if (resMatch) {
                                    const status = resMatch[1];
                                    const dur = resMatch[2] || '';
                                    const clen = resMatch[3] || '';
                                    logInfo(`[HTTP RESPONSE] ${status}${dur ? ' ' + dur : ''} len=${clen}`, 'HTTP');
                                    logInfo(`[HTTP DETAIL] ${redacted}`, 'HTTP');
                                    recentHttpDetailLines.push(`[RES] ${redacted}`);
                                    if (recentHttpDetailLines.length > 500) recentHttpDetailLines = recentHttpDetailLines.slice(-500);
                                    continue;
                                }
                                // Enhanced JSON parsing for PLUCT_HTTP logs
                                const pluctHttpMatch = /PLUCT_HTTP:\s*(\{.*\})/i.exec(redacted);
                                if (pluctHttpMatch) {
                                    try {
                                        const httpData = JSON.parse(pluctHttpMatch[1]);
                                        if (httpData.event === 'request') {
                                            logInfo(`[HTTP REQUEST] ${httpData.method} ${httpData.url}`, 'HTTP');
                                            logInfo(`[HTTP REQUEST] Headers: ${JSON.stringify(httpData.headers, null, 2)}`, 'HTTP');
                                            logInfo(`[HTTP REQUEST] Body: ${httpData.body}`, 'HTTP');
                                        } else if (httpData.event === 'response') {
                                            logInfo(`[HTTP RESPONSE] ${httpData.code} ${httpData.url}`, 'HTTP');
                                            logInfo(`[HTTP RESPONSE] Duration: ${httpData.duration}ms`, 'HTTP');
                                            logInfo(`[HTTP RESPONSE] Body: ${httpData.body}`, 'HTTP');
                                        } else if (httpData.event === 'error') {
                                            logInfo(`[HTTP ERROR] ${httpData.url}: ${httpData.error}`, 'HTTP');
                                        }
                                        recentHttpDetailLines.push(`[PLUCT_HTTP] ${redacted}`);
                                        if (recentHttpDetailLines.length > 500) recentHttpDetailLines = recentHttpDetailLines.slice(-500);
                                        continue;
                                    } catch (e) {
                                        // Fall through to regular processing
                                    }
                                }
                                
                                if (/(Headers?:|Payload:|Body:|Content[- ]Type:|Host:|User-Agent:)/i.test(redacted)) {
                                    const sanitized = redacted.replace(/(Authorization:\s*Bearer\s+)[A-Za-z0-9\-_.+=/]+/i, '$1<redacted>');
                                    logInfo(`[HTTP DETAIL] ${sanitized}`, 'HTTP');
                                    recentHttpDetailLines.push(sanitized);
                                    if (recentHttpDetailLines.length > 500) recentHttpDetailLines = recentHttpDetailLines.slice(-500);
                                    continue;
                                }
                                if (/(REQUEST_SUBMITTED|ttt\/transcribe|proxy|am_start|START u0|android\.intent\.action\.SEND)/i.test(redacted)) {
                                    logInfo(`[ACTIVITY] ${redacted}`, 'HTTP');
                                    recentHttpDetailLines.push(redacted);
                                    if (recentHttpDetailLines.length > 500) recentHttpDetailLines = recentHttpDetailLines.slice(-500);
                                }
                    } catch {}
                }
            }
        });
        liveProc.stderr.on('data', (buf) => { 
            const text = buf.toString(); 
            if (text) { 
                if (liveWriteStream) {
                    try { liveWriteStream.write('[stderr] ' + text); } catch {} 
                }
                logInfo(`[stderr] ${text}`, 'Logcat');
            } 
        });
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

module.exports = { clear, dump, recent, waitForPattern, saveRecent, startLive, stopLive, recentHttpDetails, saveRecentHttpDetails, findLastHttpExchange, saveRecentHttpDetails };


