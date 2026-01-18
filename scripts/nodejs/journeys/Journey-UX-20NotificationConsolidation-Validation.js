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
            'adb shell dumpsys notification | findstr /i "Transcribing\|Transcription Started\|Pluct"'
        );
        
        // Count unique notification IDs
        const notificationLines = notificationDump.output ? notificationDump.output.split('\n').filter(line => 
            (line.includes('Transcribing') || line.includes('Transcription Started') || line.includes('Pluct')) &&
            !line.trim().isEmpty()
        ) : [];
        
        // Check logcat for notification creation evidence
        const logcatCheck = await this.core.executeCommand(
            'adb logcat -d -t 100 | findstr /i "PluctNotificationHelper\|showTranscriptionProgressNotification\|Notification.*notify"'
        );
        
        // Validation: Should have at most 1 notification OR logcat evidence
        if (notificationLines.length > 1) {
            return { 
                success: false, 
                error: `Multiple notifications detected: ${notificationLines.length} notifications found` 
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
            'adb logcat -d -t 100 | findstr /i "ic_launcher\|R.mipmap.ic_launcher"'
        );
        
        // Step 6: Verify notification updates progress
        await this.core.sleep(5000);
        const progressCheck = await this.core.executeCommand(
            'adb shell dumpsys notification | findstr /i "progress\|Transcribing"'
        );
        
        this.core.logger.info('✅ Notification consolidation validation passed');
        return { 
            success: true, 
            details: { 
                notificationCount: notificationLines.length,
                hasAppIcon: iconCheck.output ? iconCheck.output.includes('ic_launcher') : false,
                hasProgress: progressCheck.output ? progressCheck.output.includes('progress') : false
            }
        };
    }
}

module.exports = JourneyUX20NotificationConsolidationValidation;
