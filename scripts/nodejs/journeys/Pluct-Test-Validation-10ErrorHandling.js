const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class PluctTestValidationErrorHandling extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Pluct-Test-Validation-10ErrorHandling';
        this.maxDuration = 30000;
    }

    async execute() {
        try {
            this.core.logger.info('Validating error handling...');
            const launch = await this.core.ensureAppForeground();
            if (!launch.success) {
                return { success: false, error: `App launch failed: ${launch.error || 'unknown'}` };
            }

            let ready = await this.core.ensureCaptureCardReady();
            if (!ready.success) {
                ready = await this.core.resetAppToFreshCaptureState();
            }
            if (!ready.success) {
                return { success: false, error: `Capture card not ready: ${ready.error || 'unknown'}` };
            }

            await this.core.clearLogcat();

            const badTikTokUrl = 'https://vt.tiktok.com/ZShttps://vt.tiktok.com/ZS9bDyhttps://vt.tiktok.com/ZS9bDyvc';
            const invalidUrlResult = await this.core.inputText('url_input_field', badTikTokUrl);
            if (!invalidUrlResult.success) {
                return { success: false, error: 'Invalid URL input test failed' };
            }

            await this.core.sleep(2000);
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            const hasErrorMessage = uiDump.includes('Paste one TikTok link only') ||
                uiDump.includes('Paste one full TikTok link') ||
                uiDump.includes('TikTok link looks incomplete');

            if (!hasErrorMessage) {
                return { success: false, error: 'Error message not displayed for invalid URL' };
            }

            if (uiDump.includes('PROCESSING') || uiDump.includes('Starting transcription') || uiDump.includes('extract_script_button')) {
                return { success: false, error: 'Invalid URL reached processing UI instead of stopping inline' };
            }

            const apiLogs = await this.core.executeCommand(
                'adb logcat -d -t 120 | findstr /i "processTikTokVideo submit transcription job"',
                undefined,
                undefined,
                { allowFailure: true }
            );
            if ((apiLogs.output || '').trim()) {
                return { success: false, error: `Invalid URL triggered API work: ${apiLogs.output}` };
            }

            const clearResult = await this.core.tapByContentDesc('Clear URL');
            if (!clearResult.success) {
                return { success: false, error: 'Error recovery clear failed' };
            }

            const validUrl = process.env.TEST_TIKTOK_URL || 'https://vt.tiktok.com/ZS9bDyvc5/';
            const validUrlResult = await this.core.inputText('url_input_field', validUrl);
            if (!validUrlResult.success) {
                return { success: false, error: 'Error recovery valid input failed' };
            }

            await this.core.sleep(1000);
            await this.core.dumpUIHierarchy();
            const recoveryUI = this.core.readLastUIDump();
            if (!recoveryUI.includes('extract_script_button') && !recoveryUI.includes('Extract Script')) {
                return { success: false, error: 'Valid TikTok URL did not recover to ready state' };
            }

            this.core.logger.info('Error handling validation passed');
            return {
                success: true,
                details: { errorDisplay: true, blockedBeforeProcessing: true, errorRecovery: true }
            };
        } catch (error) {
            this.core.logger.error(`Error handling validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctTestValidationErrorHandling;
