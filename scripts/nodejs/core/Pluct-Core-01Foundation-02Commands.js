/**
 * Pluct-Core-01Foundation-02Commands - Command execution module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Handles command execution and system operations
 */
class PluctCoreFoundationCommands {
    constructor(config, logger) {
        this.config = config;
        this.logger = logger;
        this._adbConnectionChecked = false;
        this._adbConnected = false;
    }

    /**
     * Verify ADB connection before executing ADB commands
     */
    async _verifyAdbConnection(timeout = 5000) {
        // Cache connection status for 30 seconds
        if (this._adbConnectionChecked && this._adbConnected) {
            return true;
        }

        try {
            const result = await this._executeCommandDirect('adb devices', timeout);
            if (!result.success) {
                this.logger.error(`❌ ADB connection check failed: ${result.error}`);
                if (result.stderr) {
                    this.logger.error(`   ADB stderr: ${result.stderr}`);
                }
                if (result.output) {
                    this.logger.error(`   ADB stdout: ${result.output}`);
                }
                this._adbConnected = false;
                this._adbConnectionChecked = true;
                return false;
            }

            // Parse device list
            const deviceLines = (result.output || '').split('\n').filter(line => {
                const trimmed = line.trim();
                return trimmed && 
                       trimmed.includes('device') && 
                       !trimmed.includes('List of devices') &&
                       !trimmed.includes('daemon');
            });

            if (deviceLines.length === 0) {
                this.logger.error('❌ ADB connection check: No devices found');
                this.logger.error(`   ADB output: ${result.output || 'empty'}`);
                this.logger.error(`   Full output: ${result.fullOutput || 'empty'}`);
                this._adbConnected = false;
                this._adbConnectionChecked = true;
                return false;
            }

            // ADB connection verified
            this._adbConnected = true;
            this._adbConnectionChecked = true;
            // Reset cache after 30 seconds
            setTimeout(() => { this._adbConnectionChecked = false; }, 30000);
            return true;
        } catch (error) {
            this.logger.error(`❌ ADB connection check exception: ${error.message}`);
            if (error.stack) {
                this.logger.error(`   Stack: ${error.stack}`);
            }
            this._adbConnected = false;
            this._adbConnectionChecked = true;
            return false;
        }
    }

