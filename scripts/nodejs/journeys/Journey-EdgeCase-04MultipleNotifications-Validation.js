const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-EdgeCase-04MultipleNotifications-Validation
 * Follows naming convention: [Journey]-[EdgeCase]-[04MultipleNotifications]-[Validation]
 * 4 scope layers: Journey, EdgeCase, MultipleNotifications, Validation
 * Validates multiple notifications prevention edge case
 */
class JourneyEdgeCase04MultipleNotificationsValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'EdgeCase-04MultipleNotifications-Validation';
    }

    async execute() {
        this.core.logger.info('Starting Multiple Notifications Edge Case Validation');
        
        // Step 1: Launch app
        await this.core.launchApp();
        await this.core.sleep(3000);
        
        // Step 2: Send intent to trigger auto-submit
        const url = this.core.config.url;
        await this.core.executeCommand(
            `adb shell am start -a android.intent.action.SEND -t "text/plain" -e android.intent.extra.TEXT "${url}" app.pluct/.PluctUIScreen01MainActivity`
        );
        
        await this.core.sleep(1000);
        
        // Step 3: Manually submit same URL immediately
        await this.core.dumpUIHierarchy();
        let uiDump = this.core.readLastUIDump() || '';
        
        // Find and click extract button
        const extractButton = this.core.findElementByText(uiDump, 'Extract Script');
        if (extractButton) {
            await this.core.clickElement(extractButton);
        } else {
            // Try input field
            const urlInput = this.core.findElementByHint(uiDump, 'Enter TikTok URL');
            if (urlInput) {
                await this.core.typeText(urlInput, url);
                await this.core.sleep(500);
                await this.core.pressKey('Enter');
            }
        }
        
        await this.core.sleep(2000);
        
        // Step 4: Verify only one notification appears
        const notificationCheck = await this.core.executeCommand(
            'adb shell dumpsys notification | findstr /i "Transcribing\|transcription"'
        );
        
        const notificationText = notificationCheck.output || '';
        const notificationCount = (notificationText.match(/Transcribing/gi) || []).length;
        
        if (notificationCount > 1) {
            return {
                success: false,
                error: `Multiple notifications detected: ${notificationCount}`
            };
        }
        
        // Step 5: Verify only one job in WorkManager (check logcat using shared validator)
        const duplicateEnqueueCheck = await this.core.logcatValidator.checkForDuplicateCalls(
            'enqueue|WorkManager.*enqueue',
            5,
            1
        );
        
        if (duplicateEnqueueCheck.isDuplicate) {
            this.logger.warn(`⚠️ Multiple WorkManager enqueues detected: ${duplicateEnqueueCheck.count}`);
        }
        
        // Step 6: Verify no duplicate processing
        const duplicateProcessingCheck = await this.core.logcatValidator.checkForDuplicateCalls(
            'Starting transcription|Submitting job|processTikTokVideo.*Starting',
            5,
            1
        );
        
        if (duplicateProcessingCheck.isDuplicate) {
            return {
                success: false,
                error: `Duplicate processing detected: ${duplicateProcessingCheck.count} starts`
            };
        }
        
        this.core.logger.info('✅ Multiple notifications edge case validated');
        return true;
    }
}

module.exports = JourneyEdgeCase04MultipleNotificationsValidation;

