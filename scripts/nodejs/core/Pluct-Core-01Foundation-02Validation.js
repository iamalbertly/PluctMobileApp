/**
 * Pluct-Core-01Foundation-02Validation - Environment validation functionality
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Adheres to 300-line limit with smart separation of concerns
 */

class PluctCoreFoundationValidation {
    constructor() {
        this.logger = new PluctLogger();
        this.validationResults = new Map();
    }

    /**
     * Validate environment
     */
    async validateEnvironment() {
        this.logger.info('🔍 Validating environment...');
        
        try {
            // Check required tools
            await this.checkRequiredTools();
            
            // Check device connectivity
            await this.checkDeviceConnectivity();
            
            // Check app installation
            await this.checkAppInstallation();
            
            // Check network connectivity
            await this.checkNetworkConnectivity();
            
            this.logger.info('✅ Environment validation passed');
            return { success: true };
        } catch (error) {
            this.logger.error('❌ Environment validation failed:', error);
            return { success: false, error: error.message };
        }
    }

    /**
     * Check required tools
     */
    async checkRequiredTools() {
        this.logger.info('🔧 Checking required tools...');
        
        const requiredTools = ['adb', 'node', 'npm'];
        const toolResults = {};
        
        for (const tool of requiredTools) {
            const exists = await this.commandExists(tool);
            toolResults[tool] = exists;
            
            if (exists) {
                const version = await this.getCommandVersion(tool);
                this.logger.info(`✅ ${tool}: ${version || 'available'}`);
            } else {
                this.logger.error(`❌ ${tool}: not found`);
            }
        }
        
        this.validationResults.set('tools', toolResults);
        
        const missingTools = Object.entries(toolResults)
            .filter(([tool, exists]) => !exists)
            .map(([tool]) => tool);
        
        if (missingTools.length > 0) {
            throw new Error(`Missing required tools: ${missingTools.join(', ')}`);
        }
    }

    /**
     * Check device connectivity
     */
    async checkDeviceConnectivity() {
        this.logger.info('📱 Checking device connectivity...');
        
        try {
            const result = await this.executeCommand('adb devices');
            if (!result.success) {
                throw new Error('ADB not working');
            }
            
            const devices = this.parseAdbDevices(result.output);
            this.validationResults.set('devices', devices);
            
            if (devices.length === 0) {
                throw new Error('No devices connected');
            }
            
            this.logger.info(`✅ Found ${devices.length} device(s)`);
            devices.forEach(device => {
                this.logger.info(`  - ${device.id}: ${device.status}`);
            });
            
        } catch (error) {
            throw new Error(`Device connectivity check failed: ${error.message}`);
        }
    }

    /**
     * Check app installation
     */
    async checkAppInstallation() {
        this.logger.info('📦 Checking app installation...');
        
        try {
            const result = await this.executeCommand('adb shell pm list packages | grep app.pluct');
            if (!result.success) {
                throw new Error('App not installed');
            }
            
            const isInstalled = result.output.includes('app.pluct');
            this.validationResults.set('app_installed', isInstalled);
            
            if (isInstalled) {
                this.logger.info('✅ App is installed');
            } else {
                throw new Error('App not found');
            }
            
        } catch (error) {
            throw new Error(`App installation check failed: ${error.message}`);
        }
    }

    /**
     * Check network connectivity
     */
    async checkNetworkConnectivity() {
        this.logger.info('🌐 Checking network connectivity...');
        
        try {
            const result = await this.executeCommand('adb shell ping -c 1 8.8.8.8');
            if (!result.success) {
                throw new Error('Network not available');
            }
            
            this.validationResults.set('network_available', true);
            this.logger.info('✅ Network connectivity available');
            
        } catch (error) {
            this.logger.warn('⚠️ Network connectivity check failed:', error.message);
            this.validationResults.set('network_available', false);
        }
    }

    /**
     * Parse ADB devices output
     */
    parseAdbDevices(output) {
        const lines = output.split('\n');
        const devices = [];
        
        for (const line of lines) {
            if (line.includes('\tdevice') || line.includes('\temulator')) {
                const parts = line.split('\t');
                if (parts.length >= 2) {
                    devices.push({
                        id: parts[0],
                        status: parts[1]
                    });
                }
            }
        }
        
        return devices;
    }

    /**
     * Check if command exists
     */
    async commandExists(command) {
        try {
            const result = await this.executeCommand(`where ${command}`, 2000);
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
            const result = await this.executeCommand(`${command} --version`, 2000);
            if (result.success) {
                return result.output.trim();
            }
        } catch (error) {
            // Try alternative version flags
            try {
                const result = await this.executeCommand(`${command} -v`, 2000);
                if (result.success) {
                    return result.output.trim();
                }
            } catch (error2) {
                // Try -V flag
                try {
                    const result = await this.executeCommand(`${command} -V`, 2000);
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
     * Execute command
     */
    async executeCommand(command, timeout = 5000) {
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
            return { success: false, error: error.message };
        }
    }

    /**
     * Get validation results
     */
    getValidationResults() {
        return Object.fromEntries(this.validationResults);
    }
}

module.exports = PluctCoreFoundationValidation;
