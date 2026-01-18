/**
 * Pluct-Test-AutoFix-01CommonIssues
 * Automatically detects and fixes common test issues
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Responsibility]
 */

const { execSync } = require('child_process');

class PluctTestAutoFix {
    constructor(core) {
        this.core = core;
        this.fixesApplied = [];
    }

    /**
     * Detect and fix common issues
     * @returns {Promise<boolean>} True if fixes were applied
     */
    async detectAndFix() {
        const issues = await this.detectIssues();
        if (issues.length === 0) {
            return false;
        }

        this.core.logger.info(`🔧 Detected ${issues.length} issue(s), attempting auto-fix...`);
        
        for (const issue of issues) {
            try {
                await this.fixIssue(issue);
                this.fixesApplied.push(issue);
            } catch (error) {
                this.core.logger.warn(`⚠️  Failed to fix issue: ${issue.type} - ${error.message}`);
            }
        }

        return this.fixesApplied.length > 0;
    }

    /**
     * Detect common issues
     * @returns {Promise<Array>} Array of detected issues
     */
    async detectIssues() {
        const issues = [];

        // Check if app is in foreground
        try {
            const foregroundCheck = execSync('adb shell dumpsys window windows | grep -E "mCurrentFocus|mFocusedApp"', { encoding: 'utf-8' });
            if (!foregroundCheck.includes('app.pluct')) {
                issues.push({ type: 'app_backgrounded', description: 'App is not in foreground' });
            }
        } catch (error) {
            // Ignore detection errors
        }

        // Check if network is disabled
        try {
            const networkCheck = execSync('adb shell settings get global airplane_mode_on', { encoding: 'utf-8' });
            if (networkCheck.trim() === '1') {
                issues.push({ type: 'network_disabled', description: 'Airplane mode is enabled' });
            }
        } catch (error) {
            // Ignore detection errors
        }

        // Check if ADB is connected
        try {
            execSync('adb devices', { encoding: 'utf-8', stdio: 'pipe' });
        } catch (error) {
            issues.push({ type: 'adb_disconnected', description: 'ADB device not connected' });
        }

        return issues;
    }

    /**
     * Fix a specific issue
     * @param {Object} issue - Issue to fix
     */
    async fixIssue(issue) {
        this.core.logger.info(`🔧 Fixing issue: ${issue.type} - ${issue.description}`);

        switch (issue.type) {
            case 'app_backgrounded':
                await this.fixAppBackgrounded();
                break;
            case 'network_disabled':
                await this.fixNetworkDisabled();
                break;
            case 'adb_disconnected':
                await this.fixAdbDisconnected();
                break;
            default:
                this.core.logger.warn(`⚠️  Unknown issue type: ${issue.type}`);
        }
    }

    /**
     * Fix app backgrounded issue
     */
    async fixAppBackgrounded() {
        try {
            // Bring app to foreground
            execSync('adb shell am start -n app.pluct/.PluctUIScreen01MainActivity', { encoding: 'utf-8' });
            await this.core.utils.sleep(2000); // Wait for app to come to foreground
            this.core.logger.info('✅ App brought to foreground');
        } catch (error) {
            throw new Error(`Failed to bring app to foreground: ${error.message}`);
        }
    }

    /**
     * Fix network disabled issue
     */
    async fixNetworkDisabled() {
        try {
            // Disable airplane mode
            execSync('adb shell settings put global airplane_mode_on 0', { encoding: 'utf-8' });
            execSync('adb shell am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false', { encoding: 'utf-8' });
            await this.core.utils.sleep(2000); // Wait for network to reconnect
            this.core.logger.info('✅ Network re-enabled');
        } catch (error) {
            throw new Error(`Failed to enable network: ${error.message}`);
        }
    }

    /**
     * Fix ADB disconnected issue
     */
    async fixAdbDisconnected() {
        // This is a critical issue that cannot be auto-fixed
        throw new Error('ADB device not connected. Please connect a device and try again.');
    }

    /**
     * Get summary of fixes applied
     * @returns {string} Summary of fixes
     */
    getSummary() {
        if (this.fixesApplied.length === 0) {
            return 'No fixes applied';
        }

        return `Applied ${this.fixesApplied.length} fix(es):\n${this.fixesApplied.map(f => `  - ${f.type}: ${f.description}`).join('\n')}`;
    }
}

module.exports = PluctTestAutoFix;






