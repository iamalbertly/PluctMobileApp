const { execSync } = require('child_process');
const fs = require('fs');

function execOut(cmd) {
	try {
		const out = execSync(cmd, { stdio: ['ignore', 'pipe', 'pipe'] });
		return out.toString();
	} catch (e) {
		return '';
	}
}

function execOutRaw(cmd) {
    try {
        const out = execSync(cmd, { stdio: ['ignore', 'pipe', 'pipe'] });
        return out; // Buffer
    } catch (e) {
        return Buffer.from('');
    }
}

function execOk(cmd) {
	try {
		execSync(cmd, { stdio: ['ignore', 'ignore', 'ignore'] });
		return true;
	} catch (e) {
		return false;
	}
}

function which(bin) {
	try {
		if (process.platform === 'win32') {
			return !!execOut(`where ${bin}`).trim();
		}
		return !!execOut(`which ${bin}`).trim();
	} catch {
		return false;
	}
}

function fileExists(p) { try { return fs.existsSync(p); } catch { return false; } }

module.exports = { execOut, execOutRaw, execOk, which, fileExists };


