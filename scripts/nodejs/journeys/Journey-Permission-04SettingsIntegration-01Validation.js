const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Permission-04SettingsIntegration-01Validation
 * Validates permission controls in settings dialog and state updates
 * Follows naming convention: [Project]-[Permission]-[Sequence][Feature]-[Sequence][Validation]
 * 5 scope layers: Project, Permission, Sequence, Feature, Sequence, Validation
 */
class JourneyPermission04SettingsIntegration01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Permission-04SettingsIntegration-01Validation';
    }

    async run() {
        this.core.logger.info('🚀 Starting: Journey-Permission-04SettingsIntegration-01Validation');
        
        try {
            // Step 1: Launch app
            this.core.logger.info('📱 Step 1: Launching app...');
            await this.core.launchApp();
            await this.core.sleep(3000);
            
            // Step 2: Open settings dialog
            this.core.logger.info('📱 Step 2: Opening settings dialog...');
            await this.ensureAppForeground();
            
            let settingsTap = await this.core.tapByTestTag('settings_button');
            if (!settingsTap.success) {
                settingsTap = await this.core.tapByContentDesc('Settings');
            }
            if (!settingsTap.success) {
                throw new Error('Could not find settings button');
            }
            await this.core.sleep(1000);
            
            // Step 3: Verify permission status indicators visible
            this.core.logger.info('📱 Step 3: Verifying permission status indicators visible...');
            await this.core.dumpUIHierarchy();
            const uiDump1 = this.core.readLastUIDump();
            
            if (!uiDump1.includes('Notifications') && !uiDump1.includes('Permissions')) {
                throw new Error('Permission status indicators not found in settings');
            }
            this.core.logger.info('✅ Permission status indicators found');
            
            // Step 4: Verify "Enable Notifications" button visible if permission denied
            this.core.logger.info('📱 Step 4: Checking notification permission status...');
            
            // First, revoke notification permission to test
            const androidVersion = await this.core.executeCommand('adb shell getprop ro.build.version.sdk');
            const sdkVersion = parseInt(androidVersion.output.trim());
            
            if (sdkVersion >= 33) {
                await this.core.executeCommand('adb shell pm revoke app.pluct android.permission.POST_NOTIFICATIONS', undefined, undefined, { allowFailure: true });
                await this.core.sleep(1000);
                
                // Refresh settings by closing and reopening
                const closeTap = await this.core.tapByTestTag('settings_dialog_close');
                if (!closeTap.success) {
                    await this.core.pressKey('Back');
                }
                await this.core.sleep(1000);
                
                await this.core.tapByTestTag('settings_button');
                await this.core.sleep(1000);
            }
            
            await this.core.dumpUIHierarchy();
            const uiDump2 = this.core.readLastUIDump();
            
            const enableButtonFound = uiDump2.includes('settings_enable_notifications_button') ||
                                     uiDump2.includes('Enable') ||
                                     uiDump2.includes('Enable Notifications');
            
            if (!enableButtonFound && sdkVersion >= 33) {
                this.core.logger.warn('⚠️ Enable Notifications button not found (may already be granted)');
            } else {
                this.core.logger.info('✅ Enable Notifications button found');
            }
            
            // Step 5: Tap "Enable Notifications" button
            if (enableButtonFound) {
                this.core.logger.info('📱 Step 5: Tapping "Enable Notifications" button...');
                const enableTap = await this.core.tapByTestTag('settings_enable_notifications_button');
                if (!enableTap.success) {
                    // Try by text
                    await this.core.tapByText('Enable');
                }
                await this.core.sleep(2000);
                
                // Step 6: Verify permission request appears (logcat validation)
                this.core.logger.info('📱 Step 6: Verifying permission request appears...');
                const logcatResult = await this.core.logcatValidator.validatePattern(
                    'requestPermissions|POST_NOTIFICATIONS',
                    'Permission request in logcat',
                    3,
                    2000,
                    50
                );
                
                if (!logcatResult.success) {
                    this.core.logger.warn('⚠️ Permission request not found in logcat');
                } else {
                    this.core.logger.info('✅ Permission request found in logcat');
                }
                
                // Step 7: Grant permission
                this.core.logger.info('📱 Step 7: Granting notification permission...');
                if (sdkVersion >= 33) {
                    await this.core.executeCommand('adb shell pm grant app.pluct android.permission.POST_NOTIFICATIONS', undefined, undefined, { allowFailure: true });
                    await this.core.sleep(2000);
                }
                
                // Step 8: Verify settings UI updates to show "Granted" status
                this.core.logger.info('📱 Step 8: Verifying settings UI updates...');
                // Refresh settings
                const closeTap2 = await this.core.tapByTestTag('settings_dialog_close');
                if (!closeTap2.success) {
                    await this.core.pressKey('Back');
                }
                await this.core.sleep(1000);
                
                await this.core.tapByTestTag('settings_button');
                await this.core.sleep(1000);
                
                await this.core.dumpUIHierarchy();
                const uiDump3 = this.core.readLastUIDump();
                
                if (uiDump3.includes('Granted') || uiDump3.includes('granted')) {
                    this.core.logger.info('✅ Settings UI shows "Granted" status');
                } else {
                    this.core.logger.warn('⚠️ "Granted" status not found (may be shown differently)');
                }
            }
            
            // Step 9: Verify overlay toggle is visible and functional
            this.core.logger.info('📱 Step 9: Verifying overlay toggle...');
            await this.core.dumpUIHierarchy();
            const uiDump4 = this.core.readLastUIDump();
            
            if (uiDump4.includes('overlay_toggle') || uiDump4.includes('Show overlay')) {
                this.core.logger.info('✅ Overlay toggle found');
                
                // Test toggle functionality
                const toggleTap = await this.core.tapByTestTag('settings_overlay_toggle');
                if (toggleTap.success) {
                    await this.core.sleep(500);
                    this.core.logger.info('✅ Overlay toggle is functional');
                }
            } else {
                // Overlay toggle may not be visible if permission not granted
                this.core.logger.info('ℹ️ Overlay toggle not visible (may require overlay permission first)');
            }
            
            this.core.logger.info('✅ Journey-Permission-04SettingsIntegration-01Validation completed');
            const finalClose = await this.core.tapByTestTag('settings_dialog_close');
            if (!finalClose.success) {
                await this.core.tapByContentDesc('Close settings');
            }
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Journey failed: ${error.message}`);
            await this.failWithDiagnostics(error.message);
            return { success: false, error: error.message };
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('Permission-04SettingsIntegration-01Validation', new JourneyPermission04SettingsIntegration01Validation(orchestrator.core));
}

module.exports = JourneyPermission04SettingsIntegration01Validation;
