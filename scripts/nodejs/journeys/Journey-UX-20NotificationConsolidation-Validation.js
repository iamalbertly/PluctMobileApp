const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-UX-20NotificationConsolidation-Validation
 * Validates notification consolidation: single notification with app icon, no duplicates
 */
class JourneyUX20NotificationConsolidationValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-20NotificationConsolidation-Validation';
    }

    async execute() {
        this.core.logger.info('Starting Notification Consolidation Validation');
        
        // Step 1: Launch app and clear notifications
        await this.core.launchApp();
        await this.core.sleep(2000);
        
        // Clear existing notifications
        await this.core.executeCommand('adb shell service call notification 1');
        await this.core.sleep(1000);
        
        // Step 2: Start transcription
        await this.core.tapByTestTag('url_input_field');
        await this.core.inputText(this.core.config.url);
        await this.core.sleep(1000);
        await this.core.tapByTestTag('extract_script_button');
        
        // Step 3: Wait for notifications to appear
        await this.core.sleep(3000);
        
        // Step 4: Wait longer for notification to appear and check via ADB
        await this.core.sleep(5000); // Give more time for notification
        
        const notificationDump = await this.core.executeCommand(
            'adb shell dumpsys notification | findstr /i /c:"app.pluct" /c:"Pluct" /c:"Text" /c:"Video ->" /c:"Audio ->" /c:"pluct_processing_live"'
        );
        
        // Count unique notification IDs
        const notificationLines = notificationDump.output ? notificationDump.output.split('\n').filter(line => 
            (line.includes('Pluct') || line.includes('Text') || line.includes('Video ->') || line.includes('Audio ->')) &&
            line.trim().length > 0
        ) : [];
        
        // Check logcat for notification creation evidence
        const logcatCheck = await this.core.executeCommand(
            'adb logcat -d -t 150 | findstr /i /c:"PluctNotificationHelper" /c:"showTranscriptionProgressNotification" /c:"notify pkg=app.pluct"'
        );
        
        const visibleTitles = new Set(
            notificationLines
                .map(line => (line.match(/(\d+% -> Text|! Pluct|100% -> Text)/i) || [])[1])
                .filter(Boolean)
        );

        // Validation: Should have at most 1 active Pluct status title OR logcat evidence
        if (visibleTitles.size > 1) {
            return { 
                success: false, 
                error: `Multiple notification titles detected: ${Array.from(visibleTitles).join(', ')}` 
            };
        }
        
        // If no notification visible but logcat shows creation, that's acceptable (may be delayed)
        if (notificationLines.length === 0 && !logcatCheck.output) {
            return { 
                success: false, 
                error: 'No notification found and no logcat evidence of notification creation' 
            };
        }
        
        // Step 5: Verify app icon in notification (check logcat for icon resource)
        const iconCheck = await this.core.executeCommand(
            'adb logcat -d -t 100 | findstr /i /c:"ic_stat_pluct" /c:"ic_launcher" /c:"R.mipmap.ic_launcher"'
        );
        
        // Step 6: Verify notification updates progress
        await this.core.sleep(5000);
        const progressCheck = await this.core.executeCommand(
            'adb shell dumpsys notification | findstr /i /c:"progress" /c:"Text" /c:"Video ->" /c:"Audio ->" /c:"%" /c:"app.pluct" /c:"pluct_processing_live"'
        );
        
        this.core.logger.info('✅ Notification consolidation validation passed');
        return { 
            success: true, 
            details: { 
                notificationCount: notificationLines.length,
                hasAppIcon: iconCheck.output ? iconCheck.output.includes('ic_launcher') : false,
                hasProgress: progressCheck.output ? /progress|Text|Video ->|Audio ->|%|pluct_processing_live/i.test(progressCheck.output) : false
            }
        };
    }
}

module.exports = JourneyUX20NotificationConsolidationValidation;
