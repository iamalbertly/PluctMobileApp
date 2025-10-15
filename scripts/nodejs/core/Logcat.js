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
			// no-op progress hint; caller logs if needed
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

// Live streaming support
let liveProc = null;
let liveWriteStream = null;

function startLive(filter, outFile) {
    try {
        stopLive();
        const dir = path.dirname(outFile);
        try { fs.mkdirSync(dir, { recursive: true }); } catch {}
        liveWriteStream = fs.createWriteStream(outFile, { flags: 'a' });
        const args = ['logcat'];
        liveProc = spawn('adb', args);
        const re = filter ? new RegExp(filter, 'i') : null;
        liveProc.stdout.on('data', (buf) => {
            const text = buf.toString();
            const lines = text.split(/\r?\n/);
            for (const line of lines) {
                if (!line) continue;
                if (!re || re.test(line)) {
                    try { liveWriteStream.write(line + '\n'); } catch {}
                    // Surface live to console with a compact prefix
                    logInfo(line, 'Logcat');
                    // Lightweight HTTP Request/Response surfacing
                    try {
                        if (/HTTP REQUEST/i.test(line)) {
                            logInfo(line, 'HTTP');
                        } else if (/HTTP RESPONSE/i.test(line)) {
                            logInfo(line, 'HTTP');
                        } else if (/(REQUEST_SUBMITTED|Authorization: Bearer|ttt\/transcribe|proxy)/i.test(line)) {
                            logInfo(line, 'HTTP');
                        }
                    } catch {}
                }
            }
        });
        liveProc.stderr.on('data', (buf) => {
            const text = buf.toString();
            if (text) {
                try { liveWriteStream.write('[stderr] ' + text); } catch {}
            }
        });
        liveProc.on('close', () => {
            stopLive();
        });
        return true;
    } catch {
        stopLive();
        return false;
    }
}

function stopLive() {
    try { if (liveProc) { try { liveProc.kill(); } catch {} } } catch {}
    liveProc = null;
    try { if (liveWriteStream) { try { liveWriteStream.end(); } catch {} } } catch {}
    liveWriteStream = null;
}

module.exports = { clear, dump, recent, waitForPattern, saveRecent, startLive, stopLive };



