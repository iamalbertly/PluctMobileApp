function ts() {
	const d = new Date();
	return d.toTimeString().split(' ')[0] + '.' + String(d.getMilliseconds()).padStart(3, '0');
}

const mode = process.env.PLUCT_VERBOSE === '1' || process.env.PLUCT_LOG_MODE === 'verbose'
	? 'verbose'
	: 'minimal';

function isVerbose() {
	return mode === 'verbose';
}

function shouldLog(level, message) {
	if (level === 'ERROR' || level === 'WARN') return true;
	if (level === 'STAGE') return true;
	if (isVerbose()) return true;

	// Minimal mode: only show current test and step progression.
	const normalized = message.trim();
	return (
		normalized.startsWith('- Step') ||
		normalized.startsWith('🎯 Running test:') ||
		normalized.startsWith('🎯 Running journey:') ||
		normalized.includes('Starting: Journey-') ||
		normalized.includes('Starting: TikTok') ||
		normalized.includes('Starting: Journey')
	);
}

function log(level, message, component) {
	if (!shouldLog(level, message)) return;
	const comp = component ? ` (${component})` : '';
	console.log(`[${ts()}] [${level}]${comp} ${message}`);
}

function logInfo(msg, component) { log('INFO', msg, component); }
function logSuccess(msg, component) { log('SUCCESS', msg, component); }
function logWarn(msg, component) { log('WARN', msg, component); }
function logError(msg, component) { log('ERROR', msg, component); }
function logStage(stage, component) { log('STAGE', `${stage}`, component); }

module.exports = { logInfo, logSuccess, logWarn, logError, logStage, isVerbose };


