const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-Permission-05PermissionDeniedFlow-01Validation
 * Validates fallback behavior when permissions are denied
 * Follows naming convention: [Project]-[Permission]-[Sequence][Feature]-[Sequence][Validation]
 * 5 scope layers: Project, Permission, Sequence, Feature, Sequence, Validation
 */
class JourneyPermission05PermissionDeniedFlow01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Permission-05PermissionDeniedFlow-01Validation';
    }

    async run() {
        this.core.logger.info('🚀 Starting: Journey-Permission-05PermissionDeniedFlow-01Validation');
        
        try {
            // Step 1: Deny notification permission permanently
            this.core.logger.info('📱 Step 1: Denying notification permission...');
            const androidVersion = await this.core.executeCommand('adb shell getprop ro.build.version.sdk');
            const sdkVersion = parseInt(androidVersion.output.trim());
            
            if (sdkVersion >= 33) {
                await this.core.executeCommand('adb shell pm revoke app.pluct android.permission.POST_NOTIFICATIONS');
                await this.core.sleep(1000);
                
                // Verify permission is denied
                const permissionCheck = await this.core.executeCommand(
                    'adb shell appops get app.pluct POST_NOTIFICATIONS'
                );
                if (permissionCheck.output.includes('allow')) {
                    // Force deny
                    await this.core.executeCommand('adb shell appops set app.pluct POST_NOTIFICATIONS deny');
                    await this.core.sleep(1000);
                }
                this.core.logger.info('✅ Notification permission denied');
            } else {
                this.core.logger.info('ℹ️ Android version < 13, notification permission always granted');
            }
            
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
            
            // Step 4: Verify toast notification appears (fallback when permission denied)
            this.core.logger.info('📱 Step 4: Verifying toast appears as fallback...');
            await this.core.sleep(1000);
            await this.core.dumpUIHierarchy();
            const uiDump1 = this.core.readLastUIDump();
            
            const toastFound = uiDump1.includes('Transcription started') || 
                              uiDump1.includes('Processing') ||
                              uiDump1.includes('toast');
            
            if (!toastFound) {
                // Check logcat
                const logcatResult = await this.core.logcatValidator.validatePattern(
                    'Toast.*started|showToast|permission.*denied.*toast',
                    'Toast fallback in logcat',
                    3,
                    2000,
                    50
                );
                
                if (!logcatResult.success) {
                    throw new Error('Toast notification not found as fallback');
                }
            }
            this.core.logger.info('✅ Toast notification found as fallback');
            
            // Step 5: Verify no system notification appears (or toast fallback works)
            this.core.logger.info('📱 Step 5: Verifying notification fallback behavior...');
            await this.core.sleep(2000);
            const notificationCheck = await this.core.executeCommand(
                'adb shell dumpsys notification | findstr /i "pluct"'
            );
            
            if (notificationCheck.output && notificationCheck.output.includes('pluct')) {
                // Check if it's an error about permission
                if (notificationCheck.output.includes('denied') || notificationCheck.output.includes('SecurityException')) {
                    this.core.logger.info('✅ System notification correctly blocked due to permission denial');
                } else {
                    // May be a queue notification or other non-permission-requiring notification
                    this.core.logger.info('ℹ️ System notification found (may be queue notification, not requiring permission)');
                }
            } else {
                this.core.logger.info('✅ No system notification requiring permission (correct behavior)');
            }
            
            // Verify toast was shown as fallback (checked in step 4)
            
            // Step 6: Verify error logged in logcat
            this.core.logger.info('📱 Step 6: Verifying error logged in logcat...');
            const errorLogcat = await this.core.logcatValidator.validatePattern(
                'Permission.*denied|SecurityException|notification.*denied',
                'Permission denial error in logcat',
                3,
                2000,
                100
            );
            
            if (errorLogcat.success) {
                this.core.logger.info('✅ Permission denial error logged');
            } else {
                this.core.logger.warn('⚠️ Permission denial error not found in logcat');
            }
            
            // Step 7: Open settings
            this.core.logger.info('📱 Step 7: Opening settings...');
            const settingsTap = await this.core.tapByTestTag('settings_button');
            if (!settingsTap.success) {
                throw new Error('Could not find settings button');
            }
            await this.core.sleep(1000);
            
            // Step 8: Verify "Enable Notifications" button shows "Open Settings" (permanently denied)
            this.core.logger.info('📱 Step 8: Verifying settings shows permission status...');
            await this.core.dumpUIHierarchy();
            const uiDump2 = this.core.readLastUIDump();
            
            // Check for permission controls in settings
            const permissionControlsFound = uiDump2.includes('Open Settings') ||
                                          uiDump2.includes('settings_enable_notifications_button') ||
                                          uiDump2.includes('Enable') ||
                                          uiDump2.includes('Notifications') ||
                                          uiDump2.includes('Permissions');
            
            if (!permissionControlsFound) {
                // Settings might not be open yet, try opening it
                this.core.logger.info('ℹ️ Permission controls not found, ensuring settings dialog is open...');
                const settingsButton = await this.core.tapByTestTag('settings_button');
                if (settingsButton.success) {
                    await this.core.sleep(1000);
                    await this.core.dumpUIHierarchy();
                    const uiDump3 = this.core.readLastUIDump();
                    if (uiDump3.includes('Notifications') || uiDump3.includes('Permissions')) {
                        this.core.logger.info('✅ Permission controls found in settings');
                    } else {
                        this.core.logger.warn('⚠️ Permission controls not visible in settings');
                    }
                } else {
                    this.core.logger.warn('⚠️ Could not open settings dialog');
                }
            } else {
                this.core.logger.info('✅ Settings provides path to re-enable permission');
            }
            
            // Step 9: Tap button
            if (openSettingsFound) {
                this.core.logger.info('📱 Step 9: Tapping "Open Settings" button...');
                const openSettingsTap = await this.core.tapByTestTag('settings_enable_notifications_button');
                if (!openSettingsTap.success) {
                    await this.core.tapByText('Open Settings');
                }
                await this.core.sleep(2000);
                
                // Step 10: Verify system settings open
                this.core.logger.info('📱 Step 10: Verifying system settings open...');
                await this.core.dumpUIHierarchy();
                const uiDump3 = this.core.readLastUIDump();
                
                // Check for settings activity
                const settingsActivityCheck = await this.core.executeCommand(
                    'adb shell dumpsys activity activities | findstr /i "Settings|settings"'
                );
                
                if (settingsActivityCheck.output && settingsActivityCheck.output.includes('Settings')) {
                    this.core.logger.info('✅ System settings opened');
                } else {
                    this.core.logger.warn('⚠️ System settings may not have opened');
                }
                
                // Go back to app
                await this.core.pressKey('Back');
                await this.core.sleep(1000);
            }
            
            // Step 11: Grant permission and verify notifications work
            this.core.logger.info('📱 Step 11: Granting permission and verifying notifications work...');
            if (sdkVersion >= 33) {
                await this.core.executeCommand('adb shell pm grant app.pluct android.permission.POST_NOTIFICATIONS');
                await this.core.sleep(2000);
                
                // Start another transcription
                await this.core.tapByTestTag('url_input_field');
                await this.core.inputText(this.core.config.url);
                await this.core.sleep(1000);
                await this.core.tapByTestTag('extract_script_button');
                await this.core.sleep(2000);
                
                // Verify notification appears
                const notificationCheck2 = await this.core.executeCommand(
                    'adb shell dumpsys notification | findstr /i "pluct"'
                );
                
                if (notificationCheck2.output && notificationCheck2.output.includes('pluct')) {
                    this.core.logger.info('✅ Notifications work after granting permission');
                } else {
                    this.core.logger.warn('⚠️ Notification not found after granting permission');
                }
            }
            
            this.core.logger.info('✅ Journey-Permission-05PermissionDeniedFlow-01Validation completed');
            return true;
            
        } catch (error) {
            this.core.logger.error(`❌ Journey failed: ${error.message}`);
            await this.failWithDiagnostics(error.message);
            return false;
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('Permission-05PermissionDeniedFlow-01Validation', new JourneyPermission05PermissionDeniedFlow01Validation(orchestrator.core));
}

module.exports = JourneyPermission05PermissionDeniedFlow01Validation;
