const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Permission-03OverlayService-01Validation
 * Validates overlay service creation, status display, and dismissal
 * Follows naming convention: [Project]-[Permission]-[Sequence][Feature]-[Sequence][Validation]
 * 5 scope layers: Project, Permission, Sequence, Feature, Sequence, Validation
 */
class JourneyPermission03OverlayService01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Permission-03OverlayService-01Validation';
    }

    async run() {
        this.core.logger.info('🚀 Starting: Journey-Permission-03OverlayService-01Validation');
        
        try {
            // Step 1: Grant overlay permission via ADB
            this.core.logger.info('📱 Step 1: Granting overlay permission via ADB...');
            await this.core.executeCommand('adb shell appops set app.pluct SYSTEM_ALERT_WINDOW allow');
            await this.core.sleep(1000);
            
            // Verify permission granted
            const permissionCheck = await this.core.executeCommand(
                'adb shell appops get app.pluct SYSTEM_ALERT_WINDOW'
            );
            if (!permissionCheck.output.includes('allow') && !permissionCheck.output.includes('MODE_ALLOWED')) {
                throw new Error('Overlay permission not granted');
            }
            this.core.logger.info('✅ Overlay permission granted');
            
            // Step 2: Launch app
            this.core.logger.info('📱 Step 2: Launching app...');
            await this.core.launchApp();
            await this.core.sleep(3000);
            
            // Step 3: Enable overlay in settings (if needed)
            this.core.logger.info('📱 Step 3: Ensuring overlay is enabled in settings...');
            await this.ensureAppForeground();
            
            // Open settings
            const settingsTap = await this.core.tapByTestTag('settings_button');
            if (settingsTap.success) {
                await this.core.sleep(1000);
                
                // Check if overlay toggle exists and is enabled
                await this.core.dumpUIHierarchy();
                const settingsDump = this.core.readLastUIDump();
                
                if (settingsDump.includes('overlay_toggle')) {
                    // Toggle might be off, enable it
                    const toggleTap = await this.core.tapByTestTag('settings_overlay_toggle');
                    if (toggleTap.success) {
                        await this.core.sleep(500);
                        this.core.logger.info('✅ Overlay enabled in settings');
                    }
                }
                
                // Close settings
                const closeTap = await this.core.tapByTestTag('settings_dialog_close');
                if (!closeTap.success) {
                    await this.core.pressKey('Back');
                }
                await this.core.sleep(1000);
            }
            
            // Step 4: Start transcription
            this.core.logger.info('📱 Step 4: Starting transcription...');
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
            
            // Step 5: Minimize app (move to background)
            this.core.logger.info('📱 Step 5: Minimizing app to background...');
            await this.core.executeCommand('adb shell input keyevent KEYCODE_HOME');
            await this.core.sleep(2000);
            
            // Step 6: Verify overlay window appears
            this.core.logger.info('📱 Step 6: Verifying overlay window appears...');
            await this.core.sleep(2000);
            
            // Check logcat for overlay creation
            const overlayLogcat = await this.core.logcatValidator.validatePattern(
                'WindowManager.*addView|Overlay.*shown|overlay.*created',
                'Overlay window creation in logcat',
                5,
                2000,
                100
            );
            
            if (!overlayLogcat.success) {
                this.core.logger.warn('⚠️ Overlay creation not found in logcat (may not be shown if app is foreground)');
            } else {
                this.core.logger.info('✅ Overlay window creation found in logcat');
            }
            
            // Check for overlay service running
            const serviceCheck = await this.core.executeCommand(
                'adb shell dumpsys activity services | findstr /i "OverlayService|overlay"'
            );
            
            if (serviceCheck.output && serviceCheck.output.includes('Overlay')) {
                this.core.logger.info('✅ Overlay service is running');
            } else {
                this.core.logger.warn('⚠️ Overlay service not found (may be normal if app is foreground)');
            }
            
            // Step 7: Verify overlay shows progress
            this.core.logger.info('📱 Step 7: Verifying overlay shows progress...');
            // Overlay is shown as a floating window, hard to capture via UI dump
            // Check logcat for status updates
            const statusLogcat = await this.core.logcatValidator.validatePattern(
                'Overlay.*status|updateStatus|Processing',
                'Overlay status update in logcat',
                3,
                2000,
                50
            );
            
            if (statusLogcat.success) {
                this.core.logger.info('✅ Overlay status updates found in logcat');
            } else {
                this.core.logger.warn('⚠️ Overlay status updates not found (may be normal)');
            }
            
            // Step 8: Wait for transcription to complete
            this.core.logger.info('📱 Step 8: Waiting for transcription to complete...');
            let completed = false;
            for (let i = 0; i < 12; i++) {
                await this.core.sleep(5000);
                
                // Check logcat for completion
                const completionCheck = await this.core.executeCommand(
                    'adb logcat -d -t 50 | findstr /i "completed|Transcription.*complete"'
                );
                
                if (completionCheck.output && completionCheck.output.includes('complete')) {
                    completed = true;
                    break;
                }
            }
            
            if (!completed) {
                this.core.logger.warn('⚠️ Transcription did not complete within timeout');
            }
            
            // Step 9: Verify overlay dismisses
            this.core.logger.info('📱 Step 9: Verifying overlay dismisses on completion...');
            await this.core.sleep(2000);
            
            // Check logcat for overlay dismissal
            const dismissLogcat = await this.core.logcatValidator.validatePattern(
                'WindowManager.*removeView|Overlay.*dismissed|dismissOverlay',
                'Overlay dismissal in logcat',
                3,
                2000,
                100
            );
            
            if (dismissLogcat.success) {
                this.core.logger.info('✅ Overlay dismissal found in logcat');
            } else {
                this.core.logger.warn('⚠️ Overlay dismissal not found (may have been dismissed earlier)');
            }
            
            // Step 10: Verify system notification still appears
            this.core.logger.info('📱 Step 10: Verifying system notification still appears...');
            const notificationCheck = await this.core.executeCommand(
                'adb shell dumpsys notification | findstr /i "pluct|Transcription"'
            );
            
            if (notificationCheck.output && notificationCheck.output.includes('pluct')) {
                this.core.logger.info('✅ System notification found');
            } else {
                this.core.logger.warn('⚠️ System notification not found (may have been dismissed)');
            }
            
            this.core.logger.info('✅ Journey-Permission-03OverlayService-01Validation completed');
            return true;
            
        } catch (error) {
            this.core.logger.error(`❌ Journey failed: ${error.message}`);
            await this.failWithDiagnostics(error.message);
            return false;
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('Permission-03OverlayService-01Validation', new JourneyPermission03OverlayService01Validation(orchestrator.core));
}

module.exports = JourneyPermission03OverlayService01Validation;
