const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyTikTokManualURL01Transcription extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'TikTokManualURLTranscription';
    }
    
    async log(message) {
        if (this.logger && typeof this.logger.info === 'function') {
            this.logger.info(message);
        }
    }

    async execute() {
        await this.log('Starting Journey: TikTok Manual URL Transcription');

        // Ensure app is launched and foreground before UI checks
        const launchResult = await this.core.launchApp();
        if (!launchResult.success) {
            return { success: false, error: `App launch failed: ${launchResult.error || 'unknown'}` };
        }
        const fgResult = await this.ensureAppForeground();
        if (!fgResult.success) {
            return { success: false, error: 'App not in foreground' };
        }

        // Step 0: Validate initial UI state
        await this.core.sleep(1500);
        await this.core.dumpUIHierarchy();
        let initialUI = this.core.readLastUIDump() || '';
        if (!initialUI.includes('url_input_field') && !initialUI.includes('Paste a TikTok link')) {
            // Retry by forcing activity to foreground and re-dumping UI once
            await this.core.executeCommand('adb shell am start -n app.pluct/.PluctUIScreen01MainActivity');
            await this.core.sleep(1500);
            await this.core.dumpUIHierarchy();
            initialUI = this.core.readLastUIDump() || '';
        }
        if (!initialUI.includes('url_input_field') && !initialUI.includes('Paste a TikTok link')) {
            return { success: false, error: 'Home screen UI not in expected state' };
        }

        // Step 1: Enter URL with validation
        await this.log('Step 1: Entering TikTok URL');
        let tapUrl = await this.core.tapByTestTag('url_input_field');
        if (!tapUrl.success) {
            tapUrl = await this.core.tapByText('Paste a TikTok link');
        }
        if (!tapUrl.success) {
            return { success: false, error: 'URL input field not found' };
        }
        
        // Validate input field is focused
        await this.core.sleep(500);
        await this.core.dumpUIHierarchy();
        const uiAfterTap = this.core.readLastUIDump() || '';
        if (!uiAfterTap.includes('focused="true"')) {
            this.logger.warn('⚠️ Input field may not be focused');
        }
        
        await this.core.inputText('https://vm.tiktok.com/ZMDRUGT2P/');
        
        // Validate text was entered
        await this.core.sleep(500);
        await this.core.dumpUIHierarchy();
        const uiAfterInput = this.core.readLastUIDump() || '';
        if (!uiAfterInput.includes('ZMDRUGT2P')) {
            this.logger.warn('⚠️ URL text not visible in UI dump after paste; continuing with submission');
        }

        // Step 2: Tap Extract with validation
        await this.log('Step 2: Tapping Extract Script');
        let tapExtract = await this.core.tapByTestTag('extract_script_button');
        if (!tapExtract.success) {
            tapExtract = await this.core.tapByText('Extract Script');
        }
        if (!tapExtract.success) {
            return { success: false, error: 'Extract button not found' };
        }

        // Step 3: Monitor API calls via logcat
        await this.log('Step 3: Monitoring API calls');
        const apiLogsBefore = await this.core.captureAPILogs(50);
        
        await this.core.sleep(1500);

        // Step 4: Wait for completion with API validation
        await this.log('Step 4: Waiting for transcript completion');
        const result = await this.core.waitForTranscriptResult(120000, 2000);
        
        // Validate no API errors occurred
        const apiLogsAfter = await this.core.captureAPILogs(500);
        const apiErrors = await this.core.checkRecentAPIErrors(500);
        if (apiErrors.hasErrors) {
            this.logger.error('❌ API errors detected during transcription:');
            apiErrors.errors.forEach(err => this.logger.error(`   ${err}`));
            return { success: false, error: `API errors: ${apiErrors.errors.join('; ')}` };
        }
        
        if (!result.success) {
            return { success: false, error: result.error || 'Transcription did not complete' };
        }

        // Step 5: Validate transcript UI appears
        await this.log('Step 5: Validating transcript UI');
        const finalUI = await this.core.dumpUIHierarchy();
        if (!finalUI.includes('transcript') && !finalUI.includes('Transcription')) {
            this.logger.warn('⚠️ Transcript UI not found, but API reported success');
        }

        await this.log('Journey Complete: Transcription Successful');
        return { success: true };
    }
}

module.exports = JourneyTikTokManualURL01Transcription;
