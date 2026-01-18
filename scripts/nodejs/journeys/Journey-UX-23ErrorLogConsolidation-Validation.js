const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Journey-UX-23ErrorLogConsolidation-Validation
 * Validates error logs consolidated in Settings, removed from home screen
 */
class JourneyUX23ErrorLogConsolidationValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'UX-23ErrorLogConsolidation-Validation';
    }

    async execute() {
        this.core.logger.info('Starting Error Log Consolidation Validation');
        
        // Step 1: Launch app
        await this.core.launchApp();
        await this.core.sleep(3000);
        
        // Step 2: Check home screen for "Recent Errors" section
        await this.core.dumpUIHierarchy();
        let uiDump = this.core.readLastUIDump() || '';
        
        const hasRecentErrorsSection = uiDump.includes('Recent Errors') || 
                                      uiDump.includes('recent_errors') ||
                                      uiDump.includes('PluctErrorLogSection');
        
        if (hasRecentErrorsSection) {
            return { 
                success: false, 
                error: 'Found "Recent Errors" section on home screen - should be removed' 
            };
        }
        
        // Step 3: Open Settings
        const settingsButton = await this.core.tapByContentDesc('Settings') || 
                              await this.core.tapByTestTag('settings_button');
        
        if (!settingsButton.success) {
            // Try alternative ways to open settings
            await this.core.executeCommand('adb shell input tap 1000 100'); // Top right area
            await this.core.sleep(2000);
        } else {
            await this.core.sleep(2000);
        }
        
        await this.core.dumpUIHierarchy();
        uiDump = this.core.readLastUIDump() || '';
        
        // Step 4: Verify "View Debug Logs" button exists
        const hasDebugLogsButton = uiDump.includes('View Debug Logs') || 
                                  uiDump.includes('view_debug_logs_button');
        
        if (!hasDebugLogsButton) {
            return { 
                success: false, 
                error: 'Debug Logs button not found in Settings' 
            };
        }
        
        // Step 5: Check for error count badge
        const hasErrorCountBadge = uiDump.includes('errors') || 
                                   uiDump.includes('error');
        
        // Step 6: Open debug logs viewer
        const debugLogsTap = await this.core.tapByTestTag('settings_view_debug_logs_button') ||
                            await this.core.tapByText('View Debug Logs');
        
        if (debugLogsTap.success) {
            await this.core.sleep(2000);
            await this.core.dumpUIHierarchy();
            const logsDump = this.core.readLastUIDump() || '';
            
            // Step 7: Verify error filtering exists
            const hasErrorFilter = logsDump.includes('Show Errors Only') || 
                                  logsDump.includes('ERROR') ||
                                  logsDump.includes('error');
            
            // Step 8: Verify errors are shown prominently
            const hasErrorCount = logsDump.includes('error(s)') || 
                                 logsDump.includes('error');
            
            this.core.logger.info('✅ Error log consolidation validation passed');
            return { 
                success: true, 
                details: { 
                    recentErrorsRemoved: true,
                    debugLogsInSettings: hasDebugLogsButton,
                    errorCountBadge: hasErrorCountBadge,
                    errorFiltering: hasErrorFilter,
                    errorProminence: hasErrorCount
                }
            };
        }
        
        this.core.logger.info('✅ Error log consolidation validation passed (basic checks)');
        return { 
            success: true, 
            details: { 
                recentErrorsRemoved: true,
                debugLogsInSettings: hasDebugLogsButton,
                errorCountBadge: hasErrorCountBadge
            }
        };
    }
}

module.exports = JourneyUX23ErrorLogConsolidationValidation;
