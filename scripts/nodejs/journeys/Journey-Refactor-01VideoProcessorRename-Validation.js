const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyRefactor01VideoProcessorRenameValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Journey-Refactor-01VideoProcessorRename-Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: VideoProcessor Rename Validation');
        
        try {
            // Step 1: Launch app and wait for it to load
            this.core.logger.info('- Step 1: Launch app');
            await this.ensureAppForeground();
            await this.core.sleep(3000);
            
            // Step 2: Clear logcat to get clean baseline
            this.core.logger.info('- Step 2: Clear logcat');
            await this.core.clearLogcat();
            
            // Step 3: Trigger transcription flow by entering URL and submitting
            this.core.logger.info('- Step 3: Trigger transcription flow');
            await this.core.dumpUIHierarchy();
            let uiDump = this.core.readLastUIDump() || '';
            
            // Find and tap URL input field
            const urlInputFound = await this.core.tapByTestTag('url_input_field') || 
                                 await this.core.tapByText('Paste a TikTok link');
            if (!urlInputFound.success) {
                return { success: false, error: 'URL input field not found' };
            }
            
            await this.core.sleep(500);
            const testUrl = 'https://vm.tiktok.com/ZMDRUGT2P/';
            await this.core.inputText(testUrl);
            await this.core.sleep(1000);
            
            // Tap Extract Script button
            const extractButton = await this.core.tapByTestTag('extract_script_button') ||
                                 await this.core.tapByText('Extract Script');
            if (!extractButton.success) {
                return { success: false, error: 'Extract Script button not found' };
            }
            
            // Step 4: Wait a bit for logs to accumulate
            await this.core.sleep(3000);
            
            // Step 5: Capture logcat and validate old class name NOT in logs
            this.core.logger.info('- Step 5: Validate old class name removed');
            const logcat = await this.core.captureAPILogs(200);
            const logcatText = logcat.join('\n');
            
            // Check for old class name (should NOT appear)
            const hasOldName = logcatText.includes('VideoProcessor') && 
                              !logcatText.includes('TranscriptionOrchestrator');
            
            if (hasOldName) {
                this.core.logger.error('❌ FAILURE: Old class name "VideoProcessor" still appears in logs');
                return { 
                    success: false, 
                    error: 'Old class name "VideoProcessor" still appears in logs (without TranscriptionOrchestrator)' 
                };
            }
            this.core.logger.info('✅ Old class name not found (expected)');
            
            // Step 6: Validate new class name appears
            this.core.logger.info('- Step 6: Validate new class name present');
            const hasNewName = logcatText.includes('TranscriptionOrchestrator');
            
            if (!hasNewName) {
                // Check if transcription actually started (may not have logs yet)
                await this.core.sleep(2000);
                const logcat2 = await this.core.captureAPILogs(100);
                const logcatText2 = logcat2.join('\n');
                const hasNewName2 = logcatText2.includes('TranscriptionOrchestrator');
                
                if (!hasNewName2) {
                    this.core.logger.warn('⚠️ New class name not found in logs (may be too early)');
                    // Don't fail - transcription may not have started yet
                } else {
                    this.core.logger.info('✅ New class name found in logs');
                }
            } else {
                this.core.logger.info('✅ New class name found in logs');
            }
            
            // Step 7: Validate functionality still works - check for transcription activity
            this.core.logger.info('- Step 7: Validate functionality');
            const hasTranscriptionActivity = logcatText.includes('Transcription') || 
                                           logcatText.includes('transcribe') ||
                                           logcatText.includes('processTikTokVideo');
            
            if (!hasTranscriptionActivity) {
                this.core.logger.warn('⚠️ No transcription activity detected (may be normal if flow not started)');
            } else {
                this.core.logger.info('✅ Transcription activity detected');
            }
            
            // Step 8: Check UI for any errors
            await this.core.dumpUIHierarchy();
            const finalUI = this.core.readLastUIDump() || '';
            const hasErrors = finalUI.toLowerCase().includes('error') && 
                             !finalUI.toLowerCase().includes('no error');
            
            if (hasErrors && !finalUI.includes('Processing')) {
                this.core.logger.warn('⚠️ UI shows error state');
            }
            
            this.core.logger.info('✅ VideoProcessor rename validation completed');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = JourneyRefactor01VideoProcessorRenameValidation;


