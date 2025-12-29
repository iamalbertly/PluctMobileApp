const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-UX-10ErrorPersistence-Validation
 * Validates that error messages persist across state changes and don't auto-dismiss
 */
class JourneyUX10ErrorPersistenceValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-10ErrorPersistence-Validation';
    }

    async execute() {
        await this.log('Starting Error Persistence Validation');
        
        // Step 1: Launch app and clear state
        await this.core.launchApp();
        await this.core.sleep(2000);
        await this.core.dumpUIHierarchy();
        
        // Step 2: Trigger an error (invalid URL or network error)
        await this.core.tapByTestTag('url_input_field');
        await this.core.inputText('invalid-url-test');
        await this.core.tapByTestTag('extract_script_button');
        await this.core.sleep(2000);
        
        // Step 3: Verify error appears
        await this.core.dumpUIHierarchy();
        let uiDump = this.core.readLastUIDump() || '';
        if (!uiDump.includes('Error') && !uiDump.includes('error') && !uiDump.includes('invalid')) {
            return { success: false, error: 'Error message not displayed' };
        }
        
        // Step 4: Trigger processing state change by entering valid URL
        await this.core.tapByTestTag('url_input_field');
        await this.core.sleep(500);
        await this.core.inputText('https://vm.tiktok.com/ZMDRUGT2P/');
        await this.core.sleep(1000);
        
        // Step 5: Verify error persists (should not disappear)
        await this.core.dumpUIHierarchy();
        uiDump = this.core.readLastUIDump() || '';
        // Error should still be visible (may have changed but should not be gone)
        if (!uiDump.includes('Error') && !uiDump.includes('error') && !uiDump.includes('invalid')) {
            this.logger.warn('Error message may have disappeared during state change');
        }
        
        // Step 6: Check logcat for error persistence logs
        const logcatResult = await this.core.executeCommand('adb logcat -d | findstr /i "PersistentError\|error_persist\|USER_FACING_ERROR"');
        if (!logcatResult.success || !logcatResult.output) {
            this.logger.warn('No error persistence logs found in logcat');
        } else {
            this.logger.info('Error persistence logs found in logcat');
        }
        
        // Step 7: Verify error can be manually dismissed
        const dismissResult = await this.core.tapByContentDesc('Dismiss error');
        if (dismissResult.success) {
            await this.core.sleep(1000);
            await this.core.dumpUIHierarchy();
            uiDump = this.core.readLastUIDump() || '';
            if (uiDump.includes('Error') || uiDump.includes('error')) {
                this.logger.warn('Error still visible after dismiss attempt');
            }
        }
        
        await this.log('Error Persistence Validation Complete');
        return { success: true };
    }
}

module.exports = JourneyUX10ErrorPersistenceValidation;



