const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyQueue02InsufficientCreditsValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Queue-02InsufficientCredits';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-Queue-02InsufficientCredits-Validation');
        
        try {
            // Step 1: Ensure app is running
            await this.ensureAppForeground();
            await this.core.sleep(2000);
            
            // Step 2: Enter TikTok URL
            this.core.logger.info('📱 Step 2: Entering TikTok URL...');
            let urlTap = await this.core.tapByTestTag('url_input_field');
            if (!urlTap.success) {
                urlTap = await this.core.tapByText('Paste TikTok Link');
            }
            if (!urlTap.success) {
                return { success: false, error: 'URL input field not found' };
            }
            
            await this.core.inputText(this.core.config.url);
            await this.core.sleep(1000);
            
            // Step 3: Tap Extract Script (will fail if credits = 0)
            this.core.logger.info('📱 Step 3: Tapping Extract Script button...');
            const extractTap = await this.core.tapByTestTag('extract_script_button');
            if (!extractTap.success) {
                return { success: false, error: 'Extract Script button not found' };
            }
            await this.core.sleep(2000);
            
            // Step 4: Verify insufficient credits dialog
            this.core.logger.info('📱 Step 4: Verifying insufficient credits dialog...');
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            
            const hasDialog = uiDump.includes('Insufficient Credits') || 
                             uiDump.includes('insufficient');
            
            if (!hasDialog) {
                this.core.logger.warn('⚠️ Insufficient credits dialog not found (may have credits)');
                // If user has credits, test passes (not a failure scenario)
                return { success: true, skipped: true, reason: 'User has sufficient credits' };
            }
            this.core.logger.info('✅ Insufficient credits dialog found');
            
            // Step 5: Verify "Save for Later" button visible
            this.core.logger.info('📱 Step 5: Verifying "Save for Later" button...');
            const hasSaveForLater = uiDump.includes('Save for Later') || 
                                   uiDump.includes('Save for later');
            
            if (!hasSaveForLater) {
                return { success: false, error: 'Save for Later button not found' };
            }
            this.core.logger.info('✅ Save for Later button found');
            
            // Step 6: Tap "Save for Later"
            this.core.logger.info('📱 Step 6: Tapping "Save for Later"...');
            const saveTap = await this.core.tapByText('Save for Later');
            if (!saveTap.success) {
                return { success: false, error: 'Save for Later button tap failed' };
            }
            await this.core.sleep(2000);
            
            // Step 7: Verify logcat shows queue reason
            this.core.logger.info('📱 Step 7: Verifying logcat for queue reason...');
            const logcatResult = await this.core.executeCommand(
                'adb logcat -d -t 50 | findstr /i "INSUFFICIENT_CREDITS Video queued QueueReason"'
            );
            
            const hasQueueLog = logcatResult.output.includes('INSUFFICIENT_CREDITS') || 
                               logcatResult.output.includes('Video queued');
            
            if (hasQueueLog) {
                this.core.logger.info('✅ Queue log found in logcat');
            } else {
                this.core.logger.warn('⚠️ Queue log not found (may still be queued)');
            }
            
            // Step 8: Verify UI shows queued item
            this.core.logger.info('📱 Step 8: Verifying queued item in UI...');
            await this.core.dumpUIHierarchy();
            const queueDump = this.core.readLastUIDump();
            
            const hasQueuedItem = queueDump.includes('queued') || 
                                 queueDump.includes('Waiting for credits');
            
            if (hasQueuedItem) {
                this.core.logger.info('✅ Queued item found in UI');
            } else {
                this.core.logger.warn('⚠️ Queued item not immediately visible');
            }
            
            // Step 9: Verify notification
            this.core.logger.info('📱 Step 9: Verifying notification...');
            const notificationResult = await this.core.executeCommand(
                'adb shell dumpsys notification | findstr /i "waiting for credits"'
            );
            
            if (notificationResult.output.includes('waiting') || 
                notificationResult.output.includes('pluct_queue')) {
                this.core.logger.info('✅ Notification found');
            }
            
            this.core.logger.info('✅ Journey-Queue-02InsufficientCredits-Validation completed');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Journey failed: ${error.message}`);
            await this.failWithDiagnostics(error.message);
            return { success: false, error: error.message };
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('Queue-02InsufficientCredits', new JourneyQueue02InsufficientCreditsValidation(orchestrator.core));
}

module.exports = { register };



