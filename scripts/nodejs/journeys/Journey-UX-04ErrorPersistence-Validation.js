const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyUX04ErrorPersistenceValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Journey-UX-04ErrorPersistence-Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-UX-04ErrorPersistence-Validation');
        
        try {
            // Step 1: Trigger an error (e.g., invalid URL submission)
            this.core.logger.info('- Step 1: Trigger error with invalid URL');
            await this.ensureAppForeground();
            await this.core.sleep(2000);
            
            // Enter invalid URL
            const invalidUrl = 'not-a-valid-url';
            await this.core.tapByTestTag('capture_component_label');
            await this.core.sleep(500);
            await this.core.typeText(invalidUrl);
            await this.core.sleep(1000);
            
            // Try to submit
            const submitTap = await this.core.tapByTestTag('extract_script_button');
            if (!submitTap.success) {
                const submitTap2 = await this.core.tapByText('Extract Script');
                if (!submitTap2.success) {
                    this.core.logger.error('❌ Could not find submit button');
                    return { success: false, error: 'Could not trigger error' };
                }
            }
            await this.core.sleep(2000);
            
            // Step 2: Verify error appears in UI
            this.core.logger.info('- Step 2: Verify error appears in UI');
            await this.dumpUI();
            const errorUI = this.core.readLastUIDump();
            
            const hasError = errorUI.includes('Error') ||
                           errorUI.includes('error') ||
                           errorUI.includes('Invalid') ||
                           errorUI.includes('invalid') ||
                           errorUI.includes('Something went wrong');
            
            if (!hasError) {
                this.core.logger.warn('⚠️ Error not immediately visible (may need different trigger)');
            } else {
                this.core.logger.info('✅ Error displayed in UI');
            }
            
            // Step 3: Dismiss error
            this.core.logger.info('- Step 3: Dismiss error');
            const dismissTap = await this.core.tapByText('Dismiss');
            if (!dismissTap.success) {
                const dismissTap2 = await this.core.tapByContentDesc('Dismiss error');
                if (!dismissTap2.success) {
                    // Try close/X button
                    await this.core.pressKey('Back');
                }
            }
            await this.core.sleep(1000);
            
            // Verify error is dismissed
            await this.dumpUI();
            const afterDismissUI = this.core.readLastUIDump();
            const errorStillVisible = afterDismissUI.includes('Error') && 
                                    (afterDismissUI.includes('error message') || 
                                     afterDismissUI.includes('Error message'));
            
            if (errorStillVisible) {
                this.core.logger.warn('⚠️ Error still visible after dismiss');
            } else {
                this.core.logger.info('✅ Error dismissed from UI');
            }
            
            // Step 4: Navigate to debug logs screen
            this.core.logger.info('- Step 4: Navigate to debug logs');
            const settingsTap = await this.core.tapByTestTag('settings_button');
            if (!settingsTap.success) {
                const settingsTap2 = await this.core.tapByText('Settings');
                if (!settingsTap2.success) {
                    this.core.logger.error('❌ Could not open settings');
                    return { success: false, error: 'Could not navigate to debug logs' };
                }
            }
            await this.core.sleep(1000);
            
            const debugLogsTap = await this.core.tapByText('View Debug Logs');
            if (!debugLogsTap.success) {
                this.core.logger.error('❌ Could not open debug logs');
                return { success: false, error: 'Could not open debug logs screen' };
            }
            await this.core.sleep(2000);
            
            // Step 5: Verify error entry exists with full details
            this.core.logger.info('- Step 5: Verify error persisted in logs');
            await this.dumpUI();
            const logsUI = this.core.readLastUIDump();
            
            const hasErrorInLogs = logsUI.includes('USER_FACING_ERROR') ||
                                  logsUI.includes('captureCardError') ||
                                  logsUI.includes('Error') && logsUI.includes('category');
            
            if (!hasErrorInLogs) {
                this.core.logger.error('❌ FAILURE: Error not found in debug logs');
                return { success: false, error: 'Error not persisted to debug logs' };
            }
            this.core.logger.info('✅ Error found in debug logs');
            
            // Verify log entry has details
            const hasDetails = logsUI.includes('message') ||
                             logsUI.includes('category') ||
                             logsUI.includes('timestamp') ||
                             logsUI.includes('requestUrl');
            
            if (hasDetails) {
                this.core.logger.info('✅ Error log entry contains details');
            } else {
                this.core.logger.warn('⚠️ Error log entry may be missing some details');
            }
            
            // Step 6: Verify "View in Logs" button works (if error is still visible)
            this.core.logger.info('- Step 6: Test "View in Logs" button');
            await this.core.pressKey('Back');
            await this.core.sleep(500);
            
            // Try to trigger another error to test the button
            await this.core.tapByTestTag('capture_component_label');
            await this.core.sleep(500);
            await this.core.typeText('another-invalid-url');
            await this.core.sleep(1000);
            
            const submitTap2 = await this.core.tapByText('Extract Script');
            if (submitTap2.success) {
                await this.core.sleep(2000);
                await this.dumpUI();
                const errorUI2 = this.core.readLastUIDump();
                
                const hasViewInLogsButton = errorUI2.includes('View in Logs') ||
                                          errorUI2.includes('View in logs');
                
                if (hasViewInLogsButton) {
                    this.core.logger.info('✅ "View in Logs" button found');
                    const viewLogsTap = await this.core.tapByText('View in Logs');
                    if (viewLogsTap.success) {
                        await this.core.sleep(1000);
                        await this.dumpUI();
                        const logsUI2 = this.core.readLastUIDump();
                        if (logsUI2.includes('Debug Logs') || logsUI2.includes('debug logs')) {
                            this.core.logger.info('✅ "View in Logs" button navigates correctly');
                        }
                    }
                } else {
                    this.core.logger.warn('⚠️ "View in Logs" button not found (may require debugLogManager)');
                }
            }
            
            this.core.logger.info('✅ Completed: Journey-UX-04ErrorPersistence-Validation');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Error persistence validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = JourneyUX04ErrorPersistenceValidation;

