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

            const bridgeResult = await this.configureLocalBusinessEngineBridge();
            if (!bridgeResult.success) {
                return { success: false, error: bridgeResult.error };
            }

            // Stop noisy foreground apps that interfere with UI automation (observed overlays from com.expensphere).
            await this.commands.executeCommand('adb shell am force-stop com.expensphere', undefined, undefined, { allowFailure: true });

            this.logger.info('Environment validation passed');
            return {
                success: true,
                details: {
                    adb: adbResult,
                    device: deviceResult,
                    app: appResult,
                    localBusinessEngineBridge: bridgeResult
                }
            };
        } catch (error) {
            this.logger.error(`Environment validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    async configureLocalBusinessEngineBridge() {
        const baseUrl = (this.config.businessEngineUrl || '').toLowerCase();
        const isLoopback = baseUrl.includes('://127.0.0.1:') || baseUrl.includes('://localhost:');
        if (!isLoopback) {
            return { success: true, skipped: true, reason: 'remote business engine' };
        }

        const portMatch = baseUrl.match(/:(\d+)(?:\/|$)/);
        const port = portMatch ? portMatch[1] : '8787';
        this.logger.info(`Configuring Android bridge for local Business Engine on port ${port}...`);

        const reverseResult = await this.commands.executeCommand(`adb reverse tcp:${port} tcp:${port}`, undefined, undefined, { allowFailure: true });
        if (!reverseResult.success) {
            return {
                success: false,
                error: `Unable to expose local Business Engine to Android device with adb reverse tcp:${port} tcp:${port}`
            };
        }

        return { success: true, port };
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
     * Uses alternative method if package listing fails due to permissions
     */
    async checkAppInstallation() {
        try {
            // Try package listing first
            const result = await this.commands.executeCommand('adb shell pm list packages | findstr pluct');
            if (result.success && result.output && result.output.includes('app.pluct')) {
                return { success: true, message: 'App installed' };
            }
            
            // If package listing fails, try to launch the app as a test
            // This works even if package listing has permission issues
            try {
                const launchResult = await this.commands.executeCommand('adb shell am start -n app.pluct/.PluctUIScreen01MainActivity');
                if (launchResult.success) {
                    // App can be launched, so it's installed
                    // Close it immediately to avoid interfering with tests
                    await this.commands.executeCommand('adb shell am force-stop app.pluct');
                    return { success: true, message: 'App installed (verified via launch)' };
                }
            } catch (launchError) {
                // Launch failed, app might not be installed
            }
            
            return { success: false, error: 'App not installed or cannot be accessed' };
        } catch (error) {
            // If all checks fail, try launch as fallback
            try {
                const launchResult = await this.commands.executeCommand('adb shell am start -n app.pluct/.PluctUIScreen01MainActivity');
                if (launchResult.success) {
                    await this.commands.executeCommand('adb shell am force-stop app.pluct');
                    return { success: true, message: 'App installed (verified via launch fallback)' };
                }
            } catch (launchError) {
                // Launch also failed
            }
            return { success: false, error: error.message };
        }
    }

    /**
     * Search logcat for specific terms (cross-platform alternative to findstr/grep)
     * @param {string[]} searchTerms - Array of terms to search for
     * @param {boolean} caseInsensitive - Whether to perform case-insensitive search
     * @returns {Promise<{success: boolean, matches: string[], found: boolean, error?: string}>}
     */
    async searchLogcat(searchTerms, caseInsensitive = true) {
        try {
            this.logger.info(`Searching logcat for: ${searchTerms.join(', ')}`);

            const result = await this.commands.executeCommand('adb logcat -d');
            if (!result.success) {
                return { success: false, error: 'Failed to get logcat', matches: [], found: false };
            }

            const logOutput = result.output || '';
            const matches = [];

            for (const term of searchTerms) {
                const searchText = caseInsensitive ? logOutput.toLowerCase() : logOutput;
                const searchTerm = caseInsensitive ? term.toLowerCase() : term;

                if (searchText.includes(searchTerm)) {
                    matches.push(term);
                    this.logger.info(`Found term in logcat: ${term}`);
                }
            }

            return { success: true, matches, found: matches.length > 0 };
        } catch (error) {
            this.logger.error(`Search logcat failed: ${error.message}`);
            return { success: false, error: error.message, matches: [], found: false };
        }
    }
}

module.exports = PluctCoreFoundationValidation;
