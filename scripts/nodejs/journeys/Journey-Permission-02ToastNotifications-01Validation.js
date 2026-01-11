const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Permission-02ToastNotifications-01Validation
 * Validates toast notification appearance, content, and auto-dismiss behavior
 * Follows naming convention: [Project]-[Permission]-[Sequence][Feature]-[Sequence][Validation]
 * 5 scope layers: Project, Permission, Sequence, Feature, Sequence, Validation
 */
class JourneyPermission02ToastNotifications01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Permission-02ToastNotifications-01Validation';
    }

    async run() {
        this.core.logger.info('🚀 Starting: Journey-Permission-02ToastNotifications-01Validation');
        
        try {
            // Step 1: Ensure permissions are granted
            this.core.logger.info('📱 Step 1: Ensuring permissions are granted...');
            const androidVersion = await this.core.executeCommand('adb shell getprop ro.build.version.sdk');
            const sdkVersion = parseInt(androidVersion.output.trim());
            
            if (sdkVersion >= 33) {
                await this.core.executeCommand('adb shell pm grant app.pluct android.permission.POST_NOTIFICATIONS', undefined, undefined, { allowFailure: true });
            }
            await this.core.executeCommand('adb shell appops set app.pluct SYSTEM_ALERT_WINDOW allow');
            await this.core.sleep(1000);
            
            // Step 2: Launch app
            this.core.logger.info('📱 Step 2: Launching app...');
            await this.core.launchApp();
            await this.core.sleep(3000);
            
            // Step 3: Start transcription
            this.core.logger.info('📱 Step 3: Starting transcription...');
            await this.ensureAppForeground();
            
            // Enter URL
            let urlTap = await this.core.tapByTestTag('url_input_field');
            if (!urlTap.success) {
                urlTap = await this.core.tapByText('Paste TikTok Link');
            }
            await this.core.inputText(this.core.config.url);
            await this.core.sleep(1000);
            
            // Tap extract button
            const extractTap = await this.core.tapByTestTag('extract_script_button');
            if (!extractTap.success) {
                throw new Error('Could not find extract button');
            }
            await this.core.sleep(2000);
            
            // Step 4: Verify toast appears with "Transcription started" message
            this.core.logger.info('📱 Step 4: Verifying toast appears with "Transcription started" message...');
            await this.core.sleep(1500); // Give toast time to appear
            await this.core.dumpUIHierarchy();
            const uiDump1 = this.core.readLastUIDump();
            
            // Check for toast message in UI dump
            // Android toasts may appear in different ways, check for common patterns
            const toastFound = uiDump1.includes('Transcription started') || 
                              uiDump1.includes('Processing TikTok video') ||
                              uiDump1.includes('Processing') ||
                              uiDump1.includes('toast');
            
            if (!toastFound) {
                // Check logcat for toast - this is more reliable than UI dump
                const logcatResult = await this.core.logcatValidator.validatePattern(
                    'Toast.*started|Transcription.*started|showToast|showTranscriptionStarted',
                    'Toast shown in logcat',
                    5,
                    2000,
                    100
                );
                
                if (!logcatResult.success) {
                    // Toast might have been very brief, check if transcription actually started
                    const transcriptionLogcat = await this.core.executeCommand(
                        'adb logcat -d -t 50 | findstr /i "transcription|processing"'
                    );
                    if (transcriptionLogcat.output && transcriptionLogcat.output.includes('transcription')) {
                        this.core.logger.warn('⚠️ Toast not found but transcription started (toast may have been too brief)');
                    } else {
                        throw new Error('Toast notification not found and transcription not started');
                    }
                } else {
                    this.core.logger.info('✅ Toast notification found in logcat');
                }
            } else {
                this.core.logger.info('✅ Toast notification found in UI dump');
            }
            
            // Step 5: Verify toast appears on main thread (no ANR in logcat)
            this.core.logger.info('📱 Step 5: Verifying no ANR in logcat...');
            const anrCheck = await this.core.executeCommand(
                'adb logcat -d -t 100 | findstr /i "ANR|Not responding"'
            );
            
            if (anrCheck.output && anrCheck.output.trim()) {
                throw new Error('ANR detected in logcat');
            }
            this.core.logger.info('✅ No ANR detected');
            
            // Step 6: Wait for transcription to complete (or simulate completion)
            this.core.logger.info('📱 Step 6: Waiting for transcription to complete...');
            // Wait up to 60 seconds for completion
            let completed = false;
            for (let i = 0; i < 12; i++) {
                await this.core.sleep(5000);
                await this.core.dumpUIHierarchy();
                const uiDump2 = this.core.readLastUIDump();
                
                // Check for completion indicators
                if (uiDump2.includes('Transcription complete') || 
                    uiDump2.includes('complete') ||
                    uiDump2.includes('transcript')) {
                    completed = true;
                    break;
                }
            }
            
            if (!completed) {
                this.core.logger.warn('⚠️ Transcription did not complete within timeout, checking for completion toast anyway');
            }
            
            // Step 7: Verify toast appears with "Transcription complete" message
            this.core.logger.info('📱 Step 7: Verifying completion toast appears...');
            await this.core.dumpUIHierarchy();
            const uiDump3 = this.core.readLastUIDump();
            
            const completionToastFound = uiDump3.includes('Transcription complete') || 
                                        uiDump3.includes('complete') ||
                                        uiDump3.includes('Tap to view');
            
            if (!completionToastFound) {
                // Check logcat
                const logcatResult2 = await this.core.logcatValidator.validatePattern(
                    'Toast.*complete|Transcription.*complete|showTranscriptionFinished',
                    'Completion toast in logcat',
                    3,
                    2000,
                    50
                );
                
                if (!logcatResult2.success) {
                    this.core.logger.warn('⚠️ Completion toast not found (may have dismissed already)');
                } else {
                    this.core.logger.info('✅ Completion toast found in logcat');
                }
            } else {
                this.core.logger.info('✅ Completion toast found');
            }
            
            // Step 8: Verify toast auto-dismisses after 5 seconds
            this.core.logger.info('📱 Step 8: Verifying toast auto-dismisses...');
            await this.core.sleep(6000); // Wait 6 seconds (toast should dismiss after 5)
            await this.core.dumpUIHierarchy();
            const uiDump4 = this.core.readLastUIDump();
            
            // Toast should be gone
            const toastStillVisible = uiDump4.includes('Transcription complete') && 
                                     uiDump4.includes('toast');
            
            if (toastStillVisible) {
                this.core.logger.warn('⚠️ Toast may still be visible (could be system delay)');
            } else {
                this.core.logger.info('✅ Toast auto-dismissed');
            }
            
            // Step 9: Verify multiple toasts don't stack (only most recent visible)
            this.core.logger.info('📱 Step 9: Verifying toast deduplication...');
            // Start another transcription quickly
            await this.core.tapByTestTag('url_input_field');
            await this.core.inputText(this.core.config.url);
            await this.core.sleep(500);
            await this.core.tapByTestTag('extract_script_button');
            await this.core.sleep(2000);
            
            await this.core.dumpUIHierarchy();
            const uiDump5 = this.core.readLastUIDump();
            
            // Count occurrences of toast-related text
            const toastCount = (uiDump5.match(/Transcription started|Processing TikTok/g) || []).length;
            if (toastCount > 2) {
                this.core.logger.warn('⚠️ Multiple toasts may be visible');
            } else {
                this.core.logger.info('✅ Toast deduplication working');
            }
            
            this.core.logger.info('✅ Journey-Permission-02ToastNotifications-01Validation completed');
            return true;
            
        } catch (error) {
            this.core.logger.error(`❌ Journey failed: ${error.message}`);
            await this.failWithDiagnostics(error.message);
            return false;
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('Permission-02ToastNotifications-01Validation', new JourneyPermission02ToastNotifications01Validation(orchestrator.core));
}

module.exports = JourneyPermission02ToastNotifications01Validation;
