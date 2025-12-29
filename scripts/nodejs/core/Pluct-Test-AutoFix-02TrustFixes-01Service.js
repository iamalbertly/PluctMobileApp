/**
 * Pluct-Test-AutoFix-02TrustFixes-01Service
 * Automatically detects and fixes trust fixes specific issues
 * Follows naming convention: [Project]-[Test]-[AutoFix]-[TrustFixes]-[Service]
 * 5 scope layers: Project, Test, AutoFix, TrustFixes, Service
 */

const PluctTestAutoFix = require('./Pluct-Test-AutoFix-01CommonIssues');
const fs = require('fs');
const path = require('path');

class PluctTestAutoFixTrustFixes extends PluctTestAutoFix {
    constructor(core) {
        super(core);
        this.codebaseFixes = [];
        this.testFixes = [];
    }

    /**
     * Detect and fix trust fixes specific issues
     * @returns {Promise<boolean>} True if fixes were applied
     */
    async detectAndFix() {
        // First run common fixes
        const commonFixes = await super.detectAndFix();
        
        // Then detect trust fixes specific issues
        const issues = await this.detectTrustFixesIssues();
        if (issues.length === 0 && !commonFixes) {
            return false;
        }

        this.core.logger.info(`🔧 Detected ${issues.length} trust fixes issue(s), attempting auto-fix...`);
        
        for (const issue of issues) {
            try {
                await this.fixTrustFixesIssue(issue);
                if (issue.type.startsWith('codebase_')) {
                    this.codebaseFixes.push(issue);
                } else {
                    this.testFixes.push(issue);
                }
                this.fixesApplied.push(issue);
            } catch (error) {
                this.core.logger.warn(`⚠️  Failed to fix issue: ${issue.type} - ${error.message}`);
            }
        }

        return this.fixesApplied.length > 0 || commonFixes;
    }

    /**
     * Detect trust fixes specific issues
     * @returns {Promise<Array>} Array of detected issues
     */
    async detectTrustFixesIssues() {
        const issues = [];

        // Check for missing imports in Kotlin files
        const kotlinFiles = [
            'app/src/main/java/app/pluct/ui/screens/Pluct-UI-Screen-01MainActivity-01IntentHandler-02QueueManager.kt',
            'app/src/main/java/app/pluct/core/credit/Pluct-Core-Credit-01AtomicReservation-01Service.kt',
            'app/src/main/java/app/pluct/services/Pluct-Core-Background-01TranscriptionWorker-02NetworkMonitor.kt',
            'app/src/main/java/app/pluct/services/Pluct-Core-Background-01TranscriptionWorker-03JobDeduplication.kt',
            'app/src/main/java/app/pluct/services/Pluct-Core-API-01UnifiedService-02TokenRefresh-01Manager.kt',
            'app/src/main/java/app/pluct/services/Pluct-Core-API-01UnifiedService-03RequestDeduplication-01Handler.kt'
        ];

        for (const filePath of kotlinFiles) {
            const fullPath = path.join(process.cwd(), filePath);
            if (fs.existsSync(fullPath)) {
                const content = fs.readFileSync(fullPath, 'utf-8');
                
                // Check for common missing imports
                if (content.includes('PluctNotificationHelper') && !content.includes('import app.pluct.notification.PluctNotificationHelper')) {
                    issues.push({
                        type: 'codebase_missing_import',
                        file: filePath,
                        description: `Missing import for PluctNotificationHelper in ${filePath}`,
                        fix: 'import app.pluct.notification.PluctNotificationHelper'
                    });
                }
                
                if (content.includes('PluctQueueManager') && !content.includes('import app.pluct.services.PluctQueueManager')) {
                    issues.push({
                        type: 'codebase_missing_import',
                        file: filePath,
                        description: `Missing import for PluctQueueManager in ${filePath}`,
                        fix: 'import app.pluct.services.PluctQueueManager'
                    });
                }
            }
        }

        // Check for incorrect UI selectors in test files
        const testFiles = [
            'scripts/nodejs/journeys/Journey-EdgeCase-01RapidIntentReceipt-Validation.js',
            'scripts/nodejs/journeys/Journey-EdgeCase-02CreditDepletion-Validation.js',
            'scripts/nodejs/journeys/Journey-EdgeCase-03NetworkLoss-Validation.js',
            'scripts/nodejs/journeys/Journey-EdgeCase-04MultipleNotifications-Validation.js',
            'scripts/nodejs/journeys/Journey-EdgeCase-05JWTExpiration-Validation.js',
            'scripts/nodejs/journeys/Journey-EdgeCase-06ConcurrentVending-Validation.js',
            'scripts/nodejs/journeys/Journey-EdgeCase-07TokenExpirationPolling-Validation.js',
            'scripts/nodejs/journeys/Journey-EdgeCase-08NetworkInterruption-Validation.js'
        ];

        for (const filePath of testFiles) {
            const fullPath = path.join(process.cwd(), filePath);
            if (fs.existsSync(fullPath)) {
                const content = fs.readFileSync(fullPath, 'utf-8');
                
                // Check for common selector issues
                if (content.includes('testTag') && !content.includes('testTag=') && !content.includes("testTag:")) {
                    issues.push({
                        type: 'test_incorrect_selector',
                        file: filePath,
                        description: `Potential incorrect testTag usage in ${filePath}`,
                        fix: 'Review testTag selectors'
                    });
                }
            }
        }

        return issues;
    }

