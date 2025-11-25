/**
 * Pluct-Core-01Foundation-02Commands - Command execution module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Handles command execution and system operations
 */
class PluctCoreFoundationCommands {
    constructor(config, logger) {
        this.config = config;
        this.logger = logger;
    }

    /**
     * Execute command with error handling
     * Auto-selects first device if multiple devices are connected
     */
    async executeCommand(command, timeout = this.config.timeouts.default) {
        try {
            // Auto-prefix adb commands with device selection if multiple devices exist
            if (command.startsWith('adb ') && !command.includes(' -s ')) {
                const devicesResult = await this._executeCommandDirect('adb devices', timeout);
                const deviceLines = devicesResult.output?.split('\n').filter(line => 
                    line.trim() && line.includes('device') && !line.includes('List of devices')
                ) || [];
                if (deviceLines.length > 1) {
                    // Prefer emulator over physical device
                    let selectedDevice = null;
                    for (const line of deviceLines) {
                        const deviceId = line.split(/\s+/)[0];
                        if (deviceId.startsWith('emulator-')) {
                            selectedDevice = deviceId;
                            break;
                        }
                    }
                    // If no emulator, use first device
                    if (!selectedDevice) {
                        selectedDevice = deviceLines[0].split(/\s+/)[0];
                    }
                    command = command.replace('adb ', `adb -s ${selectedDevice} `);
                    this.logger.info(`Multiple devices detected, using: ${selectedDevice}`);
                }
            }
            
            return await this._executeCommandDirect(command, timeout);
        } catch (error) {
            this.logger.error(`(${error.constructor.name}) ${error.message}`);
            return { success: false, error: error.message, command: command };
        }
    }
    
    /**
     * Internal method to execute command directly (without device selection logic)
     */
    async _executeCommandDirect(command, timeout) {
        const { exec } = require('child_process');
        const { promisify } = require('util');
        const execAsync = promisify(exec);
        
        const { stdout, stderr } = await execAsync(command, { timeout });
        
        // Command succeeded if no error was thrown
        return { 
            success: true, 
            output: stdout, 
            error: stderr,
            fullOutput: stdout + stderr
        };
    }

    /**
     * Clear app cache
     */
    async clearAppCache() {
        try {
            this.logger.info('Clearing app cache...');
            const result = await this.executeCommand('adb shell pm clear app.pluct');
            
            if (result.success) {
                this.logger.info('App cache cleared successfully');
                await this.sleep(2000); // Wait for cache clear to complete
            }
            
            return result;
        } catch (error) {
            this.logger.error(`Cache clear failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Clear WorkManager tasks
     */
    async clearWorkManagerTasks() {
        try {
            this.logger.info('Clearing WorkManager tasks...');
            // Note: This command may fail if no jobs exist - that's expected
            const result = await this.executeCommand('adb shell dumpsys jobscheduler');
            
            if (result.success && result.output && result.output.toLowerCase().includes('pluct')) {
                this.logger.info('WorkManager tasks found, clearing...');
                await this.executeCommand('adb shell cmd jobscheduler cancel-all app.pluct');
                this.logger.info('WorkManager tasks cleared successfully');
            } else {
                this.logger.info('No WorkManager tasks found (this is normal)');
            }
            
            return { success: true };
        } catch (error) {
            // Log but don't fail - missing WorkManager tasks is not an error
            this.logger.warn(`(${error.constructor.name}) ${error.message}`);
            return { success: true }; // Return success anyway
        }
    }

    /**
     * Sleep utility
     */
    async sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

module.exports = PluctCoreFoundationCommands;
