const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class TTTranscribeIntegrationJourney extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'TTTranscribeIntegration';
    }

    async execute() {
        this.core.logger.info('🎯 Testing TTTranscribe Integration...');

        // 1) Launch app to home with a fresh local user so repeated validations do not hit stale 0-credit state.
        const captureReady = await this.core.resetAppToFreshCaptureState();
        if (!captureReady.success) return { success: false, error: captureReady.error };

        const creditSeed = await this.core.ensureLocalMobileCredits(3);
        if (!creditSeed.success) return { success: false, error: creditSeed.error };

        const errorCardCheck = await this.core.validateErrorCardUsability();
        if (!errorCardCheck.success) {
            return { success: false, error: errorCardCheck.error };
        }

        // 2) Enter test URL in the current capture card
        let urlTap = await this.core.tapByTestTag('video_url_input');
        if (!urlTap.success) {
            urlTap = await this.core.tapByText('TikTok URL');
            if (!urlTap.success) {
                const fallbackTap = await this.core.tapFirstEditText();
                if (!fallbackTap.success) return { success: false, error: 'URL field not found' };
            }
        }

        await this.core.inputText(this.core.config.url);

        // 3) Validate URL shape before spending a transcription attempt
        if (!/^https?:\/\/[^\s]*tiktok\.com\//i.test(this.core.config.url)) {
            return { success: false, error: 'Invalid TikTok URL' };
        }

        // 4) Trigger transcription through the current Extract Script control
        this.core.logger.info('🔍 Triggering transcription process...');
        const submitResult = await this.core.ui.buttonTapping.tapExtractScriptButton();
        if (!submitResult.success) {
            return { success: false, error: submitResult.error || 'Could not find Extract Script button' };
        }

        // 5) Wait for processing to start
        await this.core.sleep(3000);

        // 7) Check for processing indicators
        await this.core.dumpUIHierarchy();
        const uiDump = this.core.readLastUIDump();
        const hasProcessing = uiDump.includes('Processing') || 
                            uiDump.includes('Transcribing') || 
                            uiDump.includes('Video -> Text') ||
                            uiDump.includes('Audio -> Text') ||
                            uiDump.includes('Status') ||
                            uiDump.includes('Progress');

        if (!hasProcessing) {
            this.core.logger.warn('⚠️ No processing indicators found in UI');
        }

        const result = await this.core.waitForTranscriptResult(180000, 1500);
        if (!result.success) {
            const uiErrors = await this.core.scanUIForErrors();
            if (!uiErrors.success) {
                return { success: false, error: uiErrors.error };
            }
            const serviceLog = await this.core.executeCommand('adb logcat -d | findstr /i "TTTranscribe service error\\|upstream_error\\|service is waking\\|ttt/transcribe"', undefined, undefined, { allowFailure: true });
            if (serviceLog.success && /TTTranscribe service error|upstream_error|service is waking/i.test(serviceLog.output || '')) {
                return { success: false, error: 'TTTranscribe service unavailable during integration validation' };
            }
            return { success: false, error: `TTTranscribe result not completed: ${result.finalStage || 'unknown'}` };
        }

        const apiErrors = await this.core.checkRecentAPIErrors(500);
        if (!apiErrors.success) {
            return {
                success: false,
                error: `Backend errors detected after transcript completion: ${(apiErrors.errors || []).slice(0, 2).join(' | ')}`
            };
        }

        this.core.logger.info('✅ TTTranscribe Integration test completed');
        return { 
            success: true, 
            note: "TTTranscribe integration validated" 
        };
    }
}

module.exports = TTTranscribeIntegrationJourney;
