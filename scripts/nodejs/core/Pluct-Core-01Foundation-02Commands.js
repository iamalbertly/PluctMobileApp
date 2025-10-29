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
     */
    async executeCommand(command, timeout = this.config.timeouts.default) {
        try {
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
        } catch (error) {
            this.logger.error(`Command failed: ${command}`, error);
            return { success: false, error: error.message };
        }
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
            const result = await this.executeCommand('adb shell dumpsys jobscheduler | findstr /i pluct');
            
            if (result.success && result.output.includes('pluct')) {
                this.logger.info('WorkManager tasks found, clearing...');
                await this.executeCommand('adb shell cmd jobscheduler cancel-all app.pluct');
            }
            
            return { success: true };
        } catch (error) {
            this.logger.error(`WorkManager clear failed: ${error.message}`);
            return { success: false, error: error.message };
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
