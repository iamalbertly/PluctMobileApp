/**
 * Pluct-Core-01Foundation-03Execution - Command execution functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Adheres to 300-line limit with smart separation of concerns
 */

class PluctCoreFoundationExecution {
    constructor() {
        this.logger = new PluctLogger();
        this.executionHistory = [];
        this.activeProcesses = new Map();
    }

    /**
     * Execute command with enhanced error handling
     */
    async executeCommand(command, options = {}) {
        const startTime = Date.now();
        const timeout = options.timeout || 5000;
        const retries = options.retries || 0;
        const retryDelay = options.retryDelay || 1000;
        
        this.logger.info(`🔧 Executing: ${command}`);
        
        for (let attempt = 0; attempt <= retries; attempt++) {
            try {
                const result = await this.executeCommandInternal(command, timeout);
                const duration = Date.now() - startTime;
                
                this.executionHistory.push({
                    command,
                    result,
                    duration,
                    attempt: attempt + 1,
                    timestamp: new Date().toISOString()
                });
                
                if (result.success) {
                    this.logger.info(`✅ Command executed successfully (${duration}ms)`);
                    return result;
                } else if (attempt < retries) {
                    this.logger.warn(`⚠️ Command failed, retrying in ${retryDelay}ms (attempt ${attempt + 1}/${retries + 1})`);
                    await this.sleep(retryDelay);
                } else {
                    this.logger.error(`❌ Command failed after ${retries + 1} attempts`);
                    return result;
                }
            } catch (error) {
                if (attempt < retries) {
                    this.logger.warn(`⚠️ Command error, retrying in ${retryDelay}ms (attempt ${attempt + 1}/${retries + 1}): ${error.message}`);
                    await this.sleep(retryDelay);
                } else {
                    this.logger.error(`❌ Command failed after ${retries + 1} attempts: ${error.message}`);
                    return { success: false, error: error.message };
                }
            }
        }
    }

    /**
     * Execute command internally
     */
    async executeCommandInternal(command, timeout) {
        try {
            const { exec } = require('child_process');
            const { promisify } = require('util');
            const execAsync = promisify(exec);
            
            const { stdout, stderr } = await execAsync(command, { timeout });
            
            return { 
                success: true, 
                output: stdout, 
                error: stderr,
                fullOutput: stdout + stderr
            };
        } catch (error) {
            return { 
                success: false, 
                error: error.message,
                output: error.stdout || '',
                fullOutput: (error.stdout || '') + (error.stderr || '')
            };
        }
    }

    /**
     * Execute command in background
     */
    async executeCommandBackground(command, options = {}) {
        const processId = `bg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
        
        this.logger.info(`🔄 Starting background process: ${processId}`);
        
        try {
            const { spawn } = require('child_process');
            const process = spawn(command, { shell: true });
            
            this.activeProcesses.set(processId, {
                process,
                command,
                startTime: Date.now(),
                options
            });
            
            return { success: true, processId };
        } catch (error) {
            this.logger.error(`❌ Failed to start background process: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Stop background process
     */
    async stopBackgroundProcess(processId) {
        const processInfo = this.activeProcesses.get(processId);
        if (!processInfo) {
            this.logger.warn(`⚠️ Background process not found: ${processId}`);
            return { success: false, error: 'Process not found' };
        }
        
        try {
            processInfo.process.kill();
            this.activeProcesses.delete(processId);
            
            const duration = Date.now() - processInfo.startTime;
            this.logger.info(`✅ Background process stopped: ${processId} (${duration}ms)`);
            
            return { success: true };
        } catch (error) {
            this.logger.error(`❌ Failed to stop background process: ${error.message}`);
            return { success: false, error: error.message };
        }
    }

    /**
     * Execute command with timeout
     */
    async executeCommandWithTimeout(command, timeout = 5000) {
        return this.executeCommand(command, { timeout, retries: 0 });
    }

    /**
     * Execute command with retries
     */
    async executeCommandWithRetries(command, retries = 3, retryDelay = 1000) {
        return this.executeCommand(command, { retries, retryDelay });
    }

    /**
     * Execute multiple commands in sequence
     */
    async executeCommandsSequence(commands, options = {}) {
        const results = [];
        const stopOnError = options.stopOnError !== false;
        
        this.logger.info(`🔄 Executing ${commands.length} commands in sequence`);
        
        for (let i = 0; i < commands.length; i++) {
            const command = commands[i];
            this.logger.info(`📝 Command ${i + 1}/${commands.length}: ${command}`);
            
            const result = await this.executeCommand(command, options);
            results.push({ command, result });
            
            if (!result.success && stopOnError) {
                this.logger.error(`❌ Command ${i + 1} failed, stopping sequence`);
                break;
            }
        }
        
        return results;
    }

    /**
     * Execute multiple commands in parallel
     */
    async executeCommandsParallel(commands, options = {}) {
        this.logger.info(`🔄 Executing ${commands.length} commands in parallel`);
        
        const promises = commands.map(command => 
            this.executeCommand(command, options)
        );
        
        const results = await Promise.all(promises);
        
        return commands.map((command, index) => ({
            command,
            result: results[index]
        }));
    }

    /**
     * Get execution history
     */
    getExecutionHistory() {
        return this.executionHistory;
    }

    /**
     * Get active processes
     */
    getActiveProcesses() {
        return Array.from(this.activeProcesses.entries()).map(([id, info]) => ({
            id,
            command: info.command,
            startTime: info.startTime,
            duration: Date.now() - info.startTime
        }));
    }

    /**
     * Clean up active processes
     */
    async cleanupActiveProcesses() {
        this.logger.info('🧹 Cleaning up active processes...');
        
        const processes = Array.from(this.activeProcesses.entries());
        for (const [processId, processInfo] of processes) {
            try {
                processInfo.process.kill();
                this.activeProcesses.delete(processId);
                this.logger.info(`✅ Cleaned up process: ${processId}`);
            } catch (error) {
                this.logger.warn(`⚠️ Failed to cleanup process ${processId}: ${error.message}`);
            }
        }
    }

    /**
     * Sleep utility
     */
    async sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

module.exports = PluctCoreFoundationExecution;
