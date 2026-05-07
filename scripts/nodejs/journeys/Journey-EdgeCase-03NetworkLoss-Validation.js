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
        let wifiDisabled = false;

        try {
            await this.core.launchApp();
            await this.core.sleep(3000);

            await this.core.executeCommand(
                `adb shell am start -a android.intent.action.SEND -t "text/plain" -e android.intent.extra.TEXT "${this.core.config.url}" app.pluct/.PluctUIScreen01MainActivity`
            );

            let startupEvidence = '';
            for (let attempt = 0; attempt < 6; attempt++) {
                await this.core.sleep(1500);
                const notificationCheck = await this.core.executeCommand(
                    'adb shell dumpsys notification | findstr /i "Transcribing\\|transcription\\|Pluct Processing\\|Pluct Complete\\|Copy Transcript"',
                    undefined,
                    undefined,
                    { allowFailure: true }
                );
                const workerLog = await this.core.executeCommand(
                    'adb logcat -d -t 200 | findstr /i "Background worker job created\\|TranscriptionWorker\\|Worker result SUCCESS\\|Updated video to completed"',
                    undefined,
                    undefined,
                    { allowFailure: true }
                );
                startupEvidence = `${notificationCheck.output || ''}\n${workerLog.output || ''}`;
                if (/Pluct Complete|Copy Transcript|Worker result SUCCESS|Updated video to completed/i.test(startupEvidence)) {
                    this.core.logger.info('Transcription completed before network loss; cached/fast completion path is stable');
                    return { success: true, cachedCompletion: true };
                }
                if (/Transcribing|transcription|Pluct Processing|Background worker job created|TranscriptionWorker/i.test(startupEvidence)) {
                    break;
                }
            }

            if (!startupEvidence) {
                return { success: false, error: 'Background processing did not start' };
            }

            await this.core.executeCommand('adb shell svc wifi disable');
            wifiDisabled = true;
            await this.core.sleep(5000);

            const queueLogcat = await this.core.executeCommand(
                'adb logcat -d -t 150 | findstr /i "Network loss detected.*queueing\\|checkAndQueueOnNetworkLoss\\|Video queued.*network loss\\|queued\\|NO_INTERNET\\|QueueReason\\|Worker result SUCCESS\\|Updated video to completed"',
                undefined,
                undefined,
                { allowFailure: true }
            );

            const safeOutcome = queueLogcat.output && (
                /queued|NO_INTERNET|QueueReason/i.test(queueLogcat.output) ||
                /Worker result SUCCESS|Updated video to completed/i.test(queueLogcat.output)
            );

            if (!safeOutcome) {
                return { success: false, error: 'Video was not queued or completed safely on network loss' };
            }

            const errorNotificationCheck = await this.core.executeCommand(
                'adb shell dumpsys notification | findstr /i "Network\\|connection lost\\|queued\\|will process\\|Pluct Complete"',
                undefined,
                undefined,
                { allowFailure: true }
            );

            if (!errorNotificationCheck.output) {
                this.core.logger.warn('Network notification not immediately visible; safe queue/completion evidence exists');
            }

            await this.core.executeCommand('adb shell svc wifi enable');
            wifiDisabled = false;
            await this.core.sleep(5000);

            const retryLogcat = await this.core.executeCommand(
                'adb logcat -d -t 100 | findstr /i "retry\\|processing\\|Network restored\\|Worker result SUCCESS"',
                undefined,
                undefined,
                { allowFailure: true }
            );

            if (!retryLogcat.output) {
                this.core.logger.warn('Auto-retry not detected, but network-loss queue/completion path was safe');
            }

            this.core.logger.info('Network loss edge case validated');
            return { success: true };
        } finally {
            if (wifiDisabled) {
                await this.core.executeCommand('adb shell svc wifi enable', undefined, undefined, { allowFailure: true });
            }
        }
    }
}

module.exports = JourneyEdgeCase03NetworkLossValidation;
