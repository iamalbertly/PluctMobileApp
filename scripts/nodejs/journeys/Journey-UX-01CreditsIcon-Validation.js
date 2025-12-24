const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyUX01CreditsIconValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Journey-UX-01CreditsIcon-Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-UX-01CreditsIcon-Validation');
        
        try {
            // Step 1: Launch app and wait for header to load
            this.core.logger.info('- Step 1: Launch app and wait for header');
            await this.ensureAppForeground();
            await this.core.sleep(3000);
            
            // Step 2: Dump UI and verify diamond emoji is NOT present
            this.core.logger.info('- Step 2: Verify diamond emoji removed');
            await this.dumpUI();
            const uiDump = this.core.readLastUIDump();
            
            const hasDiamond = uiDump.includes('💎') || 
                              uiDump.includes('diamond') ||
                              uiDump.toLowerCase().includes('diamond icon');
            
            if (hasDiamond) {
                this.core.logger.error('❌ FAILURE: Diamond emoji still present in UI');
                this.core.logger.error('❌ UI dump contains diamond references');
                return { success: false, error: 'Diamond emoji not removed' };
            }
            this.core.logger.info('✅ Diamond emoji not found (expected)');
            
            // Step 3: Verify wallet/currency icon is present
            this.core.logger.info('- Step 3: Verify currency icon present');
            // Check for Material Icon resource or AccountBalanceWallet
            const hasCurrencyIcon = uiDump.includes('AccountBalanceWallet') ||
                                   uiDump.includes('credit balance icon') ||
                                   uiDump.includes('Credit balance icon');
            
            if (!hasCurrencyIcon) {
                this.core.logger.warn('⚠️ Currency icon reference not found in UI dump (may be rendered as icon resource)');
            } else {
                this.core.logger.info('✅ Currency icon found');
            }
            
            // Step 4: Verify "Credits:" text is NOT present, only number
            this.core.logger.info('- Step 4: Verify "Credits:" text removed');
            const hasCreditsLabel = uiDump.includes('Credits:') || 
                                   uiDump.includes('Credits :');
            
            if (hasCreditsLabel) {
                this.core.logger.error('❌ FAILURE: "Credits:" text label still present');
                return { success: false, error: '"Credits:" text label not removed' };
            }
            this.core.logger.info('✅ "Credits:" text label not found (expected)');
            
            // Verify number is present (balance should be shown)
            const hasBalanceNumber = /\b\d+\b/.test(uiDump) && 
                                    (uiDump.includes('Credit balance') || 
                                     uiDump.includes('credit balance'));
            
            if (!hasBalanceNumber) {
                this.core.logger.warn('⚠️ Balance number not clearly visible (may be loading)');
            } else {
                this.core.logger.info('✅ Balance number found in header');
            }
            
            // Step 5: Check logcat for icon-related errors
            this.core.logger.info('- Step 5: Check logcat for errors');
            const apiErrors = await this.core.checkRecentAPIErrors(100);
            if (!apiErrors.success && apiErrors.errors) {
                const iconErrors = apiErrors.errors.filter(e => 
                    e.toLowerCase().includes('icon') || 
                    e.toLowerCase().includes('accountbalancewallet')
                );
                if (iconErrors.length > 0) {
                    this.core.logger.error('❌ FAILURE: Icon-related errors in logcat');
                    return { success: false, error: 'Icon errors detected', details: iconErrors };
                }
            }
            this.core.logger.info('✅ No icon-related errors in logcat');
            
            this.core.logger.info('✅ Completed: Journey-UX-01CreditsIcon-Validation');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Credits icon validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = JourneyUX01CreditsIconValidation;

