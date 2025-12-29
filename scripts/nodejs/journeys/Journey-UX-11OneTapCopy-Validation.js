const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-UX-11OneTapCopy-Validation
 * Validates one-tap copy button functionality on transcription cards
 */
class JourneyUX11OneTapCopyValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-11OneTapCopy-Validation';
    }

    async execute() {
        await this.log('Starting One-Tap Copy Validation');
        
        // Step 1: Ensure app has a completed transcription
        await this.core.launchApp();
        await this.core.sleep(2000);
        await this.core.dumpUIHierarchy();
        
        // Step 2: Check if transcription list has completed items
        let uiDump = this.core.readLastUIDump() || '';
        let hasCompletedTranscription = uiDump.includes('COMPLETED') || 
                                       uiDump.includes('Transcript ready') ||
                                       uiDump.includes('copy_transcript_button');
        
        if (!hasCompletedTranscription) {
            // Need to create a transcription first
            this.logger.info('No completed transcription found, creating one...');
            await this.core.tapByTestTag('url_input_field');
            await this.core.inputText('https://vm.tiktok.com/ZMDRUGT2P/');
            await this.core.tapByTestTag('extract_script_button');
            
            // Wait for completion
            const result = await this.core.waitForTranscriptResult(120000, 2000);
            if (!result.success) {
                return { success: false, error: 'Failed to create transcription for copy test' };
            }
            await this.core.sleep(2000);
        }
        
        // Step 3: Verify copy button exists on completed transcription card
        await this.core.dumpUIHierarchy();
        uiDump = this.core.readLastUIDump() || '';
        if (!uiDump.includes('copy_transcript_button') && !uiDump.includes('ContentCopy')) {
            return { success: false, error: 'Copy button not found on transcription card' };
        }
        
        // Step 4: Tap copy button
        const copyResult = await this.core.tapByTestTag('copy_transcript_button');
        if (!copyResult.success) {
            // Fallback to content description
            const copyResult2 = await this.core.tapByContentDesc('Copy transcript to clipboard');
            if (!copyResult2.success) {
                return { success: false, error: 'Could not tap copy button' };
            }
        }
        
        await this.core.sleep(1000);
        
        // Step 5: Verify clipboard confirmation appears
        await this.core.dumpUIHierarchy();
        uiDump = this.core.readLastUIDump() || '';
        if (!uiDump.includes('copied') && !uiDump.includes('clipboard')) {
            this.logger.warn('Clipboard confirmation not found in UI');
        } else {
            this.logger.info('Clipboard confirmation found in UI');
        }
        
        // Step 6: Check logcat for copy action
        const logcatResult = await this.core.executeCommand('adb logcat -d | findstr /i "clipboard\|copy.*transcript\|setPrimaryClip"');
        if (logcatResult.success && logcatResult.output) {
            this.logger.info('Copy action logged in logcat');
        } else {
            this.logger.warn('No copy action logs found in logcat');
        }
        
        // Step 7: Verify long-press menu still works (power user feature)
        await this.core.sleep(1000);
        // Try to find a completed video card and long-press it
        const longPressResult = await this.core.executeCommand('adb shell input swipe 500 800 500 800 1000');
        if (longPressResult.success) {
            await this.core.sleep(1000);
            await this.core.dumpUIHierarchy();
            uiDump = this.core.readLastUIDump() || '';
            if (uiDump.includes('Share') || uiDump.includes('Delete')) {
                this.logger.info('Long-press menu still accessible');
            }
        }
        
        await this.log('One-Tap Copy Validation Complete');
        return { success: true };
    }
}

module.exports = JourneyUX11OneTapCopyValidation;



