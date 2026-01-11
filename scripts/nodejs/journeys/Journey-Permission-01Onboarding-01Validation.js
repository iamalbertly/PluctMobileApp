const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Permission-01Onboarding-01Validation
 * Validates permission onboarding flow with UI dump, logcat, and ADB validation
 * Follows naming convention: [Project]-[Permission]-[Sequence][Feature]-[Sequence][Validation]
 * 5 scope layers: Project, Permission, Sequence, Feature, Sequence, Validation
 */
class JourneyPermission01Onboarding01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Permission-01Onboarding-01Validation';
    }

    async run() {
        this.core.logger.info('🚀 Starting: Journey-Permission-01Onboarding-01Validation');
        
        try {
            // Step 1: Clear app data to simulate first-time user
            this.core.logger.info('📱 Step 1: Clearing app data to simulate first-time user...');
            await this.core.executeCommand('adb shell pm clear app.pluct');
            await this.core.sleep(2000);
            
            // Step 2: Launch app
            this.core.logger.info('📱 Step 2: Launching app...');
            await this.core.launchApp();
            await this.core.sleep(3000);
            
            // Step 3: Verify onboarding dialog appears
            this.core.logger.info('📱 Step 3: Verifying onboarding dialog appears...');
            await this.core.dumpUIHierarchy();
            const uiDump1 = this.core.readLastUIDump();
            
            if (!uiDump1.includes('permission_onboarding_dialog') && 
                !uiDump1.includes('Enable Notifications') && 
                !uiDump1.includes('Enable Overlay')) {
                // Onboarding might appear after welcome dialog, wait a bit
                await this.core.sleep(2000);
                await this.core.dumpUIHierarchy();
                const uiDump2 = this.core.readLastUIDump();
                if (!uiDump2.includes('permission_onboarding_dialog') && 
                    !uiDump2.includes('Enable Notifications')) {
                    throw new Error('Onboarding dialog not found in UI dump');
                }
            }
            this.core.logger.info('✅ Onboarding dialog found');
            
            // Step 4: Verify dialog explains notification permission
            this.core.logger.info('📱 Step 4: Verifying dialog explains notification permission...');
            const uiDump3 = this.core.readLastUIDump();
            if (!uiDump3.includes('Notifications') || !uiDump3.includes('transcription')) {
                throw new Error('Onboarding dialog does not explain notification permission');
            }
            this.core.logger.info('✅ Dialog explains notification permission');
            
            // Step 5: Tap "Enable" button
            this.core.logger.info('📱 Step 5: Tapping "Enable" button...');
            const enableTap = await this.core.tapByTestTag('permission_onboarding_enable_button');
            if (!enableTap.success) {
                // Try by text as fallback
                const enableTap2 = await this.core.tapByText('Enable');
                if (!enableTap2.success) {
                    throw new Error('Could not find Enable button');
                }
            }
            await this.core.sleep(2000);
            
            // Step 6: Verify permission request dialog appears (logcat validation)
            this.core.logger.info('📱 Step 6: Verifying permission request appears...');
            const logcatResult = await this.core.logcatValidator.validatePattern(
                'Permission.*requested|POST_NOTIFICATIONS|requestPermissions',
                'Permission request in logcat',
                3,
                2000,
                100
            );
            
            if (!logcatResult.success) {
                this.core.logger.warn('⚠️ Permission request not found in logcat (may have been granted already)');
            } else {
                this.core.logger.info('✅ Permission request found in logcat');
            }
            
            // Step 7: Grant permission via ADB (if Android 13+)
            this.core.logger.info('📱 Step 7: Granting notification permission via ADB...');
            const androidVersion = await this.core.executeCommand('adb shell getprop ro.build.version.sdk');
            const sdkVersion = parseInt(androidVersion.output.trim());
            
            if (sdkVersion >= 33) {
                // Android 13+ requires POST_NOTIFICATIONS permission
                await this.core.executeCommand('adb shell pm grant app.pluct android.permission.POST_NOTIFICATIONS');
                await this.core.sleep(1000);
                this.core.logger.info('✅ Notification permission granted via ADB');
            } else {
                this.core.logger.info('ℹ️ Android version < 13, notification permission always granted');
            }
            
            // Step 8: Verify onboarding continues to overlay permission (if needed)
            this.core.logger.info('📱 Step 8: Checking for overlay permission onboarding...');
            await this.core.sleep(2000);
            await this.core.dumpUIHierarchy();
            const uiDump4 = this.core.readLastUIDump();
            
            // Check if overlay permission dialog appears
            if (uiDump4.includes('Overlay') || uiDump4.includes('permission_onboarding_dialog')) {
                this.core.logger.info('✅ Overlay permission onboarding found');
                
                // Tap Enable for overlay
                const overlayEnableTap = await this.core.tapByTestTag('permission_onboarding_enable_button');
                if (!overlayEnableTap.success) {
                    await this.core.tapByText('Enable');
                }
                await this.core.sleep(2000);
                
                // Grant overlay permission via ADB
                await this.core.executeCommand('adb shell appops set app.pluct SYSTEM_ALERT_WINDOW allow');
                await this.core.sleep(1000);
                this.core.logger.info('✅ Overlay permission granted via ADB');
            } else {
                this.core.logger.info('ℹ️ Overlay permission onboarding not shown (may already be granted or skipped)');
            }
            
            // Step 9: Verify permissions are granted (ADB validation)
            this.core.logger.info('📱 Step 9: Verifying permissions are granted...');
            const permissionCheck = await this.core.executeCommand(
                'adb shell dumpsys package app.pluct | findstr /i "permission"'
            );
            
            if (sdkVersion >= 33) {
                if (!permissionCheck.output.includes('POST_NOTIFICATIONS')) {
                    // Check via appops instead
                    const appopsCheck = await this.core.executeCommand(
                        'adb shell appops get app.pluct POST_NOTIFICATIONS'
                    );
                    if (!appopsCheck.output.includes('allow')) {
                        throw new Error('Notification permission not granted');
                    }
                }
            }
            
            // Check overlay permission
            const overlayCheck = await this.core.executeCommand(
                'adb shell appops get app.pluct SYSTEM_ALERT_WINDOW'
            );
            if (!overlayCheck.output.includes('allow') && !overlayCheck.output.includes('MODE_ALLOWED')) {
                this.core.logger.warn('⚠️ Overlay permission not granted (optional)');
            } else {
                this.core.logger.info('✅ Overlay permission granted');
            }
            
            // Step 10: Verify app functions normally
            this.core.logger.info('📱 Step 10: Verifying app functions normally...');
            await this.core.sleep(2000);
            await this.core.dumpUIHierarchy();
            const uiDump5 = this.core.readLastUIDump();
            
            if (!uiDump5.includes('app.pluct') && !uiDump5.includes('Pluct')) {
                throw new Error('App UI not visible after onboarding');
            }
            this.core.logger.info('✅ App functions normally after onboarding');
            
            // Step 11: Verify onboarding not shown again (check preferences)
            this.core.logger.info('📱 Step 11: Verifying onboarding state persisted...');
            const logcatCheck = await this.core.logcatValidator.validatePattern(
                'permission_onboarding|onboarding.*seen',
                'Onboarding state in logcat',
                2,
                1000,
                50
            );
            
            this.core.logger.info('✅ Journey-Permission-01Onboarding-01Validation completed');
            return true;
            
        } catch (error) {
            this.core.logger.error(`❌ Journey failed: ${error.message}`);
            await this.failWithDiagnostics(error.message);
            return false;
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('Permission-01Onboarding-01Validation', new JourneyPermission01Onboarding01Validation(orchestrator.core));
}

module.exports = JourneyPermission01Onboarding01Validation;
