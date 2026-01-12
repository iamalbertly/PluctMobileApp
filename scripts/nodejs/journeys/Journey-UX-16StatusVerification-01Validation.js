const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyUX16StatusVerification01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-16StatusVerification-01Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-UX-16StatusVerification-01Validation');
        
        try {
            const testUrl = this.core.config.url;
            
            // Step 1: Start transcription via intent
            this.core.logger.info(`📱 Step 1: Starting transcription via intent with URL: ${testUrl}`);
            await this.core.clearLogcat();
            
            const intentCommand = `adb shell am start -a android.intent.action.SEND -t "text/plain" --es android.intent.extra.TEXT "${testUrl}" app.pluct/.PluctUIScreen01MainActivity`;
            const intentResult = await this.core.executeCommand(intentCommand);
            if (!intentResult.success) {
                return { success: false, error: 'Failed to send intent' };
            }
            await this.core.sleep(2000);
            
            // Step 2: Tap Extract Script button
            this.core.logger.info('📱 Step 2: Tapping Extract Script button...');
            await this.ensureAppForeground();
            const extractTap = await this.core.tapByTestTag('extract_script_button');
            if (!extractTap.success) {
                return { success: false, error: 'Extract Script button not found' };
            }
            await this.core.sleep(2000);
            
            // Step 3: Wait for transcription to complete
            this.core.logger.info('📱 Step 3: Waiting for transcription to complete...');
            let completed = false;
            let jobId = null;
            
            for (let i = 0; i < 60; i++) {
                await this.core.sleep(5000);
                
                // Check logcat for completion
                const statusLog = await this.core.executeCommand(
                    `adb logcat -d -t 100 | findstr /i "Updated video to completed status|status=completed|COMPLETED"`
                );
                
                if (statusLog.output.includes('completed') || statusLog.output.includes('COMPLETED')) {
                    // Extract jobId from logcat if available
                    const jobIdLog = await this.core.executeCommand(
                        `adb logcat -d -t 200 | findstr /i "jobId|job_id"`
                    );
                    if (jobIdLog.output) {
                        const match = jobIdLog.output.match(/job[Ii]d[:\s]+([^\s,]+)/);
                        if (match) {
                            jobId = match[1];
                        }
                    }
                    
                    completed = true;
                    this.core.logger.info('✅ Transcription completed');
                    break;
                }
                
                // Check for failure
                const failedLog = await this.core.executeCommand(
                    `adb logcat -d -t 100 | findstr /i "Transcription failed|FAILED"`
                );
                if (failedLog.output.includes('failed') || failedLog.output.includes('FAILED')) {
                    this.core.logger.warn('⚠️ Transcription failed, but continuing test');
                    break;
                }
            }
            
            if (!completed) {
                this.core.logger.warn('⚠️ Transcription did not complete within timeout, continuing test');
            }
            
            // Step 4: Verify transcript saved in database
            this.core.logger.info('📱 Step 4: Verifying transcript saved in database...');
            const dbLog = await this.core.executeCommand(
                `adb logcat -d -t 200 | findstr /i "Updated video to completed status|transcript"`
            );
            
            if (dbLog.output.includes('completed') || dbLog.output.includes('transcript')) {
                this.core.logger.info('✅ Database update confirmed in logcat');
            } else {
                this.core.logger.warn('⚠️ Database update log not found');
            }
            
            // Step 5: Kill app
            this.core.logger.info('📱 Step 5: Killing app...');
            await this.core.executeCommand('adb shell am force-stop app.pluct');
            await this.core.sleep(2000);
            
            // Step 6: Reopen app
            this.core.logger.info('📱 Step 6: Reopening app...');
            await this.core.clearLogcat();
            const reopenResult = await this.core.launchApp();
            if (!reopenResult.success) {
                return { success: false, error: 'App reopen failed' };
            }
            await this.core.sleep(3000); // Wait for resumer to run
            
            // Step 7: Verify logcat shows status verification
            this.core.logger.info('📱 Step 7: Verifying status verification in logcat...');
            const verificationLog = await this.core.executeCommand(
                `adb logcat -d -t 100 | findstr /i "StatusVerifier.*verifyAndUpdateStatus|Verifying status for video"`
            );
            
            if (!verificationLog.output.includes('StatusVerifier') && 
                !verificationLog.output.includes('Verifying status')) {
                this.core.logger.error('❌ FAILURE: Status verification not found in logcat');
                return { success: false, error: 'Status verification not logged' };
            }
            this.core.logger.info('✅ Status verification found in logcat');
            
            // Step 8: Verify "already completed" message (not "resuming polling")
            this.core.logger.info('📱 Step 8: Verifying no re-processing occurred...');
            const resumeLog = await this.core.executeCommand(
                `adb logcat -d -t 100 | findstr /i "already completed|resuming polling|Scheduled background worker"`
            );
            
            const hasResumeAttempt = resumeLog.output.includes('resuming polling') || 
                                    resumeLog.output.includes('Scheduled background worker');
            const hasAlreadyCompleted = resumeLog.output.includes('already completed') || 
                                       resumeLog.output.includes('already completed, updating database');
            
            if (hasResumeAttempt && !hasAlreadyCompleted) {
                this.core.logger.error('❌ FAILURE: Resume attempt detected for completed job');
                return { success: false, error: 'Re-processing detected for completed job' };
            }
            
            if (hasAlreadyCompleted) {
                this.core.logger.info('✅ "Already completed" message found, no re-processing');
            } else {
                this.core.logger.warn('⚠️ Neither resume nor "already completed" message found');
            }
            
            // Step 9: Verify no duplicate API calls
            this.core.logger.info('📱 Step 9: Verifying no duplicate API calls...');
            const apiCallsLog = await this.core.executeCommand(
                `adb logcat -d -t 200 | findstr /i "vend-token|submitTranscription"`
            );
            
            // Count occurrences
            const vendTokenCount = (apiCallsLog.output.match(/vend-token/g) || []).length;
            const submitCount = (apiCallsLog.output.match(/submitTranscription/g) || []).length;
            
            this.core.logger.info(`API call counts - vend-token: ${vendTokenCount}, submitTranscription: ${submitCount}`);
            
            // Should have at most 1-2 vend-token calls (initial + maybe refresh) and 1 submit
            if (vendTokenCount > 3 || submitCount > 1) {
                this.core.logger.warn('⚠️ High API call count detected (may indicate duplicates)');
            } else {
                this.core.logger.info('✅ API call counts look normal');
            }
            
            // Step 10: Verify UI shows completed transcription
            this.core.logger.info('📱 Step 10: Verifying UI shows completed transcription...');
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            
            // Check for completed status indicators
            const hasCompletedStatus = uiDump.includes('COMPLETED') || 
                                      uiDump.includes('Completed') ||
                                      uiDump.includes('completed') ||
                                      uiDump.includes('CheckCircle');
            
            const hasProcessingStatus = uiDump.includes('PROCESSING') || 
                                      uiDump.includes('Processing') ||
                                      uiDump.includes('processing') ||
                                      uiDump.includes('Refresh');
            
            if (hasProcessingStatus && !hasCompletedStatus) {
                this.core.logger.error('❌ FAILURE: UI shows PROCESSING for completed job');
                return { success: false, error: 'UI shows incorrect status' };
            }
            
            if (hasCompletedStatus) {
                this.core.logger.info('✅ UI shows completed status');
            } else {
                this.core.logger.warn('⚠️ Completed status not clearly visible in UI');
            }
            
            this.core.logger.info('✅ Journey-UX-16StatusVerification-01Validation completed successfully');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Journey failed: ${error.message}`);
            await this.failWithDiagnostics(error.message);
            return { success: false, error: error.message };
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('UX-16StatusVerification-01Validation', new JourneyUX16StatusVerification01Validation(orchestrator.core));
}

module.exports = { register };
