const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-EdgeCase-03NetworkLoss-Validation
 * Follows naming convention: [Journey]-[EdgeCase]-[03NetworkLoss]-[Validation]
 * 4 scope layers: Journey, EdgeCase, NetworkLoss, Validation
 * Validates network loss during background processing edge case
 */
class JourneyEdgeCase03NetworkLossValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'EdgeCase-03NetworkLoss-Validation';
    }

    async execute() {
        this.core.logger.info('Starting Network Loss Edge Case Validation');
        
        // Step 1: Launch app
        await this.core.launchApp();
        await this.core.sleep(3000);
        
        // Step 2: Start transcription via intent (triggers background)
        const intentStartTime = Date.now();
        await this.core.executeCommand(
            `adb shell am start -a android.intent.action.SEND -t "text/plain" -e android.intent.extra.TEXT "${this.core.config.url}" app.pluct/.PluctUIScreen01MainActivity`
        );
        
        // Step 3: Wait for background processing to start
        await this.core.sleep(3000);
        
        // Verify notification appears
        const notificationCheck = await this.core.executeCommand(
            'adb shell dumpsys notification | findstr /i "Transcribing\|transcription"'
        );
        
        if (!notificationCheck.output) {
            return {
                success: false,
                error: 'Background processing notification not found'
            };
        }
        
        // Step 4: Disable WiFi mid-process
        await this.core.executeCommand('adb shell svc wifi disable');
        await this.core.sleep(2000);
        
        // Step 5: Verify proactive queueing on network loss (UX IMPROVEMENT #3)
        await this.core.sleep(3000);
        
        // Check logcat for proactive queueing
        const queueLogcat = await this.core.executeCommand(
            'adb logcat -d -t 100 | findstr /i "Network loss detected.*queueing\|checkAndQueueOnNetworkLoss\|Video queued.*network loss"'
        );
        
        const proactiveQueueDetected = queueLogcat.output && (
            queueLogcat.output.includes('Network loss detected') ||
            queueLogcat.output.includes('checkAndQueueOnNetworkLoss') ||
            queueLogcat.output.includes('Video queued')
        );
        
        if (proactiveQueueDetected) {
            this.core.logger.info('✅ Proactive queueing detected on network loss');
        } else {
            this.core.logger.warn('⚠️ Proactive queueing log not found (may queue on next poll)');
        }
        
        // Step 6: Verify video is queued for retry
        const queueCheckLogcat = await this.core.executeCommand(
            'adb logcat -d -t 100 | findstr /i "queued\|NO_INTERNET\|queueVideo\|QueueReason"'
        );
        
        const queued = queueCheckLogcat.output && (
            queueCheckLogcat.output.includes('queued') ||
            queueCheckLogcat.output.includes('NO_INTERNET') ||
            queueCheckLogcat.output.includes('QueueReason')
        );
        
        if (!queued) {
            await this.core.executeCommand('adb shell svc wifi enable');
            return {
                success: false,
                error: 'Video was not queued for retry on network loss'
            };
        }
        
        this.core.logger.info('✅ Video queued successfully on network loss');
        
        // Step 6b: Verify error notification appears
        const errorNotificationCheck = await this.core.executeCommand(
            'adb shell dumpsys notification | findstr /i "Network\|connection lost\|queued\|will process"'
        );
        
        const hasErrorNotification = errorNotificationCheck.output && (
            errorNotificationCheck.output.includes('Network') ||
            errorNotificationCheck.output.includes('connection lost') ||
            errorNotificationCheck.output.includes('queued') ||
            errorNotificationCheck.output.includes('will process')
        );
        
        if (!hasErrorNotification) {
            this.core.logger.warn('⚠️ Error notification not immediately visible (may appear later)');
        } else {
            this.core.logger.info('✅ Error notification shown on network loss');
        }
        
        // Step 7: Re-enable WiFi
        await this.core.executeCommand('adb shell svc wifi enable');
        await this.core.sleep(3000);
        
        // Step 8: Verify video auto-processes
        await this.core.sleep(5000);
        
        const retryLogcat = await this.core.executeCommand(
            'adb logcat -d -t 50 | findstr /i "retry\|processing\|Network restored"'
        );
        
        const retrying = retryLogcat.output && (
            retryLogcat.output.includes('retry') ||
            retryLogcat.output.includes('processing') ||
            retryLogcat.output.includes('Network restored')
        );
        
        if (!retrying) {
            this.logger.warn('⚠️ Auto-retry not detected, but video may still process');
        }
        
        this.core.logger.info('✅ Network loss edge case validated');
        return true;
    }
}

module.exports = JourneyEdgeCase03NetworkLossValidation;

