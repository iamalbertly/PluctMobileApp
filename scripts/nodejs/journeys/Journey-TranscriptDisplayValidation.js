const PluctCoreFoundation = require('../core/Pluct-Core-01Foundation');
const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class TranscriptDisplayValidationJourney extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'TranscriptDisplayValidation';
    }

    async execute() {
        try {
            this.core.logger.info('📝 [TranscriptDisplayValidation] Starting transcript display validation...');
            
            // 1. Launch app
            const launchResult = await this.core.launchApp();
            if (!launchResult.success) {
                return { success: false, error: 'Failed to launch app' };
            }
            await this.core.sleep(2000);
            
            // 2. Enter test URL
            const testUrl = 'https://www.tiktok.com/@thesunnahguy/video/7493203244727012630';
            this.core.logger.info(`📝 Entering test URL: ${testUrl}`);
            
            // Try multiple methods to input URL
            let inputSuccess = false;
            let tapResult = await this.core.tapByTestTag('url_input_field');
            if (!tapResult.success) {
                tapResult = await this.core.tapByText('Paste a TikTok link');
            }
            if (!tapResult.success) {
                tapResult = await this.core.tapFirstEditText();
            }
            
            if (tapResult.success) {
                await this.core.sleep(500);
                await this.core.inputText(testUrl);
                await this.core.sleep(1000);
                inputSuccess = true;
            }
            
            if (!inputSuccess) {
                return { success: false, error: 'Failed to enter URL' };
            }
            
            // 3. Tap Extract Script button
            this.core.logger.info('👆 Tapping Extract Script button...');
            let extractTap = await this.core.tapByTestTag('extract_script_button');
            if (!extractTap.success) {
                extractTap = await this.core.tapByText('Extract Script');
            }
            if (!extractTap.success) {
                return { success: false, error: 'Failed to tap Extract Script button' };
            }
            
            // 4. Wait for completion (monitor logcat)
            this.core.logger.info('⏳ Waiting for transcription to complete...');
            const startTime = Date.now();
            const maxWait = 180000; // 3 minutes
            let completed = false;
            
            while (Date.now() - startTime < maxWait) {
                const logcat = await this.core.executeCommand(
                    'adb logcat -d -t 50 | findstr /i "Transcription completed transcript saved COMPLETED processVideo_complete"'
                );
                
                if (logcat.output.includes('Transcription completed') ||
                    logcat.output.includes('processVideo_complete') ||
                    logcat.output.includes('COMPLETED')) {
                    this.core.logger.info('✅ Transcription completed');
                    completed = true;
                    break;
                }
                
                await this.core.sleep(5000);
            }
            
            if (!completed) {
                return { success: false, error: 'Transcription did not complete within timeout' };
            }
            
            // 5. Wait for UI update (give DB time to write)
            this.core.logger.info('⏳ Waiting for UI to update...');
            await this.core.sleep(2000);
            
            // 6. Dump UI hierarchy
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            
            // 7. Validate transcript appears in UI
            const hasTranscript = uiDump.includes('transcript') ||
                                 uiDump.includes('Transcript ready') ||
                                 uiDump.includes('Transcript available') ||
                                 (uiDump.match(/transcript/gi) || []).length > 0;
            
            if (!hasTranscript) {
                this.core.logger.error('❌ Transcript not found in UI');
                return {
                    success: false,
                    error: 'Transcript not found in UI after completion',
                    uiDump: uiDump.substring(0, 1000) // First 1000 chars for debugging
                };
            }
            
            // 8. Validate transcript preview text exists (check for actual text content)
            const hasTranscriptText = uiDump.match(/transcript.*[a-zA-Z]{10,}/i) !== null ||
                                     uiDump.includes('Transcript ready');
            
            this.core.logger.info(`✅ Transcript found: ${hasTranscript}, Text content: ${hasTranscriptText}`);
            
            return {
                success: hasTranscript && hasTranscriptText,
                transcriptFound: hasTranscript,
                transcriptTextFound: hasTranscriptText
            };
        } catch (err) {
            return { success: false, error: err.message };
        }
    }
}

function register(orchestrator) {
    const core = new PluctCoreFoundation();
    orchestrator.registerJourney('TranscriptDisplayValidation', new TranscriptDisplayValidationJourney(core));
}

module.exports = { TranscriptDisplayValidationJourney, register };
















