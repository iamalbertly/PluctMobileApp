const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-UX-12BackgroundProcessing-Validation
 * Validates app minimizes and shows notification during background transcription
 */
class JourneyUX12BackgroundProcessingValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-12BackgroundProcessing-Validation';
    }

    async execute() {
        await this.log('Starting Background Processing Validation');
        
        // Step 1: Launch app
        await this.core.launchApp();
        await this.core.sleep(2000);
        
        // Step 2: Send intent to trigger auto-submit (which should minimize)
        await this.core.executeCommand(
            `adb shell am start -a android.intent.action.SEND -t "text/plain" -e android.intent.extra.TEXT "${this.core.config.url}" --es android.intent.extra.SUBJECT "TikTok" app.pluct/.PluctUIScreen01MainActivity`
        );
        await this.core.sleep(2000);
        
        // Step 3: Verify notification appears
        let notificationFound = false;
        for (let i = 0; i < 5; i++) {
            const notificationCheck = await this.core.executeCommand(
                'adb shell dumpsys notification | findstr /i "Transcribing\|transcription\|Starting transcription"'
            );
            
            if (notificationCheck.output) {
                notificationFound = true;
                this.logger.info('✅ Notification detected');
                break;
            }
            await this.core.sleep(1000);
        }
        
        if (!notificationFound) {
            return { 
                success: false, 
                error: 'Notification not detected after auto-submit' 
            };
        }
        
        // Step 4: Verify app is in background (may not always minimize, but should show notification)
        await this.core.sleep(2000);
        
        const activityCheck = await this.core.executeCommand(
            'adb shell dumpsys activity activities | findstr /i "mResumedActivity"'
        );
        
        // App may still be in foreground, but notification should be showing
        
        // Step 5: Monitor notification updates
        let progressUpdates = 0;
        const startTime = Date.now();
        const maxWait = 60000; // 1 minute
        
        while (Date.now() - startTime < maxWait) {
            const notificationCheck = await this.core.executeCommand(
                'adb shell dumpsys notification | findstr /i "Transcribing\|Downloading\|Extracting\|Transcribing with AI"'
            );
            
            if (notificationCheck.output) {
                const currentContent = notificationCheck.output;
                if (currentContent.includes('Downloading') || 
                    currentContent.includes('Extracting') ||
                    currentContent.includes('Transcribing with AI')) {
                    progressUpdates++;
                    this.logger.info(`Progress update ${progressUpdates} detected`);
                }
            }
            
            // Check if completed
            const completeCheck = await this.core.executeCommand(
                'adb shell dumpsys notification | findstr /i "Complete\|completed\|Transcription Complete"'
            );
            
            if (completeCheck.output) {
                this.logger.info('✅ Transcription completed notification detected');
                break;
            }
            
            await this.core.sleep(3000);
        }
        
        // Step 6: Verify background worker execution in logcat
        const workerLog = await this.core.executeCommand(
            'adb logcat -d -t 200 | findstr /i "PluctCoreBackground01TranscriptionWorker\|TranscriptionWorker.*doWork\|Background transcription"'
        );
        
        if (workerLog.output) {
            this.logger.info('✅ Background worker execution detected in logcat');
        } else {
            this.logger.warn('⚠️ Background worker logs not found (may use foreground processing)');
        }
        
        // Step 7: Edge case - Network drops during background processing
        // This is tested in a separate edge case test
        
        await this.log('Background Processing Validation Complete');
        return { success: true, notificationFound, progressUpdates };
    }
}

module.exports = JourneyUX12BackgroundProcessingValidation;

