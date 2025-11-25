/**
 * Pluct-Core-01Foundation-04Validation - Validation module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Handles environment validation and system checks
 */
class PluctCoreFoundationValidation {
    constructor(config, logger, commands) {
        this.config = config;
        this.logger = logger;
        this.commands = commands;
    }

    /**
     * Validate environment
     */
    async validateEnvironment() {
        try {
            this.logger.info('Validating environment...');
            
            // Check ADB connectivity
            const adbResult = await this.checkADBConnectivity();
            if (!adbResult.success) {
                return { success: false, error: 'ADB connectivity check failed' };
            }
            
            // Check device status
            const deviceResult = await this.checkDeviceStatus();
            if (!deviceResult.success) {
                return { success: false, error: 'Device status check failed' };
            }
            
            // Check app installation
            const appResult = await this.checkAppInstallation();
            if (!appResult.success) {
                return { success: false, error: 'App installation check failed' };
            }
            
            this.logger.info('Environment validation passed');
            return { 
                success: true, 
                details: { 
                    adb: adbResult, 
                    device: deviceResult, 
                    app: appResult 
                } 
            };
        } catch (error) {
            this.logger.error(`Environment validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Check ADB connectivity
     */
    async checkADBConnectivity() {
        try {
            const result = await this.commands.executeCommand('adb devices');
            if (result.success && result.output.includes('device')) {
                return { success: true, message: 'ADB connected' };
            }
            return { success: false, error: 'No ADB devices found' };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Check device status
     */
    async checkDeviceStatus() {
        try {
            // Get first available device
            const devicesResult = await this.commands.executeCommand('adb devices');
            const deviceMatch = devicesResult.output?.match(/^([^\s]+)\s+device$/m);
            const deviceId = deviceMatch ? deviceMatch[1] : null;
            const adbPrefix = deviceId ? `adb -s ${deviceId}` : 'adb';
            
            const result = await this.commands.executeCommand(`${adbPrefix} shell getprop ro.build.version.release`);
            if (result.success && result.output.trim()) {
                return { success: true, androidVersion: result.output.trim() };
            }
            return { success: false, error: 'Device status check failed' };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    /**
     * Check app installation
     */
    async checkAppInstallation() {
        try {
            const result = await this.commands.executeCommand('adb shell pm list packages | findstr pluct');
            if (result.success && result.output.includes('app.pluct')) {
                return { success: true, message: 'App installed' };
            }
            return { success: false, error: 'App not installed' };
        } catch (error) {
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctCoreFoundationValidation;
