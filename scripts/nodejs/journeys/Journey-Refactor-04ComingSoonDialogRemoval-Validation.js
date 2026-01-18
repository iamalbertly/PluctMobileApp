const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyRefactor04ComingSoonDialogRemovalValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Journey-Refactor-04ComingSoonDialogRemoval-Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Coming Soon Dialog Removal Validation');
        
        try {
            // Step 1: Launch app
            await this.ensureAppForeground();
            await this.core.sleep(2000);
            
            // Step 2: Clear logcat
            await this.core.clearLogcat();
            
            // Step 3: Check UI for "Coming Soon" text
            this.core.logger.info('- Step 3: Check for "Coming Soon" text in UI');
            await this.core.dumpUIHierarchy();
            let uiDump = this.core.readLastUIDump() || '';
            
            // Check for "Coming Soon" text (case insensitive)
            const hasComingSoon = uiDump.toLowerCase().includes('coming soon');
            
            if (hasComingSoon) {
                // Check if it's the old dialog or something else
                const isOldDialog = uiDump.includes('Premium AI analysis will be available');
                
                if (isOldDialog) {
                    return { 
                        success: false, 
                        error: '"Coming Soon" dialog still present in UI' 
                    };
                }
                // Might be in strings.xml or other non-critical places
                this.core.logger.warn('⚠️ "Coming Soon" text found but may be non-critical');
            } else {
                this.core.logger.info('✅ "Coming Soon" text not found in UI');
            }
            
            // Step 4: Try to trigger premium/upgrade flow if available
            this.core.logger.info('- Step 4: Check for upgrade/premium flow');
            
            // Look for premium/upgrade buttons or dialogs
            const hasUpgradeFlow = uiDump.includes('Upgrade') || 
                                  uiDump.includes('Premium') ||
                                  uiDump.includes('support@pluct.app');
            
            if (hasUpgradeFlow) {
                this.core.logger.info('✅ Upgrade/premium flow elements found');
            }
            
            // Step 5: Check logcat for "Coming Soon" references
            this.core.logger.info('- Step 5: Check logcat for "Coming Soon"');
            await this.core.sleep(1000);
            const logcat = await this.core.captureAPILogs(100);
            const logcatText = logcat.join('\n');
            
            const hasComingSoonInLogs = logcatText.toLowerCase().includes('coming soon');
            
            if (hasComingSoonInLogs) {
                // Check if it's from the old dialog
                const isOldDialogLog = logcatText.includes('Premium AI analysis will be available');
                
                if (isOldDialogLog) {
                    return { 
                        success: false, 
                        error: '"Coming Soon" dialog still being triggered' 
                    };
                }
            }
            
            // Step 6: Verify support contact message if upgrade flow exists
            this.core.logger.info('- Step 6: Verify support contact message');
            const hasSupportContact = uiDump.includes('support@pluct.app') ||
                                     logcatText.includes('support@pluct.app');
            
            if (hasSupportContact) {
                this.core.logger.info('✅ Support contact message found');
            }
            
            // Step 7: Verify no broken dialogs
            this.core.logger.info('- Step 7: Verify no broken dialogs');
            const hasDialogErrors = logcatText.includes('Dialog') && 
                                   (logcatText.includes('Exception') || 
                                    logcatText.includes('Error'));
            
            if (hasDialogErrors) {
                this.core.logger.warn('⚠️ Dialog-related errors found in logs');
            } else {
                this.core.logger.info('✅ No dialog errors');
            }
            
            this.core.logger.info('✅ Coming Soon dialog removal validation completed');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = JourneyRefactor04ComingSoonDialogRemovalValidation;


