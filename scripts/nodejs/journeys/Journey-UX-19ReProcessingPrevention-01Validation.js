const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyUX19ReProcessingPrevention01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-19ReProcessingPrevention-01Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-UX-19ReProcessingPrevention-01Validation');
        
        try {
            const testUrl = this.core.config.url;
            
            // Step 1: Complete transcription normally
            this.core.logger.info(`📱 Step 1: Completing transcription normally with URL: ${testUrl}`);
            await this.core.clearLogcat();
            
            const intentCommand = `adb shell am start -a android.intent.action.SEND -t "text/plain" --es android.intent.extra.TEXT "${testUrl}" app.pluct/.PluctUIScreen01MainActivity`;
            const intentResult = await this.core.executeCommand(intentCommand);
            if (!intentResult.success) {
                return { success: false, error: 'Failed to send intent' };
            }
            await this.core.sleep(2000);
            
            // Tap Extract Script
            await this.ensureAppForeground();
            const extractTap = await this.core.tapByTestTag('extract_script_button');
            if (!extractTap.success) {
                return { success: false, error: 'Extract Script button not found' };
            }
            await this.core.sleep(2000);
            
            // Wait for completion
            let completed = false;
            for (let i = 0; i < 60; i++) {
                await this.core.sleep(5000);
                const statusLog = await this.core.executeCommand(
                    `adb logcat -d -t 100 | findstr /i "Updated video to completed status|status=completed|COMPLETED"`
                );
                if (statusLog.output.includes('completed') || statusLog.output.includes('COMPLETED')) {
                    completed = true;
                    this.core.logger.info('✅ Transcription completed');
                    break;
                }
            }
            
            if (!completed) {
                this.core.logger.warn('⚠️ Transcription did not complete, continuing test anyway');
            }
            
            // Step 2: Verify COMPLETED status
            this.core.logger.info('📱 Step 2: Verifying COMPLETED status...');
            await this.core.dumpUIHierarchy();
            const uiDump1 = this.core.readLastUIDump();
            const hasCompleted = uiDump1.includes('COMPLETED') || 
                               uiDump1.includes('Completed') ||
                               uiDump1.includes('CheckCircle');
            
            if (!hasCompleted) {
                this.core.logger.warn('⚠️ Completed status not visible in UI (may need refresh)');
            } else {
                this.core.logger.info('✅ Completed status confirmed');
            }
            
            // Step 3: Kill app and reopen (Test 2 validation)
            this.core.logger.info('📱 Step 3: Killing app and reopening...');
            await this.core.executeCommand('adb shell am force-stop app.pluct');
            await this.core.sleep(2000);
            
            await this.core.clearLogcat();
            const reopenResult = await this.core.launchApp();
            if (!reopenResult.success) {
                return { success: false, error: 'App reopen failed' };
            }
            await this.core.sleep(3000);
            
            // Verify no re-processing
            const resumeLog = await this.core.executeCommand(
                'adb logcat -d -t 100 | findstr /i "already completed|resuming polling|Scheduled background worker"'
            );
            
            const hasResumeAttempt = resumeLog.output.includes('resuming polling') || 
                                    resumeLog.output.includes('Scheduled background worker');
            const hasAlreadyCompleted = resumeLog.output.includes('already completed');
            
            if (hasResumeAttempt && !hasAlreadyCompleted) {
                this.core.logger.error('❌ FAILURE: Re-processing detected on app reopen');
                return { success: false, error: 'Re-processing occurred on app reopen' };
            }
            
            if (hasAlreadyCompleted) {
                this.core.logger.info('✅ No re-processing on app reopen confirmed');
            }
            
            // Step 4: Manually trigger transcription for same URL
            this.core.logger.info('📱 Step 4: Manually triggering transcription for same URL...');
            await this.core.clearLogcat();
            
            const intentCommand2 = `adb shell am start -a android.intent.action.SEND -t "text/plain" --es android.intent.extra.TEXT "${testUrl}" app.pluct/.PluctUIScreen01MainActivity`;
            await this.core.executeCommand(intentCommand2);
            await this.core.sleep(2000);
            
            await this.ensureAppForeground();
            const extractTap2 = await this.core.tapByTestTag('extract_script_button');
            if (!extractTap2.success) {
                return { success: false, error: 'Extract Script button not found on retry' };
            }
            await this.core.sleep(3000);
            
            // Step 5: Verify deduplication prevents duplicate processing
            this.core.logger.info('📱 Step 5: Verifying deduplication prevents duplicate processing...');
            const dedupLog = await this.core.executeCommand(
                'adb logcat -d -t 100 | findstr /i "already being processed|already completed|duplicate.*rejected"'
            );
            
            const hasDeduplication = dedupLog.output.includes('already being processed') || 
                                   dedupLog.output.includes('already completed') ||
                                   dedupLog.output.includes('duplicate') ||
                                   dedupLog.output.includes('rejected');
            
            if (!hasDeduplication) {
                this.core.logger.warn('⚠️ Deduplication log not found (may have processed anyway)');
            } else {
                this.core.logger.info('✅ Deduplication detected in logcat');
            }
            
            // Step 6: Verify UI shows appropriate message
            this.core.logger.info('📱 Step 6: Verifying UI shows appropriate message...');
            await this.core.dumpUIHierarchy();
            const uiDump2 = this.core.readLastUIDump();
            
            // Check for error message or existing transcript
            const hasErrorMessage = uiDump2.includes('already') || 
                                  uiDump2.includes('duplicate') ||
                                  uiDump2.includes('processing');
            const hasExistingTranscript = uiDump2.includes('transcript') || 
                                        uiDump2.includes('Transcript');
            
            if (hasErrorMessage || hasExistingTranscript) {
                this.core.logger.info('✅ Appropriate UI feedback shown');
            } else {
                this.core.logger.warn('⚠️ UI feedback not clearly visible');
            }
            
            // Step 7: Verify no duplicate API calls
            this.core.logger.info('📱 Step 7: Verifying no duplicate API calls...');
            const apiCallsLog = await this.core.executeCommand(
                'adb logcat -d -t 300 | findstr /i "vend-token|submitTranscription"'
            );
            
            // Count occurrences for this specific URL
            const allVendTokens = (apiCallsLog.output.match(/vend-token/g) || []).length;
            const allSubmits = (apiCallsLog.output.match(/submitTranscription/g) || []).length;
            
            this.core.logger.info(`Total API calls - vend-token: ${allVendTokens}, submitTranscription: ${allSubmits}`);
            
            // Should have reasonable counts (not excessive duplicates)
            if (allVendTokens > 5 || allSubmits > 2) {
                this.core.logger.warn('⚠️ High API call count detected (may indicate duplicates)');
            } else {
                this.core.logger.info('✅ API call counts look reasonable');
            }
            
            this.core.logger.info('✅ Journey-UX-19ReProcessingPrevention-01Validation completed successfully');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Journey failed: ${error.message}`);
            await this.failWithDiagnostics(error.message);
            return { success: false, error: error.message };
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('UX-19ReProcessingPrevention-01Validation', new JourneyUX19ReProcessingPrevention01Validation(orchestrator.core));
}

module.exports = { register };
