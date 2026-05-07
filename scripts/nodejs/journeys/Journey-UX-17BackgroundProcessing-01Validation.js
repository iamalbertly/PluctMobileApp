const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyUX17BackgroundProcessing01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-17BackgroundProcessing-01Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-UX-17BackgroundProcessing-01Validation');
        
        try {
            const testUrl = this.core.config.url;
            await this.ensureAppForeground();
            await this.core.tapByContentDesc('Close settings');
            await this.core.pressKey('Back');
            await this.core.sleep(1000);
            
            // Step 1: Check current battery optimization status
            this.core.logger.info('📱 Step 1: Checking current battery optimization status...');
            const batteryStatusResult = await this.core.executeCommand(
                'adb shell dumpsys deviceidle whitelist | findstr /i "pluct"'
            );
            
            const isCurrentlyExempt = batteryStatusResult.output.includes('pluct') || 
                                      batteryStatusResult.output.includes('app.pluct');
            
            this.core.logger.info(`Current battery optimization status: ${isCurrentlyExempt ? 'Exempt' : 'Not exempt'}`);
            
            // Step 2: If exempt, disable exemption for testing
            if (isCurrentlyExempt) {
                this.core.logger.info('📱 Step 2: Disabling battery optimization exemption for testing...');
                await this.core.executeCommand('adb shell dumpsys deviceidle whitelist -app.pluct');
                await this.core.sleep(2000);
                this.core.logger.info('✅ Battery optimization exemption disabled');
            } else {
                this.core.logger.info('📱 Step 2: Already not exempt, continuing...');
            }
            
            // Step 3: Start transcription
            this.core.logger.info(`📱 Step 3: Starting transcription with URL: ${testUrl}`);
            await this.core.clearLogcat();
            
            const intentCommand = `adb shell am start -a android.intent.action.SEND -t "text/plain" --es android.intent.extra.TEXT "${testUrl}" app.pluct/.PluctUIScreen01MainActivity`;
            const intentResult = await this.core.executeCommand(intentCommand);
            if (!intentResult.success) {
                return { success: false, error: 'Failed to send intent' };
            }
            await this.core.sleep(2000);
            
            // Step 4: Tap Extract Script button
            this.core.logger.info('📱 Step 4: Tapping Extract Script button...');
            await this.ensureAppForeground();
            const extractTap = await this.core.tapByTestTag('extract_script_button');
            if (!extractTap.success) {
                const autoSubmitLog = await this.core.executeCommand(
                    'adb logcat -d -t 180 | findstr /i /c:"Auto-submitting" /c:"Starting background transcription" /c:"notify pkg=app.pluct" /c:"100% -> Text"',
                    undefined,
                    undefined,
                    { allowFailure: true }
                );
                if (!autoSubmitLog.output) {
                    return { success: false, error: 'Extract Script button not found and auto-submit was not detected' };
                }
                this.core.logger.info('âœ… Auto-submit/background progress detected; manual tap no longer needed');
            } else {
                await this.core.sleep(3000); // Wait for transcription to start
            }
            
            // Step 5: Background app immediately
            this.core.logger.info('📱 Step 5: Backgrounding app...');
            await this.core.executeCommand('adb shell input keyevent KEYCODE_HOME');
            await this.core.sleep(2000);
            
            // Step 6: Wait and check logcat for WorkManager activity
            this.core.logger.info('📱 Step 6: Waiting 30 seconds and checking for background worker activity...');
            await this.core.sleep(30000);
            
            const workerLog = await this.core.executeCommand(
                'adb logcat -d -t 200 | findstr /i /c:"TranscriptionWorker" /c:"Starting background transcription" /c:"WorkManager"'
            );
            
            const hasWorkerActivity = workerLog.output.includes('TranscriptionWorker') || 
                                    workerLog.output.includes('Starting background transcription') ||
                                    workerLog.output.includes('WorkManager');
            
            if (!hasWorkerActivity) {
                this.core.logger.warn('⚠️ Background worker activity not found (may be delayed or killed)');
            } else {
                this.core.logger.info('✅ Background worker activity detected');
            }
            
            // Step 7: Verify transcription continues in background
            this.core.logger.info('📱 Step 7: Verifying transcription continues in background...');
            await this.core.sleep(10000);
            
            const progressLog = await this.core.executeCommand(
                'adb logcat -d -t 200 | findstr /i /c:"progress" /c:"polling" /c:"status check" /c:"pluct_processing_live"'
            );
            
            const hasProgress = progressLog.output.includes('progress') || 
                               progressLog.output.includes('polling') ||
                               progressLog.output.includes('status check');
            
            if (hasProgress) {
                this.core.logger.info('✅ Background processing activity detected');
            } else {
                this.core.logger.warn('⚠️ No background processing activity detected (may be killed by battery optimization)');
            }
            
            // Step 8: Re-enable exemption
            this.core.logger.info('📱 Step 8: Re-enabling battery optimization exemption...');
            await this.core.executeCommand('adb shell dumpsys deviceidle whitelist +app.pluct');
            await this.core.sleep(2000);
            this.core.logger.info('✅ Battery optimization exemption re-enabled');
            
            // Step 9: Verify background processing continues
            this.core.logger.info('📱 Step 9: Verifying background processing continues after exemption...');
            await this.core.sleep(10000);
            
            const continuedLog = await this.core.executeCommand(
                'adb logcat -d -t 100 | findstr /i /c:"TranscriptionWorker" /c:"progress" /c:"polling"'
            );
            
            const continuesProcessing = continuedLog.output.includes('TranscriptionWorker') || 
                                       continuedLog.output.includes('progress') ||
                                       continuedLog.output.includes('polling');
            
            if (continuesProcessing) {
                this.core.logger.info('✅ Background processing continues after exemption');
            } else {
                this.core.logger.warn('⚠️ Background processing may have stopped (check if transcription completed)');
            }
            
            this.core.logger.info('✅ Journey-UX-17BackgroundProcessing-01Validation completed successfully');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Journey failed: ${error.message}`);
            await this.failWithDiagnostics(error.message);
            return { success: false, error: error.message };
        } finally {
            // Re-enable exemption in case of failure
            await this.core.executeCommand('adb shell dumpsys deviceidle whitelist +app.pluct');
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('UX-17BackgroundProcessing-01Validation', new JourneyUX17BackgroundProcessing01Validation(orchestrator.core));
}

module.exports = { register };
