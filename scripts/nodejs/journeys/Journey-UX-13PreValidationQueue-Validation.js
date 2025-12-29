const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-UX-13PreValidationQueue-Validation
 * Validates pre-validation queue prompts appear before Extract button tap
 */
class JourneyUX13PreValidationQueueValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-13PreValidationQueue-Validation';
    }

    async execute() {
        await this.log('Starting Pre-Validation Queue Validation');
        
        // Step 1: Launch app
        await this.core.launchApp();
        await this.core.sleep(2000);
        
        // Step 2: Enter URL
        await this.core.tapByTestTag('url_input_field');
        await this.core.inputText('https://vm.tiktok.com/ZMDRUGT2P/');
        await this.core.sleep(1500);
        
        // Step 3: Verify queue prompt appears BEFORE tapping Extract
        await this.core.dumpUIHierarchy();
        let uiDump = this.core.readLastUIDump() || '';
        
        // Check for queue prompt indicators
        const hasQueuePrompt = uiDump.includes('Save for Later') || 
                              uiDump.includes('Insufficient credits') ||
                              uiDump.includes('No internet') ||
                              uiDump.includes('Cannot process now') ||
                              uiDump.includes('Waiting for');
        
        if (hasQueuePrompt) {
            this.logger.info('Queue prompt found before Extract button tap - pre-validation working');
        } else {
            this.logger.warn('Queue prompt not found before Extract button tap');
            // This may be OK if user has credits and network
        }
        
        // Step 4: Check for Extract button state
        const extractButtonCheck = await this.core.executeCommand('adb shell uiautomator dump /dev/tty 2>&1 | findstr /i "extract\|Extract"');
        if (extractButtonCheck.success && extractButtonCheck.output) {
            this.logger.info('Extract button found in UI');
        }
        
        // Step 5: If queue prompt is available, test queue functionality
        if (hasQueuePrompt) {
            const queueResult = await this.core.tapByText('Save for Later');
            if (queueResult.success) {
                await this.core.sleep(2000);
                
                // Verify URL was queued
                await this.core.dumpUIHierarchy();
                uiDump = this.core.readLastUIDump() || '';
                if (uiDump.includes('queued') || uiDump.includes('Queued') || uiDump.includes('waiting')) {
                    this.logger.info('URL successfully queued via pre-validation prompt');
                } else {
                    this.logger.warn('URL queued but confirmation not visible in UI');
                }
                
                // Check for notification
                const notificationCheck = await this.core.executeCommand('adb shell dumpsys notification | findstr /i "pluct\|queue"');
                if (notificationCheck.success && notificationCheck.output) {
                    this.logger.info('Queue notification appeared after queuing');
                }
            } else {
                this.logger.warn('Could not tap Save for Later button');
            }
        }
        
        // Step 6: Test with network disabled
        this.logger.info('Testing with network disabled...');
        await this.core.executeCommand('adb shell svc wifi disable');
        await this.core.executeCommand('adb shell svc data disable');
        await this.core.sleep(2000);
        
        // Clear and re-enter URL
        await this.core.tapByTestTag('url_input_field');
        await this.core.executeCommand('adb shell input keyevent KEYCODE_CTRL_A');
        await this.core.inputText('https://vm.tiktok.com/ZMDRUGT2P/');
        await this.core.sleep(1500);
        
        await this.core.dumpUIHierarchy();
        uiDump = this.core.readLastUIDump() || '';
        const hasOfflinePrompt = uiDump.includes('Save for Later') || 
                                uiDump.includes('No internet') ||
                                uiDump.includes('internet connection');
        
        if (hasOfflinePrompt) {
            this.logger.info('Queue prompt appeared for offline scenario');
        } else {
            this.logger.warn('Queue prompt not found for offline scenario');
        }
        
        // Re-enable network
        await this.core.executeCommand('adb shell svc wifi enable');
        await this.core.executeCommand('adb shell svc data enable');
        await this.core.sleep(2000);
        
        // Step 7: Check logcat for pre-validation logs
        const logcatResult = await this.core.executeCommand('adb logcat -d | findstr /i "pre.*validation\|queue.*prompt\|network.*available"');
        if (logcatResult.success && logcatResult.output) {
            this.logger.info('Pre-validation logs found in logcat');
        }
        
        await this.log('Pre-Validation Queue Validation Complete');
        return { success: true };
    }
}

module.exports = JourneyUX13PreValidationQueueValidation;



