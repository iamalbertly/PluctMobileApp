const { execOut } = require('../core/Exec');

function isDeviceConnected() {
	const out = execOut('adb devices');
	return /\bdevice\b/.test(out);
}

module.exports = { isDeviceConnected };


