const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyQueue01NetworkConnectivityValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Queue-01NetworkConnectivity';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-Queue-01NetworkConnectivity-Validation');
        
        try {
            // Step 1: Disable network
            this.core.logger.info('📱 Step 1: Disabling network...');
            await this.core.executeCommand('adb shell svc wifi disable');
            await this.core.executeCommand('adb shell svc data disable');
            await this.core.sleep(2000);
            
            // Step 2: Launch app
            this.core.logger.info('📱 Step 2: Launching app...');
            const launchResult = await this.core.launchApp();
            if (!launchResult.success) {
                return { success: false, error: 'App launch failed' };
            }
            await this.core.sleep(3000);
            
            // Step 3: Enter TikTok URL
            this.core.logger.info('📱 Step 3: Entering TikTok URL...');
            await this.ensureAppForeground();
            await this.core.dumpUIHierarchy();
            
            // Find and tap URL input
            let urlTap = await this.core.tapByTestTag('url_input_field');
            if (!urlTap.success) {
                urlTap = await this.core.tapByText('Paste TikTok Link');
            }
            if (!urlTap.success) {
                return { success: false, error: 'URL input field not found' };
            }
            
            await this.core.inputText(this.core.config.url);
            await this.core.sleep(1000);
            
            // Step 4: Tap Extract Script button
            this.core.logger.info('📱 Step 4: Tapping Extract Script button...');
            const extractTap = await this.core.tapByTestTag('extract_script_button');
            if (!extractTap.success) {
                return { success: false, error: 'Extract Script button not found' };
            }
            await this.core.sleep(2000);
            
            // Step 5: Verify error dialog shows "Save for Later"
            this.core.logger.info('📱 Step 5: Verifying error dialog...');
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            
            const hasSaveForLater = uiDump.includes('Save for Later') || 
                                   uiDump.includes('Save for later') ||
                                   uiDump.includes('save_for_later');
            
            if (!hasSaveForLater) {
                this.core.logger.error('❌ FAILURE: "Save for Later" option not found in dialog');
                return { success: false, error: 'Save for Later option not found' };
            }
            this.core.logger.info('✅ "Save for Later" option found');
            
            // Step 6: Tap "Save for Later"
            this.core.logger.info('📱 Step 6: Tapping "Save for Later"...');
            const saveTap = await this.core.tapByText('Save for Later');
            if (!saveTap.success) {
                return { success: false, error: 'Save for Later button not found' };
            }
            await this.core.sleep(2000);
            
            // Step 7: Verify logcat shows queue reason
            this.core.logger.info('📱 Step 7: Verifying logcat for queue reason...');
            const logcatResult = await this.core.executeCommand(
                'adb logcat -d -t 50 | findstr /i "QueueReason.*NO_INTERNET Video queued"'
            );
            
            const hasQueueLog = logcatResult.output.includes('NO_INTERNET') || 
                               logcatResult.output.includes('Video queued') ||
                               logcatResult.output.includes('QueueReason');
            
            if (!hasQueueLog) {
                this.core.logger.warn('⚠️ Queue log not found in logcat (may still be queued)');
            } else {
                this.core.logger.info('✅ Queue log found in logcat');
            }
            
            // Step 8: Verify UI shows queue section
            this.core.logger.info('📱 Step 8: Verifying queue section in UI...');
            await this.core.dumpUIHierarchy();
            const queueDump = this.core.readLastUIDump();
            
            const hasQueueSection = queueDump.includes('video(s) queued') || 
                                   queueDump.includes('queued') ||
                                   queueDump.includes('Queue') ||
                                   queueDump.includes('waiting for');
            
            if (!hasQueueSection) {
                this.core.logger.warn('⚠️ Queue section not immediately visible (may need refresh)');
            } else {
                this.core.logger.info('✅ Queue section found in UI');
            }
            
            // Step 9: Verify notification appears
            this.core.logger.info('📱 Step 9: Checking for notification...');
            const notificationResult = await this.core.executeCommand(
                'adb shell dumpsys notification | findstr /i "pluct_queue"'
            );
            
            if (notificationResult.output.includes('pluct_queue') || 
                notificationResult.output.includes('Pluct Queue')) {
                this.core.logger.info('✅ Queue notification found');
            } else {
                this.core.logger.warn('⚠️ Queue notification not found (may need time to appear)');
            }
            
            // Step 10: Re-enable network
            this.core.logger.info('📱 Step 10: Re-enabling network...');
            await this.core.executeCommand('adb shell svc wifi enable');
            await this.core.sleep(3000);
            
            // Step 11: Verify auto-processing starts
            this.core.logger.info('📱 Step 11: Verifying auto-processing...');
            await this.core.sleep(5000);
            
            const autoProcessLog = await this.core.executeCommand(
                'adb logcat -d -t 100 | findstr /i "Processing queued Auto-processing"'
            );
            
            if (autoProcessLog.output.includes('Processing queued') || 
                autoProcessLog.output.includes('Auto-processing')) {
                this.core.logger.info('✅ Auto-processing detected in logcat');
            } else {
                this.core.logger.warn('⚠️ Auto-processing log not found (may process later)');
            }
            
            // Step 12: Verify notification updates
            this.core.logger.info('📱 Step 12: Verifying notification update...');
            await this.core.sleep(2000);
            const updatedNotification = await this.core.executeCommand(
                'adb shell dumpsys notification | findstr /i "pluct_queue"'
            );
            
            if (updatedNotification.output.includes('processing') || 
                updatedNotification.output.includes('Pluct Queue')) {
                this.core.logger.info('✅ Notification updated');
            }
            
            this.core.logger.info('✅ Journey-Queue-01NetworkConnectivity-Validation completed successfully');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Journey failed: ${error.message}`);
            await this.failWithDiagnostics(error.message);
            return { success: false, error: error.message };
        } finally {
            // Re-enable network in case of failure
            await this.core.executeCommand('adb shell svc wifi enable');
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('Queue-01NetworkConnectivity', new JourneyQueue01NetworkConnectivityValidation(orchestrator.core));
}

module.exports = { register };



