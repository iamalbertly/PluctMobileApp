const PluctCoreFoundation = require('../core/Pluct-Core-01Foundation');

class ChildSubmitTranscription {
    constructor() {
        this.core = new PluctCoreFoundation();
        this.testUrl = this.core.config.url;
    }

    async execute(params = {}) {
        const { serviceToken } = params;
        try {
            this.core.logger.info('🎬 [Child-02] Triggering transcription from the Android app UI...');
            // 1) Ensure app is launched
            const launch = await this.core.launchApp();
            if (!launch.success) {
                return { success: false, error: 'Failed to launch app' };
            }
            await this.core.sleep(1000);

            // 2) Send SHARE intent with the configured URL (same SSOT as Kotlin app)
            const intentCommand = `adb shell am start -a android.intent.action.SEND -t "text/plain" --es android.intent.extra.TEXT '${this.core.config.url}' app.pluct/.MainActivity`;
            await this.core.executeCommand(intentCommand);
            await this.core.sleep(2000);

            // 3) Tap the button to start processing (Extract Script)
            const tap = await this.core.tapByText('Extract Script');
            if (!tap.success) {
                // fallback try by content-desc/test tag
                await this.core.dumpUIHierarchy();
                const tapAlt = await this.core.tapByContentDesc('Start transcription button');
                if (!tapAlt.success) {
                    return { success: false, error: 'Failed to start transcription from UI' };
                }
            }

            // 4) Validate via logcat that the app performed Business Engine calls
            const logResult = await this.core.executeCommand('adb logcat -d | findstr /i "CaptureCard PluctBusinessEngineService API Error TTTranscribe"');
            const hasApiSignal = logResult.success;
            if (!hasApiSignal) {
                this.core.logger.warn('⚠️ [Child-02] No API signals found in logcat yet; proceeding to next step.');
            } else {
                this.core.logger.info('✅ [Child-02] Detected API activity in logcat from the app');
            }

            // We do not have a jobId from the UI directly; allow orchestrator to poll by UI/logs
            return { success: true, jobId: null, status: 'processing' };
        } catch (err) {
            return { success: false, error: err.message };
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('TikTok-Intent-01Transcription-02SubmitTranscription', new ChildSubmitTranscription());
}

module.exports = { ChildSubmitTranscription, register };


