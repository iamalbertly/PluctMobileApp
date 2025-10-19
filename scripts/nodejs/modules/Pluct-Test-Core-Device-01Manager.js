const { execOut } = require('../core/Pluct-Test-Core-Exec');

/**
 * Consolidated device manager for Pluct tests
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[increment][CoreResponsibility]
 */

function isDeviceConnected() {
    const out = execOut('adb devices');
    return /\bdevice\b/.test(out);
}

module.exports = { isDeviceConnected };
