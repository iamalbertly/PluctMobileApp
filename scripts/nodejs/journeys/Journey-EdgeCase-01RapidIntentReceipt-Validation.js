const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-EdgeCase-01RapidIntentReceipt-Validation
 * Follows naming convention: [Journey]-[EdgeCase]-[01RapidIntentReceipt]-[Validation]
 * 4 scope layers: Journey, EdgeCase, RapidIntentReceipt, Validation
 * Validates rapid intent receipt edge case - second video queued when processing active
 */
class JourneyEdgeCase01RapidIntentReceiptValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'EdgeCase-01RapidIntentReceipt-Validation';
    }

    async execute() {
        this.core.logger.info('Starting Rapid Intent Receipt Edge Case Validation');
        
        // Step 1: Launch app
        await this.core.launchApp();
        await this.core.sleep(3000);
        
        // Step 2: Start transcription manually
        await this.core.dumpUIHierarchy();
        let uiDump = this.core.readLastUIDump() || '';
        
        // Find and click extract button
        const extractButton = this.core.findElementByText(uiDump, 'Extract Script');
        if (extractButton) {
            await this.core.clickElement(extractButton);
            await this.core.sleep(1000);
        } else {
            // Try to find input field and enter URL
            const urlInput = this.core.findElementByHint(uiDump, 'Enter TikTok URL');
            if (urlInput) {
                await this.core.typeText(urlInput, this.core.config.url);
                await this.core.sleep(500);
                await this.core.pressKey('Enter');
                await this.core.sleep(1000);
            }
        }
        
        // Step 3: Verify processing started
        await this.core.sleep(2000);
        const logcatResult = await this.core.executeCommand(
            'adb logcat -d -t 50 | findstr /i "Starting transcription\|Submitting job"'
        );
        
        if (!logcatResult.output || !logcatResult.output.includes('Starting')) {
            return {
                success: false,
                error: 'First transcription did not start'
            };
        }
        
        // Step 4: Send intent with TikTok URL within 500ms
        const secondUrl = 'https://vm.tiktok.com/ZMA730881/';
        await this.core.sleep(500);
        
        const intentStartTime = Date.now();
        await this.core.executeCommand(
            `adb shell am start -a android.intent.action.SEND -t "text/plain" -e android.intent.extra.TEXT "${secondUrl}" app.pluct/.PluctUIScreen01MainActivity`
        );
        
        // Step 5: Verify second video is queued (not processed)
        await this.core.sleep(2000);
        
        const queueLogcat = await this.core.executeCommand(
            'adb logcat -d -t 50 | findstr /i "queued\|queue\|SERVICE_UNAVAILABLE"'
        );
        
        const queued = queueLogcat.output && (
            queueLogcat.output.includes('queued') ||
            queueLogcat.output.includes('SERVICE_UNAVAILABLE')
        );
        
        if (!queued) {
            return {
                success: false,
                error: 'Second video was not queued when processing active'
            };
        }
        
        // Step 6: Verify only one notification appears
        await this.core.sleep(2000);
        const notificationCheck = await this.core.executeCommand(
            'adb shell dumpsys notification | findstr /i /c:"app.pluct" /c:"Pluct" /c:"Text" /c:"Video ->" /c:"Audio ->" /c:"%"'
        );
        
        const notificationCount = new Set(
            ((notificationCheck.output || '').match(/\d+% -> Text|100% -> Text|! Pluct/gi) || [])
        ).size;
        if (notificationCount > 1) {
            return {
                success: false,
                error: `Multiple notifications detected: ${notificationCount}`
            };
        }
        
        // Step 7: Verify queue count increases
        await this.core.dumpUIHierarchy();
        uiDump = this.core.readLastUIDump() || '';
        
        const queueCountMatch = uiDump.match(/queue.*?(\d+)/i);
        if (queueCountMatch) {
            const queueCount = parseInt(queueCountMatch[1]);
            if (queueCount < 1) {
                return {
                    success: false,
                    error: 'Queue count did not increase'
                };
            }
        }
        
        this.core.logger.info('✅ Rapid intent receipt edge case validated');
        return true;
    }
}

module.exports = JourneyEdgeCase01RapidIntentReceiptValidation;

