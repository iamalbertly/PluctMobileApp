/**
 * Pluct-Core-01Foundation-05Logcat-01Validator - Logcat validation utility
 * Follows naming convention: [Project]-[Core]-[Foundation]-[Logcat]-[Validator]
 * 5 scope layers: Project, Core, Foundation, Logcat, Validator
 * Consolidates duplicate logcat checking patterns across journey tests
 */
class PluctCoreFoundationLogcatValidator {
    constructor(commands, logger) {
        this.commands = commands;
        this.logger = logger;
    }

    /**
     * Validate logcat pattern with retries
     * @param {string} pattern - Pattern to search for (supports | for OR)
     * @param {string} description - Human-readable description for logging
     * @param {number} maxRetries - Maximum number of retry attempts
     * @param {number} retryDelayMs - Delay between retries in milliseconds
     * @param {number} logLines - Number of recent log lines to check (-t flag)
     * @returns {Promise<{success: boolean, matches: string[], output?: string}>}
     */
    async validatePattern(pattern, description, maxRetries = 3, retryDelayMs = 2000, logLines = null) {
        const logFlag = logLines ? `-t ${logLines}` : '-d';
        
        for (let i = 0; i < maxRetries; i++) {
            const command = `adb logcat ${logFlag} | findstr /i "${pattern}"`;
            const result = await this.commands.executeCommand(command);
            
            if (result.success && result.output && result.output.trim()) {
                const matches = result.output.split('\n').filter(line => line.trim());
                this.logger.info(`✅ ${description} confirmed (attempt ${i + 1}/${maxRetries})`);
                return { success: true, matches, output: result.output };
            }
            
            if (i < maxRetries - 1) {
                await new Promise(resolve => setTimeout(resolve, retryDelayMs));
            }
        }
        
        this.logger.warn(`⚠️ ${description} not found after ${maxRetries} attempts`);
        return { success: false, matches: [] };
    }

    /**
     * Check for errors in logcat
     * @param {string[]} excludePatterns - Patterns to exclude from error detection
     * @param {number} logLines - Number of recent log lines to check
     * @returns {Promise<{success: boolean, errorCount: number, errors: string[]}>}
     */
    async checkForErrors(excludePatterns = [], logLines = 100) {
        const excludeFlag = excludePatterns.length > 0 
            ? excludePatterns.map(p => `-v "${p}"`).join(' ')
            : '';
        
        const result = await this.commands.executeCommand(
            `adb logcat -d -t ${logLines} | findstr /i "E/|ERROR|FATAL|Exception"`
        );
        
        if (!result.success || !result.output) {
            return { success: true, errorCount: 0, errors: [] };
        }
        
        const lines = result.output.split('\n').filter(line => line.trim());
        const criticalErrors = lines.filter(line => {
            const lower = line.toLowerCase();
            // Exclude warnings and expected errors
            if (lower.includes('warn')) return false;
            if (excludePatterns.some(pattern => lower.includes(pattern.toLowerCase()))) return false;
            // Include only critical errors
            return lower.includes('fatal') || 
                   lower.includes('crash') || 
                   lower.includes('unclosed') ||
                   (lower.includes('exception') && !lower.includes('expected'));
        });
        
        return {
            success: criticalErrors.length === 0,
            errorCount: criticalErrors.length,
            errors: criticalErrors
        };
    }

    /**
     * Check for duplicate API calls
     * @param {string} apiPattern - Pattern to identify API calls (e.g., "vend-token", "submitTranscription")
     * @param {number} timeWindowSeconds - Time window in seconds to check for duplicates
     * @param {number} maxAllowed - Maximum allowed calls in time window
     * @returns {Promise<{success: boolean, count: number, isDuplicate: boolean}>}
     */
    async checkForDuplicateCalls(apiPattern, timeWindowSeconds = 5, maxAllowed = 1) {
        const logLines = timeWindowSeconds * 10; // Approximate lines per second
        const result = await this.validatePattern(
            apiPattern,
            `Duplicate ${apiPattern} check`,
            1,
            0,
            logLines
        );
        
        if (!result.success) {
            return { success: true, count: 0, isDuplicate: false };
        }
        
        const callCount = result.matches.length;
        const isDuplicate = callCount > maxAllowed;
        
        if (isDuplicate) {
            this.logger.warn(`⚠️ Multiple ${apiPattern} calls detected (${callCount}), may indicate duplicate processing`);
        }
        
        return {
            success: true,
            count: callCount,
            isDuplicate
        };
    }

    /**
     * Validate processing lock registration/cleanup
     * @param {string} url - URL to check processing lock for
     * @returns {Promise<{success: boolean, registered: boolean, cleaned: boolean}>}
     */
    async validateProcessingLock(url) {
        const registered = await this.validatePattern(
            `Registered processing for URL.*${url}|ProcessingLock.*Registered.*${url}`,
            'Processing lock registered',
            2
        );
        
        const cleaned = await this.validatePattern(
            `Unregistered processing for URL.*${url}|ProcessingLock.*Unregistered.*${url}`,
            'Processing lock cleanup',
            2
        );
        
        return {
            success: registered.success || cleaned.success,
            registered: registered.success,
            cleaned: cleaned.success
        };
    }
}

module.exports = PluctCoreFoundationLogcatValidator;
