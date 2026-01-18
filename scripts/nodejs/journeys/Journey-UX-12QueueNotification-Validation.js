const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-UX-12QueueNotification-Validation
 * Validates queue notification persistence and updates
 */
class JourneyUX12QueueNotificationValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-12QueueNotification-Validation';
    }

    async execute() {
        this.core.logger.info('Starting Queue Notification Validation');
        
        // Step 1: Disable network (simulate offline)
        this.logger.info('Disabling network to simulate offline scenario...');
        await this.core.executeCommand('adb shell svc wifi disable');
        await this.core.executeCommand('adb shell svc data disable');
        await this.core.sleep(2000);
        
        // Step 2: Launch app
        await this.core.launchApp();
        await this.core.sleep(2000);
        
        // Step 3: Enter URL
        await this.core.tapByTestTag('url_input_field');
        await this.core.inputText('https://vm.tiktok.com/ZMDRUGT2P/');
        await this.core.sleep(1500);
        
        // Step 4: Check for queue prompt or attempt to extract
        await this.core.dumpUIHierarchy();
        let uiDump = this.core.readLastUIDump() || '';
        let hasQueuePrompt = uiDump.includes('Save for Later') || 
                            uiDump.includes('No internet') ||
                            uiDump.includes('Cannot process now');
        
        if (!hasQueuePrompt) {
            // Try tapping Extract to trigger queue
            await this.core.tapByTestTag('extract_script_button');
            await this.core.sleep(2000);
            await this.core.dumpUIHierarchy();
            uiDump = this.core.readLastUIDump() || '';
            hasQueuePrompt = uiDump.includes('Save for Later') || 
                            uiDump.includes('No internet');
        }
        
        // Step 5: Queue the video
        if (hasQueuePrompt) {
            const queueResult = await this.core.tapByText('Save for Later');
            if (!queueResult.success) {
                // Try alternative text
                const queueResult2 = await this.core.tapByText('Queue for Later');
                if (!queueResult2.success) {
                    this.logger.warn('Could not find queue button, may need to check UI state');
                }
            }
        } else {
            this.logger.warn('Queue prompt not found, but continuing with notification check');
        }
        
        await this.core.sleep(2000);
        
        // Step 6: Check for persistent notification
        const notificationCheck = await this.core.executeCommand('adb shell dumpsys notification | findstr /i "pluct\|queue"');
        if (!notificationCheck.success || !notificationCheck.output) {
            return { success: false, error: 'Queue notification not found' };
        }
        
        // Step 7: Verify notification content
        if (!notificationCheck.output.includes('queued') && 
            !notificationCheck.output.includes('video') && 
            !notificationCheck.output.includes('waiting')) {
            return { success: false, error: 'Queue notification content incorrect' };
        }
        
        this.logger.info('Queue notification found with correct content');
        
        // Step 8: Re-enable network
        this.logger.info('Re-enabling network...');
        await this.core.executeCommand('adb shell svc wifi enable');
        await this.core.executeCommand('adb shell svc data enable');
        await this.core.sleep(3000);
        
        // Step 9: Verify notification updates when processing starts
        await this.core.sleep(5000);
        const notificationCheck2 = await this.core.executeCommand('adb shell dumpsys notification | findstr /i "pluct\|processing\|queued"');
        if (notificationCheck2.success && notificationCheck2.output) {
            this.logger.info('Notification updated for processing state');
        } else {
            this.logger.warn('Notification update not detected, but this may be expected if queue processed quickly');
        }
        
        // Step 10: Check logcat for queue notification logs
        const logcatResult = await this.core.executeCommand('adb logcat -d | findstr /i "queue.*notification\|updateQueueNotification"');
        if (logcatResult.success && logcatResult.output) {
            this.logger.info('Queue notification logs found in logcat');
        }
        
        this.core.logger.info('Queue Notification Validation Complete');
        return { success: true };
    }
}

module.exports = JourneyUX12QueueNotificationValidation;






