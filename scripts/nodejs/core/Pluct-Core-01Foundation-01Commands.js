/**
 * Pluct-Core-01Foundation-01Commands - Command execution functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Adheres to 300-line limit with smart separation of concerns
 */

class PluctCoreFoundationCommands {
    constructor() {
        this.config = {
            timeouts: { default: 5000, short: 2000, long: 10000 },
            retry: { maxAttempts: 3, delay: 1000 }
        };
        this.logger = new PluctLogger();
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
     * Execute command with retry logic
     */
    async executeCommandWithRetry(command, maxAttempts = this.config.retry.maxAttempts) {
        let lastError;
        
        for (let attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                this.logger.info(`Attempt ${attempt}/${maxAttempts}: ${command}`);
                const result = await this.executeCommand(command);
                
                if (result.success) {
                    return result;
                }
                
                lastError = result.error;
                
                if (attempt < maxAttempts) {
                    this.logger.warn(`Attempt ${attempt} failed, retrying in ${this.config.retry.delay}ms...`);
                    await this.sleep(this.config.retry.delay);
                }
            } catch (error) {
                lastError = error.message;
                if (attempt < maxAttempts) {
                    this.logger.warn(`Attempt ${attempt} failed with exception, retrying in ${this.config.retry.delay}ms...`);
                    await this.sleep(this.config.retry.delay);
                }
            }
        }
        
        this.logger.error(`All ${maxAttempts} attempts failed for command: ${command}`);
        return { success: false, error: lastError };
    }

    /**
     * Execute multiple commands in sequence
     */
    async executeCommandsSequentially(commands) {
        const results = [];
        
        for (const command of commands) {
            this.logger.info(`Executing: ${command}`);
            const result = await this.executeCommand(command);
            results.push({ command, result });
            
            if (!result.success) {
                this.logger.error(`Command failed: ${command}`);
                break;
            }
        }
        
        return results;
    }

    /**
     * Execute multiple commands in parallel
     */
    async executeCommandsParallel(commands) {
        this.logger.info(`Executing ${commands.length} commands in parallel`);
        
        const promises = commands.map(async (command) => {
            this.logger.info(`Executing: ${command}`);
            const result = await this.executeCommand(command);
            return { command, result };
        });
        
        return await Promise.all(promises);
    }

    /**
     * Check if command exists
     */
    async commandExists(command) {
        try {
            const result = await this.executeCommand(`where ${command}`, this.config.timeouts.short);
            return result.success;
        } catch (error) {
            return false;
        }
    }

    /**
     * Get command version
     */
    async getCommandVersion(command) {
        try {
            const result = await this.executeCommand(`${command} --version`, this.config.timeouts.short);
            if (result.success) {
                return result.output.trim();
            }
        } catch (error) {
            // Try alternative version flags
            try {
                const result = await this.executeCommand(`${command} -v`, this.config.timeouts.short);
                if (result.success) {
                    return result.output.trim();
                }
            } catch (error2) {
                // Try -V flag
                try {
                    const result = await this.executeCommand(`${command} -V`, this.config.timeouts.short);
                    if (result.success) {
                        return result.output.trim();
                    }
                } catch (error3) {
                    // Command doesn't support version flags
                }
            }
        }
        return null;
    }

    /**
     * Sleep utility
     */
    async sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

module.exports = PluctCoreFoundationCommands;
