const { execOut } = require('../core/Pluct-Test-Core-Exec');

function isDeviceConnected() {
    const out = execOut('adb devices');
    return /\bdevice\b/.test(out);
}

module.exports = { isDeviceConnected };


