function ts() {
	const d = new Date();
	return d.toTimeString().split(' ')[0] + '.' + String(d.getMilliseconds()).padStart(3, '0');
}

function log(level, message, component) {
	const comp = component ? ` (${component})` : '';
	console.log(`[${ts()}] [${level}]${comp} ${message}`);
}

function logInfo(msg, component) { log('INFO', msg, component); }
function logSuccess(msg, component) { log('SUCCESS', msg, component); }
function logWarn(msg, component) { log('WARN', msg, component); }
function logError(msg, component) { log('ERROR', msg, component); }
function logStage(stage, component) { log('STAGE', `${stage}`, component); }

module.exports = { logInfo, logSuccess, logWarn, logError, logStage };