    /**
     * Fix a trust fixes specific issue
     * @param {Object} issue - Issue to fix
     */
    async fixTrustFixesIssue(issue) {
        this.core.logger.info(`🔧 Fixing trust fixes issue: ${issue.type} - ${issue.description}`);

        switch (issue.type) {
            case 'codebase_missing_import':
                await this.fixMissingImport(issue);
                break;
            case 'test_incorrect_selector':
                await this.fixIncorrectSelector(issue);
                break;
            default:
                this.core.logger.warn(`⚠️  Unknown trust fixes issue type: ${issue.type}`);
        }
    }

    /**
     * Fix missing import
     */
    async fixMissingImport(issue) {
        const fullPath = path.join(process.cwd(), issue.file);
        if (!fs.existsSync(fullPath)) {
            throw new Error(`File not found: ${issue.file}`);
        }

        const content = fs.readFileSync(fullPath, 'utf-8');
        
        // Find the package declaration
        const packageMatch = content.match(/^package\s+[\w.]+/m);
        if (!packageMatch) {
            throw new Error('Could not find package declaration');
        }

        // Find the last import statement
        const importMatches = content.matchAll(/^import\s+[\w.]+/gm);
        let lastImportIndex = -1;
        for (const match of importMatches) {
            lastImportIndex = match.index + match[0].length;
        }

        if (lastImportIndex === -1) {
            // No imports, add after package
            const packageEnd = content.indexOf('\n', packageMatch.index) + 1;
            const newContent = content.slice(0, packageEnd) + 
                `\n${issue.fix}\n` + 
                content.slice(packageEnd);
            fs.writeFileSync(fullPath, newContent, 'utf-8');
        } else {
            // Add after last import
            const newContent = content.slice(0, lastImportIndex) + 
                `\n${issue.fix}` + 
                content.slice(lastImportIndex);
            fs.writeFileSync(fullPath, newContent, 'utf-8');
        }

        this.core.logger.info(`✅ Added missing import: ${issue.fix}`);
    }

    /**
     * Fix incorrect selector (placeholder - would need specific logic)
     */
    async fixIncorrectSelector(issue) {
        // This would need specific logic based on the selector issue
        // For now, just log a warning
        this.core.logger.warn(`⚠️  Selector issue detected in ${issue.file}, manual review recommended`);
    }

    /**
     * Attempt to fix a test result
     * @param {Object} result - Test result with error
     * @returns {Promise<boolean>} True if fix was attempted
     */
    async attemptFix(result) {
        if (!result.error) {
            return false;
        }

        // Detect common error patterns and attempt fixes
        const error = result.error.toLowerCase();
        
        if (error.includes('missing import') || error.includes('unresolved reference')) {
            // Try to detect and fix missing imports
            const issues = await this.detectTrustFixesIssues();
            const importIssues = issues.filter(i => i.type === 'codebase_missing_import');
            if (importIssues.length > 0) {
                for (const issue of importIssues) {
                    await this.fixTrustFixesIssue(issue);
                }
                return true;
            }
        }

        if (error.includes('selector') || error.includes('testtag') || error.includes('element not found')) {
            // Try to fix selector issues
            const issues = await this.detectTrustFixesIssues();
            const selectorIssues = issues.filter(i => i.type === 'test_incorrect_selector');
            if (selectorIssues.length > 0) {
                for (const issue of selectorIssues) {
                    await this.fixTrustFixesIssue(issue);
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Get summary of fixes applied
     * @returns {string} Summary of fixes
     */
    getSummary() {
        const commonSummary = super.getSummary();
        const codebaseCount = this.codebaseFixes.length;
        const testCount = this.testFixes.length;
        
        if (codebaseCount === 0 && testCount === 0) {
            return commonSummary;
        }

        let summary = commonSummary;
        if (codebaseCount > 0) {
            summary += `\nCodebase fixes: ${codebaseCount}`;
        }
        if (testCount > 0) {
            summary += `\nTest fixes: ${testCount}`;
        }

        return summary;
    }
}

module.exports = PluctTestAutoFixTrustFixes;

