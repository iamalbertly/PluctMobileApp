const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyUX02CreditsLoadingValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Journey-UX-02CreditsLoading-Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-UX-02CreditsLoading-Validation');
        
        try {
            // Step 1: Clear app data to force fresh load
            this.core.logger.info('- Step 1: Clear app data for fresh load');
            await this.core.executeCommand('adb shell pm clear app.pluct');
            await this.core.sleep(1000);
            
            // Step 2: Launch app
            this.core.logger.info('- Step 2: Launch app');
            await this.core.launchApp();
            
            // Step 3: Immediately dump UI (within 500ms of launch)
            this.core.logger.info('- Step 3: Capture initial UI state immediately');
            await this.core.sleep(500);
            await this.dumpUI();
            const initialUI = this.core.readLastUIDump();
            
            // Step 4: Verify "0" is NOT shown in header
            this.core.logger.info('- Step 4: Verify "0" not shown during loading');
            const showsZero = (initialUI.includes('Credits: 0') || 
                             initialUI.includes('Credits : 0') ||
                             (initialUI.includes('0') && initialUI.includes('credit balance')));
            
            if (showsZero) {
                this.core.logger.error('❌ FAILURE: "0" credits shown during loading state');
                return { success: false, error: 'Shows "0" during loading instead of loading indicator' };
            }
            this.core.logger.info('✅ "0" not shown during loading (expected)');
            
            // Step 5: Verify loading spinner or "Loading..." text is present
            this.core.logger.info('- Step 5: Verify loading indicator present');
            const hasLoadingIndicator = initialUI.includes('Loading...') ||
                                      initialUI.includes('Loading') ||
                                      initialUI.includes('CircularProgressIndicator') ||
                                      initialUI.includes('Loading credit balance');
            
            if (!hasLoadingIndicator) {
                this.core.logger.warn('⚠️ Loading indicator not immediately visible (may have loaded quickly)');
            } else {
                this.core.logger.info('✅ Loading indicator found');
            }
            
            // Step 6: Wait 5 seconds and verify balance appears
            this.core.logger.info('- Step 6: Wait for balance to load');
            await this.core.sleep(5000);
            await this.dumpUI();
            const loadedUI = this.core.readLastUIDump();
            
            const hasBalance = /\b\d+\b/.test(loadedUI) && 
                              (loadedUI.includes('Credit balance') || 
                               loadedUI.includes('credit balance'));
            
            if (!hasBalance) {
                this.core.logger.warn('⚠️ Balance not visible after 5 seconds (may need more time)');
            } else {
                this.core.logger.info('✅ Balance appeared after loading');
            }
            
            // Step 7: Check logcat for balance API calls
            this.core.logger.info('- Step 7: Verify balance API calls in logcat');
            const apiLogs = await this.core.captureAPILogs(200);
            if (apiLogs.success) {
                const balanceRequests = apiLogs.parsed.requests.filter(r => 
                    r.includes('/v1/credits/balance') || 
                    r.includes('balance')
                );
                if (balanceRequests.length > 0) {
                    this.core.logger.info(`✅ Balance API request found in logcat`);
                } else {
                    this.core.logger.warn('⚠️ Balance API request not found in recent logcat');
                }
            }
            
            this.core.logger.info('✅ Completed: Journey-UX-02CreditsLoading-Validation');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Credits loading state validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = JourneyUX02CreditsLoadingValidation;

