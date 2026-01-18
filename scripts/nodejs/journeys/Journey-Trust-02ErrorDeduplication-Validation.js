const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Trust-02ErrorDeduplication-Validation
 * Validates unified error state management to prevent duplicate error messages
 */
class JourneyTrust02ErrorDeduplicationValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Trust-02ErrorDeduplication-Validation';
    }

    async execute() {
        this.core.logger.info('Starting Error Deduplication Validation');
        
        // Step 1: Launch app
        await this.core.launchApp();
        await this.core.sleep(2000);
        
        // Step 2: Trigger an error (invalid URL)
        await this.core.tapByTestTag('url_input_field');
        await this.core.inputText('invalid-url-test');
        await this.core.tapByTestTag('extract_script_button');
        await this.core.sleep(2000);
        
        // Step 3: Dump UI and count error instances
        await this.core.dumpUIHierarchy();
        let uiDump = this.core.readLastUIDump() || '';
        
        // Count error banners
        const errorBannerMatches = (uiDump.match(/error_container|errorContainer|Error message/gi) || []).length;
        
        // Count error text instances
        const errorTextMatches = (uiDump.match(/Error|error/gi) || []).length;
        
        this.logger.info(`Error banner count: ${errorBannerMatches}, Error text count: ${errorTextMatches}`);
        
        // Step 4: Verify only one error banner visible
        if (errorBannerMatches > 1) {
            return { 
                success: false, 
                error: `Multiple error banners detected: ${errorBannerMatches}` 
            };
        }
        
        // Step 5: Check logcat for duplicate error logs using shared validator
        const errorCheck = await this.core.logcatValidator.checkForErrors(
            ['warn', 'expected'], // Exclude warnings and expected errors
            100
        );
        
        // Should have at least one error log, but not excessive duplicates
        if (errorCheck.errorCount > 5) {
            this.logger.warn(`⚠️ Multiple error logs detected: ${errorCheck.errorCount}`);
        }
        
        // Step 6: Test queue prompt suppresses error banner
        // Clear current error, then trigger queue scenario
        await this.core.tapByTestTag('url_input_field');
        await this.core.clearText();
        await this.core.inputText(this.core.config.url);
        await this.core.sleep(1000);
        
        // Disable network to trigger queue prompt
        await this.core.executeCommand('adb shell svc wifi disable');
        await this.core.sleep(2000);
        
        await this.core.dumpUIHierarchy();
        uiDump = this.core.readLastUIDump() || '';
        
        // Queue prompt should be visible, error banner should not
        const hasQueuePrompt = uiDump.includes('Save for Later') || uiDump.includes('Queue');
        const hasErrorBanner = uiDump.includes('error_container') || uiDump.includes('errorContainer');
        
        if (hasQueuePrompt && hasErrorBanner) {
            return { 
                success: false, 
                error: 'Error banner visible when queue prompt should suppress it' 
            };
        }
        
        // Re-enable network
        await this.core.executeCommand('adb shell svc wifi enable');
        await this.core.sleep(2000);
        
        // Step 7: Test duplicate toast suppression
        // Send intent to trigger toast
        await this.core.executeCommand(
            `adb shell am start -a android.intent.action.SEND -t "text/plain" -e android.intent.extra.TEXT "${this.core.config.url}" --es android.intent.extra.SUBJECT "TikTok" app.pluct/.PluctUIScreen01MainActivity`
        );
        await this.core.sleep(3000);
        
        // Check logcat for toast messages
        const toastLog = await this.core.executeCommand(
            'adb logcat -d -t 50 | findstr /i "TikTok video link received"'
        );
        
        const toastCount = (toastLog.output || '').split('TikTok video link received').length - 1;
        
        if (toastCount > 1) {
            return { 
                success: false, 
                error: `Duplicate toast detected: ${toastCount} instances` 
            };
        }
        
        this.core.logger.info('Error Deduplication Validation Complete');
        return { success: true };
    }
}

module.exports = JourneyTrust02ErrorDeduplicationValidation;