    /**
     * Execute command with error handling and retry logic
     * Auto-selects first device if multiple devices are connected
     */
    async executeCommand(command, timeout = this.config.timeouts.default, retries = 0) {
        const maxRetries = command.startsWith('adb ') ? 2 : 0; // Retry ADB commands up to 2 times
        
        try {
            // For ADB commands, verify connection first
            if (command.startsWith('adb ') && !command.includes(' -s ')) {
                const isConnected = await this._verifyAdbConnection(Math.min(timeout, 5000));
                if (!isConnected) {
                    const errorMsg = 'ADB device not connected or not responding. Check: 1) Device is connected via USB/WiFi, 2) USB debugging is enabled, 3) ADB server is running (try: adb kill-server && adb start-server)';
                    this.logger.error(`❌ ${errorMsg}`);
                    return { 
                        success: false, 
                        error: errorMsg,
                        command: command,
                        adbConnectionIssue: true
                    };
                }

                // Auto-prefix adb commands with device selection if multiple devices exist
                const devicesResult = await this._executeCommandDirect('adb devices', timeout);
                if (!devicesResult.success) {
                    const errorMsg = `Failed to list ADB devices: ${devicesResult.stderr || devicesResult.error || 'Unknown error'}`;
                    this.logger.error(`❌ ${errorMsg}`);
                    if (devicesResult.output) {
                        this.logger.error(`   Output: ${devicesResult.output}`);
                    }
                    return { 
                        success: false, 
                        error: errorMsg,
                        command: command,
                        stderr: devicesResult.stderr,
                        output: devicesResult.output
                    };
                }

                const deviceLines = (devicesResult.output || '').split('\n').filter(line => {
                    const trimmed = line.trim();
                    return trimmed && 
                           trimmed.includes('device') && 
                           !trimmed.includes('List of devices') &&
                           !trimmed.includes('daemon');
                });
                
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
                } else if (deviceLines.length === 1) {
                    const deviceId = deviceLines[0].split(/\s+/)[0];
                    // Using single ADB device
                }
            }
            
            // Execute with retry logic
            let lastError = null;
            for (let attempt = 0; attempt <= maxRetries; attempt++) {
                if (attempt > 0) {
                    const waitTime = Math.min(1000 * Math.pow(2, attempt - 1), 5000); // Exponential backoff, max 5s
                    this.logger.warn(`Retrying ADB command (attempt ${attempt + 1}/${maxRetries + 1}) after ${waitTime}ms...`);
                    await this.sleep(waitTime);
                }

                const result = await this._executeCommandDirect(command, timeout);
                
                // If successful, return immediately
                if (result.success) {
                    return result;
                }

                // Check if error is retryable
                const errorText = (result.stderr || result.error || '').toLowerCase();
                const isKillProcessError = result.errorCode === 137 || 
                                          errorText.includes('killprocess') ||
                                          errorText.includes('call killprocess');
                const isRetryable = isKillProcessError ||
                                   errorText.includes('timeout') || 
                                   errorText.includes('connection') ||
                                   errorText.includes('device offline') ||
                                   errorText.includes('no devices') ||
                                   errorText.includes('unauthorized');
                
                // For killProcess errors, add extra delay and cleanup
                if (isKillProcessError && attempt < maxRetries) {
                    this.logger.warn(`⚠️ Process was killed (Error 137), cleaning up and retrying...`);
                    // Try to kill any remaining uiautomator processes
                    try {
                        await this._executeCommandDirect('adb shell pkill -f uiautomator', 5000);
                        await this.sleep(1000);
                    } catch (e) {
                        // Ignore cleanup errors
                    }
                }

                if (!isRetryable || attempt >= maxRetries) {
                    // Not retryable or out of retries, return error
                    return result;
                }

                lastError = result;
            }

            return lastError || { success: false, error: 'Command failed after retries', command: command };
        } catch (error) {
            const errorMsg = `${error.message}${error.stderr ? ` | stderr: ${error.stderr}` : ''}${error.stdout ? ` | stdout: ${error.stdout}` : ''}`;
            this.logger.error(`(${error.constructor.name}) Command execution failed: ${errorMsg}`);
            return { 
                success: false, 
                error: errorMsg, 
                command: command,
                exception: error.message,
                stderr: error.stderr,
                stdout: error.stdout
            };
        }
    }
    
    /**
     * Internal method to execute command directly (without device selection logic)
     * Uses exec with fallback to spawn for better error handling
     */
    async _executeCommandDirect(command, timeout) {
        const { exec, spawn } = require('child_process');
        const { promisify } = require('util');
        const execAsync = promisify(exec);
        
        try {
            // Use shell option for Windows compatibility
            const { stdout, stderr } = await execAsync(command, { 
                timeout,
                maxBuffer: 10 * 1024 * 1024, // 10MB buffer for large outputs
                shell: true // Ensure shell is used for Windows compatibility
            });
            
            // Command succeeded if no error was thrown
            // Warnings in stderr are OK (e.g., "Activity not started, intent has been delivered")
            const stdoutStr = (stdout || '').toString();
            const stderrStr = (stderr || '').toString();
            
            // Check if output indicates success (some ADB commands output "Success")
            if (stdoutStr.toLowerCase().includes('success') || stderrStr.toLowerCase().includes('success')) {
                return { 
                    success: true, 
                    output: stdoutStr, 
                    error: stderrStr,
                    fullOutput: stdoutStr + stderrStr
                };
            }
            
            return { 
                success: true, 
                output: stdoutStr, 
                error: stderrStr,
                fullOutput: stdoutStr + stderrStr
            };
        } catch (error) {
            // Extract detailed error information - handle different error object structures
            const errorStderr = (error.stderr || '').toString().trim();
            const errorStdout = (error.stdout || '').toString().trim();
            const errorMessage = (error.message || '').toString();
            const errorCode = error.code || error.signal || '';
            const errorSignal = error.signal || '';
            
            // CRITICAL: Check if the command actually succeeded despite the error
            // Some ADB commands return "Success" in stdout/stderr even when exec throws
            const combinedOutput = (errorStdout + errorStderr).toLowerCase();
            if (combinedOutput.includes('success') && !combinedOutput.includes('error') && !combinedOutput.includes('failed')) {
                // Command reported success in output despite exec error
                return {
                    success: true,
                    output: errorStdout || errorStderr || '',
                    error: errorStderr || '',
                    fullOutput: (errorStdout || '') + (errorStderr || '')
                };
            }
            
            // Debug: Log the full error structure for troubleshooting
            if (command.startsWith('adb ') && !errorStderr && !errorStdout) {
                this.logger.warn(`⚠️ ADB command failed but no stderr/stdout captured. Error structure: ${JSON.stringify({
                    message: errorMessage,
                    code: errorCode,
                    signal: errorSignal,
                    hasStderr: !!error.stderr,
                    hasStdout: !!error.stdout,
                    errorKeys: Object.keys(error)
                })}`);
            }
            
            // Check if it's just a warning (non-fatal)
            const errorOutput = (errorStderr + errorStdout + errorMessage).toLowerCase();
            const isWarning = errorOutput.includes('warning:') || 
                             errorOutput.includes('activity not started, intent has been delivered');
            
            if (isWarning) {
                // Treat warnings as success
                return {
                    success: true,
                    output: errorStdout || errorStderr || '',
                    error: errorStderr || '',
                    fullOutput: (errorStdout || '') + (errorStderr || '')
                };
            }
            
            // CRITICAL: Handle findstr/grep commands that return exit code 1 when no matches found
            // This is normal behavior - exit code 1 means "no matches", not an error
            const isSearchCommand = command.includes('findstr') || command.includes('grep') || command.includes('find');
            if (isSearchCommand && errorCode === 1 && !errorStderr && !errorStdout) {
                // No matches found, but this is not an error - return success with empty output
                this.logger.info(`Search command returned no matches (exit code 1): ${command}`);
                return {
                    success: true,
                    output: '',
                    error: '',
                    fullOutput: '',
                    noMatches: true
                };
            }
            if (isSearchCommand && errorCode === 1 && errorStdout) {
                // Some matches found in stdout despite exit code 1 - treat as success
                this.logger.info(`Search command found matches despite exit code 1: ${command}`);
                return {
                    success: true,
                    output: errorStdout,
                    error: errorStderr || '',
                    fullOutput: (errorStdout || '') + (errorStderr || '')
                };
            }
            
            // Check for killProcess errors (Error Code 137 = SIGKILL)
            const isKillProcessError = errorCode === 137 || 
                                      errorCode === 'SIGKILL' ||
                                      errorMessage.includes('killProcess') ||
                                      errorStderr.includes('killProcess');
            
            // Build comprehensive error message
            let detailedError = errorMessage;
            if (isKillProcessError) {
                detailedError = `Process was killed by system (Error 137/SIGKILL): ${errorMessage}. This usually indicates memory pressure or timeout.`;
            } else if (errorCode === 'ETIMEDOUT' || errorMessage.includes('timeout')) {
                detailedError = `Command timed out after ${timeout}ms: ${errorMessage}`;
            } else if (errorSignal) {
                detailedError = `Command terminated by signal ${errorSignal}: ${errorMessage}`;
            } else if (errorStderr) {
                detailedError = `${errorMessage} | ADB Error: ${errorStderr}`;
            } else if (errorStdout) {
                detailedError = `${errorMessage} | Output: ${errorStdout}`;
            } else {
                // If we have no stderr/stdout, try to get more info from the error object
                detailedError = `${errorMessage} | Exit code: ${errorCode || 'unknown'}`;
            }
            
            // Log detailed error for debugging
            if (command.startsWith('adb ')) {
                this.logger.error(`❌ ADB Command Failed: ${command}`);
                this.logger.error(`   Error Code: ${errorCode || 'N/A'}`);
                if (errorSignal) this.logger.error(`   Signal: ${errorSignal}`);
                if (isKillProcessError) {
                    this.logger.error(`   ⚠️ Process was killed (SIGKILL/Error 137) - likely memory or timeout issue`);
                }
                this.logger.error(`   Error Message: ${errorMessage}`);
                if (errorStderr) {
                    this.logger.error(`   ADB stderr: ${errorStderr}`);
                } else {
                    this.logger.warn(`   ⚠️ No stderr captured - this may indicate a connection issue`);
                }
                if (errorStdout) {
                    this.logger.error(`   ADB stdout: ${errorStdout}`);
                } else {
                    this.logger.warn(`   ⚠️ No stdout captured`);
                }
            }
            
            // Real error - return failure with all details
            return {
                success: false,
                error: detailedError,
                output: errorStdout,
                stderr: errorStderr,
                fullOutput: (errorStdout || '') + (errorStderr || ''),
                errorCode: errorCode,
                errorSignal: errorSignal,
                command: command,
                isKillProcessError: isKillProcessError
            };
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
