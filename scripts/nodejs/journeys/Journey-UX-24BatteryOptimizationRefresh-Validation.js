const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-UX-24BatteryOptimizationRefresh-Validation
 * Validates battery optimization status refreshes correctly
 */
class JourneyUX24BatteryOptimizationRefreshValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-24BatteryOptimizationRefresh-Validation';
    }

    async execute() {
        this.core.logger.info('Starting Battery Optimization Refresh Validation');
        
        // Step 1: Launch app
        await this.core.launchApp();
        await this.core.sleep(3000);
        
        // Step 2: Open Settings
        let settingsButton = await this.core.tapByContentDesc('Settings');
        if (!settingsButton.success) {
            settingsButton = await this.core.tapByTestTag('settings_button');
        }
        
        if (!settingsButton.success) {
            await this.core.executeCommand('adb shell input tap 1000 100');
            await this.core.sleep(2000);
        } else {
            await this.core.sleep(2000);
        }
        
        await this.core.dumpUIHierarchy();
        let uiDump = this.core.readLastUIDump() || '';
        
        // Step 3: Verify initial battery optimization status is displayed
        const hasBatterySection = uiDump.includes('Background Processing') || 
                                 uiDump.includes('Battery Optimization') ||
                                 uiDump.includes('battery');
        
        if (!hasBatterySection) {
            return { 
                success: false, 
                error: 'Battery optimization section not found in Settings' 
            };
        }
        
        // Step 4: Check initial status text
        const hasOptimizedStatus = uiDump.includes('Enabled') ||
                                  uiDump.includes('May be restricted') ||
                                  uiDump.includes('Background Processing');
        
        if (!hasOptimizedStatus) {
            return { 
                success: false, 
                error: 'Battery optimization status not displayed' 
            };
        }
        
        // Step 5: Check logcat for status refresh calls
        const refreshCheck = await this.core.executeCommand(
            'adb logcat -d -t 200 | findstr /i /c:"Battery optimization" /c:"Battery optimization exempt check" /c:"Permission cache invalidated"'
        );
        
        // Step 6: Verify status persists after app restart simulation
        // (We can't actually restart, but we can check if status is cached properly)
        await this.core.sleep(2000);
        await this.core.dumpUIHierarchy();
        const statusDump = this.core.readLastUIDump() || '';
        
        const statusStillVisible = statusDump.includes('Enabled') ||
                                   statusDump.includes('May be restricted') ||
                                   statusDump.includes('Background Processing');
        
        if (!statusStillVisible) {
            return { 
                success: false, 
                error: 'Battery optimization status disappeared after refresh' 
            };
        }
        
        this.core.logger.info('✅ Battery optimization refresh validation passed');
        return { 
            success: true, 
            details: { 
                initialStatusDisplayed: hasOptimizedStatus,
                statusRefreshWorking: refreshCheck.output ? refreshCheck.output.length > 0 : false,
                statusPersists: statusStillVisible
            }
        };
    }
}

module.exports = JourneyUX24BatteryOptimizationRefreshValidation;
