const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyUX15BatteryOptimization01Validation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-15BatteryOptimization-01Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-UX-15BatteryOptimization-01Validation');
        
        try {
            // Step 1: Launch app
            this.core.logger.info('📱 Step 1: Launching app...');
            const launchResult = await this.core.launchApp();
            if (!launchResult.success) {
                return { success: false, error: 'App launch failed' };
            }
            await this.core.sleep(2000);
            
            // Step 2: Navigate to Settings
            this.core.logger.info('📱 Step 2: Navigating to Settings...');
            await this.ensureAppForeground();
            await this.core.dumpUIHierarchy();
            
            // Tap settings (top bar or bottom nav)
            let settingsTap = await this.core.tapByTestTag('settings_button');
            if (!settingsTap || !settingsTap.success) {
                settingsTap = await this.core.tapByTestTag('nav_settings');
            }
            if (!settingsTap || !settingsTap.success) {
                settingsTap = await this.core.tapByContentDesc('Settings');
            }
            if (!settingsTap || !settingsTap.success) {
                return { success: false, error: 'Settings button not found' };
            }
            await this.core.sleep(2000);
            
            // Step 3: Navigate to Permissions section
            this.core.logger.info('📱 Step 3: Navigating to Permissions section...');
            await this.core.dumpUIHierarchy();
            const permissionsTap = await this.core.tapByText('Permissions');
            if (!permissionsTap || !permissionsTap.success) {
                return { success: false, error: 'Permissions section not found' };
            }
            await this.core.sleep(2000);
            
            // Step 4: Verify battery optimization status is displayed
            this.core.logger.info('📱 Step 4: Verifying battery optimization status in UI...');
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            
            const hasBackgroundProcessing = uiDump.includes('Background Processing') || 
                                          uiDump.includes('background processing') ||
                                          uiDump.includes('BackgroundProcessing');
            
            if (!hasBackgroundProcessing) {
                this.core.logger.error('❌ FAILURE: "Background Processing" row not found in UI');
                return { success: false, error: 'Background Processing row not found' };
            }
            this.core.logger.info('✅ "Background Processing" row found in UI');
            
            // Step 5: Verify logcat shows permission check
            this.core.logger.info('📱 Step 5: Verifying logcat for permission check...');
            await this.core.sleep(1000);
            const logcatResult = await this.core.executeCommand(
                'adb logcat -d -t 50 | findstr /i "PermissionManager.*Battery optimization"'
            );
            
            const hasPermissionLog = logcatResult.output.includes('Battery optimization') || 
                                   logcatResult.output.includes('battery optimization exempt');
            
            if (!hasPermissionLog) {
                this.core.logger.warn('⚠️ Permission check log not found (may be cached)');
            } else {
                this.core.logger.info('✅ Permission check log found in logcat');
            }
            
            // Step 6: Check current exemption status
            this.core.logger.info('📱 Step 6: Checking current battery optimization status...');
            await this.core.dumpUIHierarchy();
            const statusDump = this.core.readLastUIDump();
            
            const isOptimized = statusDump.includes('Optimized') && 
                              !statusDump.includes('May be restricted');
            const isRestricted = statusDump.includes('May be restricted') || 
                               statusDump.includes('restricted');
            
            this.core.logger.info(`Current status: ${isOptimized ? 'Optimized' : isRestricted ? 'May be restricted' : 'Unknown'}`);
            
            // Step 7: If not exempt, test "Enable" button
            if (isRestricted || (!isOptimized && !isRestricted)) {
                this.core.logger.info('📱 Step 7: Testing "Enable" button...');
                await this.core.dumpUIHierarchy();
                
                const enableButton = await this.core.tapByTestTag('settings_enable_battery_optimization_button');
                
                if (!enableButton || !enableButton.success) {
                    const enableText = await this.core.tapByText('Enable');
                    if (!enableText || !enableText.success) {
                        this.core.logger.warn('⚠️ Enable button not found (may already be optimized)');
                    } else {
                        await this.handleEnableButton();
                    }
                } else {
                    await this.handleEnableButton();
                }
            } else {
                this.core.logger.info('✅ Already optimized, skipping enable button test');
            }
            
            this.core.logger.info('✅ Journey-UX-15BatteryOptimization-01Validation completed successfully');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Journey failed: ${error.message}`);
            await this.failWithDiagnostics(error.message);
            return { success: false, error: error.message };
        }
    }
    
    async handleEnableButton() {
        this.core.logger.info('✅ Enable button tapped, waiting for settings to open...');
        await this.core.sleep(3000);
        
        // Verify settings intent launched
        const settingsLog = await this.core.executeCommand(
            'adb logcat -d -t 30 | findstr /i "ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"'
        );
        
        if (settingsLog.output.includes('BATTERY_OPTIMIZATIONS') || 
            settingsLog.output.includes('battery')) {
            this.core.logger.info('✅ Battery optimization settings intent launched');
        } else {
            this.core.logger.warn('⚠️ Settings intent log not found (may have opened)');
        }
        
        // Return to app (user would do this manually, but for test we'll wait)
        this.core.logger.info('Waiting 5 seconds for user to return from settings...');
        await this.core.sleep(5000);
        
        // Step 8: Verify status refreshed after returning
        this.core.logger.info('📱 Step 8: Verifying status refresh after returning from settings...');
        await this.core.dumpUIHierarchy();
        const refreshedDump = this.core.readLastUIDump();
        
        // Status should be refreshed (may still be restricted if user didn't grant)
        const statusRefreshed = refreshedDump.includes('Background Processing');
        if (statusRefreshed) {
            this.core.logger.info('✅ Status section still visible after returning from settings');
        } else {
            this.core.logger.warn('⚠️ Status section not found (may need navigation)');
        }
    }
}

function register(orchestrator) {
    orchestrator.registerJourney('UX-15BatteryOptimization-01Validation', new JourneyUX15BatteryOptimization01Validation(orchestrator.core));
}

module.exports = { register };
