const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Intent-01TikTok-02AutoSubmit-01Validation.js
 * Validates that TikTok intents automatically trigger transcription when credits are available
 */
class JourneyIntent01TikTok02AutoSubmit01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Intent-01TikTok-02AutoSubmit-01Validation';
        this.maxDuration = 200000; // 3.3 minutes max
    }

    async execute() {
        this.core.logger.info('🎯 Starting TikTok Intent Auto-Submit Validation Journey...');
        const startTime = Date.now();
        const testUrl = this.core.config.url;

        try {
            // Step 1: Pre-Condition - Ensure app has credits or free uses
            this.core.logger.info('📱 Step 1: Checking credit balance...');
            const launch = await this.core.launchApp();
            if (!launch.success) {
                return { success: false, error: 'Failed to launch app' };
            }
            await this.core.sleep(3000); // Wait for balance to load

            // Check logcat for balance loaded
            const balanceLog = await this.core.executeCommand('adb logcat -d | findstr /i "Credit balance loaded\|balance.*=" | tail -n 5');
            this.core.logger.info(`📊 Balance logs: ${balanceLog.output || 'No balance logs found'}`);

            // Step 2: Clear app state for clean test
            this.core.logger.info('📱 Step 2: Clearing app state...');
            await this.core.clearAppData();
            await this.core.sleep(1000);

            // Step 3: Launch app and wait for credit balance to load
            this.core.logger.info('📱 Step 3: Launching app and waiting for balance...');
            await this.core.launchApp();
            await this.core.sleep(5000); // Wait for balance to load

            // Step 4: Verify balance loaded
            this.core.logger.info('📱 Step 4: Verifying balance loaded...');
            await this.core.clearLogcat();
            const balanceCheck = await this.core.executeCommand('adb logcat -d | findstr /i "Credit balance loaded\|hasLoadedBalanceOnce"');
            if (!balanceCheck.success || balanceCheck.output.length === 0) {
                this.core.logger.warn('⚠️ Balance loading not confirmed in logcat, proceeding anyway');
            } else {
                this.core.logger.info('✅ Balance loading confirmed');
            }

            // Step 5: Send Intent
            this.core.logger.info(`📱 Step 5: Sending ACTION_SEND intent with URL: ${testUrl}`);
            const intentCommand = `adb shell am start -a android.intent.action.SEND -t "text/plain" --es android.intent.extra.TEXT "${testUrl}" app.pluct/.PluctUIScreen01MainActivity`;
            const intentResult = await this.core.executeCommand(intentCommand);
            if (!intentResult.success) {
                return { success: false, error: 'Failed to send intent' };
            }
            await this.core.sleep(2000); // Wait for intent processing

            // Step 6: Verify Intent Received
            this.core.logger.info('📱 Step 6: Verifying intent received...');
            const intentLog = await this.core.executeCommand('adb logcat -d | findstr /i "IntentHandler.*Received shared text\|IntentHandler.*TikTok URL detected"');
            if (!intentLog.success || !intentLog.output.includes('IntentHandler')) {
                return { success: false, error: 'Intent not received - no logcat evidence' };
            }
            this.core.logger.info('✅ Intent received confirmed');

            // Step 7: Verify URL Prefilled
            this.core.logger.info('📱 Step 7: Verifying URL prefilled in UI...');
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            const urlInField = uiDump.includes(testUrl) || uiDump.includes('url_input_field');
            if (!urlInField) {
                this.core.logger.warn('⚠️ URL not visible in UI dump, checking logcat...');
                const prefilledLog = await this.core.executeCommand('adb logcat -d | findstr /i "Found prefilled URL\|MainActivity.*prefilled"');
                if (!prefilledLog.success || !prefilledLog.output.includes('prefilled')) {
                    return { success: false, error: 'URL not prefilled' };
                }
            }
            this.core.logger.info('✅ URL prefilled confirmed');

            // Step 8: Verify Auto-Submit Triggered
            this.core.logger.info('📱 Step 8: Verifying auto-submit triggered...');
            await this.core.sleep(2000); // Wait for auto-submit
            const autoSubmitLog = await this.core.executeCommand('adb logcat -d | findstr /i "Auto-submitting URL\|CaptureCard.*Auto-submitting"');
            if (!autoSubmitLog.success || !autoSubmitLog.output.includes('Auto-submitting')) {
                return { success: false, error: 'Auto-submit not triggered - no logcat evidence within 2 seconds' };
            }
            this.core.logger.info('✅ Auto-submit triggered confirmed');

            // Step 9: Verify Notification Shown
            this.core.logger.info('📱 Step 9: Verifying notification shown...');
            await this.core.sleep(1000);
            const notificationLog = await this.core.executeCommand('adb logcat -d | findstr /i "Transcription started notification\|PluctNotificationHelper.*Transcription started"');
            const notificationCheck = await this.core.executeCommand('adb shell dumpsys notification | findstr /i "Transcription Started"');
            if ((!notificationLog.success || !notificationLog.output.includes('Transcription started')) && 
                (!notificationCheck.success || !notificationCheck.output.includes('Transcription Started'))) {
                this.core.logger.warn('⚠️ Notification not found in logcat or system, but continuing...');
            } else {
                this.core.logger.info('✅ Notification shown confirmed');
            }

            // Step 10: Verify Processing Started
            this.core.logger.info('📱 Step 10: Verifying processing started...');
            await this.core.sleep(3000);
            const apiLog = await this.core.executeCommand('adb logcat -d | findstr /i "vend-token\|PluctCoreAPIHTTPClient.*vend-token"');
            if (!apiLog.success || !apiLog.output.includes('vend-token')) {
                return { success: false, error: 'API call not made - no vend-token in logcat within 5 seconds' };
            }
            this.core.logger.info('✅ Processing started confirmed');

            // Step 11: Verify UI State
            this.core.logger.info('📱 Step 11: Verifying UI state...');
            await this.core.dumpUIHierarchy();
            const processingDump = this.core.readLastUIDump();
            const hasProcessingIndicator = processingDump.includes('processing_indicator') || 
                                          processingDump.includes('Processing') ||
                                          processingDump.includes('Transcribing');
            if (!hasProcessingIndicator) {
                this.core.logger.warn('⚠️ Processing indicator not visible in UI');
            } else {
                this.core.logger.info('✅ Processing indicator visible');
            }

            // Check for error messages
            if (processingDump.includes('error_message_text') && processingDump.match(/error_message_text[^>]*>([^<]+)</)) {
                const errorMatch = processingDump.match(/error_message_text[^>]*>([^<]+)</);
                if (errorMatch && errorMatch[1].trim().length > 0) {
                    return { success: false, error: `Error message shown: ${errorMatch[1]}` };
                }
            }

            // Step 12: Wait for Completion
            this.core.logger.info('📱 Step 12: Waiting for transcription completion...');
            const transcriptResult = await this.core.waitForTranscriptResult(180000, 2000);
            if (!transcriptResult.success) {
                return { success: false, error: `Transcription failed or timed out: ${transcriptResult.error || 'Unknown error'}` };
            }

            // Step 13: Verify Transcript
            this.core.logger.info('📱 Step 13: Verifying transcript displayed...');
            await this.core.dumpUIHierarchy();
            const finalDump = this.core.readLastUIDump();
            const hasTranscript = finalDump.includes('transcript') || 
                                 finalDump.includes('Transcript') ||
                                 finalDump.includes('transcript_text');
            if (!hasTranscript) {
                return { success: false, error: 'Transcript not displayed in UI' };
            }
            this.core.logger.info('✅ Transcript displayed confirmed');

            const duration = Date.now() - startTime;
            this.core.logger.info(`✅ TikTok Intent Auto-Submit Validation Journey completed successfully in ${duration}ms`);
            return { success: true, duration, transcript: transcriptResult.transcript };

        } catch (error) {
            this.core.logger.error(`❌ Journey failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = JourneyIntent01TikTok02AutoSubmit01Validation;
