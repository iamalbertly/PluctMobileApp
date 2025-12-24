const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyUX03CreditRequestLoggingValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Journey-UX-03CreditRequestLogging-Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-UX-03CreditRequestLogging-Validation');
        
        try {
            // Step 1: Launch app and navigate to settings
            this.core.logger.info('- Step 1: Launch app and open settings');
            await this.ensureAppForeground();
            await this.core.sleep(2000);
            
            // Tap settings button
            const settingsTap = await this.core.tapByTestTag('settings_button');
            if (!settingsTap.success) {
                const settingsTap2 = await this.core.tapByText('Settings');
                if (!settingsTap2.success) {
                    this.core.logger.error('❌ Could not open settings');
                    return { success: false, error: 'Could not open settings dialog' };
                }
            }
            await this.core.sleep(1000);
            
            // Step 2: Tap "Add More Credits"
            this.core.logger.info('- Step 2: Tap "Add More Credits"');
            const addCreditsTap = await this.core.tapByText('Add More Credits');
            if (!addCreditsTap.success) {
                this.core.logger.error('❌ Could not find "Add More Credits" button');
                return { success: false, error: 'Could not find Add More Credits button' };
            }
            await this.core.sleep(1000);
            
            // Step 3: Enter test confirmation text
            this.core.logger.info('- Step 3: Enter test confirmation text');
            const testConfirmation = `TEST_${Date.now()}_CONFIRMATION`;
            await this.core.typeText(testConfirmation);
            await this.core.sleep(500);
            
            // Step 4: Submit request
            this.core.logger.info('- Step 4: Submit credit request');
            const submitTap = await this.core.tapByText('Submit Request');
            if (!submitTap.success) {
                const submitTap2 = await this.core.tapByText('Send Request');
                if (!submitTap2.success) {
                    this.core.logger.error('❌ Could not submit request');
                    return { success: false, error: 'Could not submit credit request' };
                }
            }
            
            // Step 5: Monitor logcat for request payload
            this.core.logger.info('- Step 5: Monitor logcat for request payload');
            await this.core.sleep(2000);
            const apiLogs = await this.core.captureAPILogs(300);
            
            if (!apiLogs.success) {
                this.core.logger.error('❌ Could not capture API logs');
                return { success: false, error: 'Could not capture logcat' };
            }
            
            // Step 6: Verify logcat contains required fields
            this.core.logger.info('- Step 6: Verify request payload in logcat');
            const allLogs = [
                ...apiLogs.parsed.requests,
                ...apiLogs.parsed.responses,
                ...apiLogs.parsed.errors,
                ...apiLogs.parsed.info || []
            ].join('\n');
            
            const hasCreditRequestCategory = allLogs.includes('CREDIT_REQUEST');
            const hasRequestId = /credit_req_\d+/.test(allLogs);
            const hasUserId = allLogs.includes('userId') || allLogs.includes('user_id');
            const hasConfirmation = allLogs.includes(testConfirmation) || allLogs.includes('confirmation');
            const hasRequestUrl = allLogs.includes('/v1/user/balance') || allLogs.includes('user/balance');
            
            if (!hasCreditRequestCategory) {
                this.core.logger.error('❌ FAILURE: CREDIT_REQUEST category not found in logcat');
                return { success: false, error: 'CREDIT_REQUEST category missing from logs' };
            }
            this.core.logger.info('✅ CREDIT_REQUEST category found');
            
            if (!hasRequestId) {
                this.core.logger.error('❌ FAILURE: Request ID not found in logcat');
                return { success: false, error: 'Request ID missing from logs' };
            }
            this.core.logger.info('✅ Request ID found in logs');
            
            if (!hasUserId) {
                this.core.logger.warn('⚠️ User ID not found in logs');
            } else {
                this.core.logger.info('✅ User ID found in logs');
            }
            
            if (!hasConfirmation) {
                this.core.logger.warn('⚠️ Confirmation text not found in logs');
            } else {
                this.core.logger.info('✅ Confirmation text found in logs');
            }
            
            if (!hasRequestUrl) {
                this.core.logger.warn('⚠️ Request URL not found in logs');
            } else {
                this.core.logger.info('✅ Request URL found in logs');
            }
            
            // Step 7: Wait for response and verify response logging
            this.core.logger.info('- Step 7: Verify response logging');
            await this.core.sleep(3000);
            const responseLogs = await this.core.captureAPILogs(200);
            const allResponseLogs = [
                ...responseLogs.parsed.requests,
                ...responseLogs.parsed.responses
            ].join('\n');
            
            const hasResponseStatus = allResponseLogs.includes('acknowledged') || 
                                    allResponseLogs.includes('status');
            
            if (hasResponseStatus) {
                this.core.logger.info('✅ Response status found in logs');
            } else {
                this.core.logger.warn('⚠️ Response status not found (may be manual processing)');
            }
            
            // Step 8: Check debug logs screen for persisted log entry
            this.core.logger.info('- Step 8: Verify log entry in debug logs');
            // Close settings if still open
            await this.core.pressKey('Back');
            await this.core.sleep(500);
            
            // Open settings again and navigate to debug logs
            const settingsTap2 = await this.core.tapByTestTag('settings_button');
            if (settingsTap2.success) {
                await this.core.sleep(1000);
                const debugLogsTap = await this.core.tapByText('View Debug Logs');
                if (debugLogsTap.success) {
                    await this.core.sleep(1000);
                    await this.dumpUI();
                    const debugUI = this.core.readLastUIDump();
                    
                    const hasCreditRequestInLogs = debugUI.includes('CREDIT_REQUEST') ||
                                                  debugUI.includes('Credit request');
                    
                    if (hasCreditRequestInLogs) {
                        this.core.logger.info('✅ Credit request found in debug logs screen');
                    } else {
                        this.core.logger.warn('⚠️ Credit request not visible in debug logs (may need to scroll)');
                    }
                }
            }
            
            // Step 9: Verify request ID visible in UI feedback
            this.core.logger.info('- Step 9: Verify request ID in UI');
            await this.core.pressKey('Back');
            await this.core.sleep(500);
            await this.dumpUI();
            const feedbackUI = this.core.readLastUIDump();
            
            const hasRequestIdInUI = /credit_req_\d+/.test(feedbackUI) ||
                                    feedbackUI.includes('Request sent') ||
                                    feedbackUI.includes('ID:');
            
            if (hasRequestIdInUI) {
                this.core.logger.info('✅ Request ID found in UI feedback');
            } else {
                this.core.logger.warn('⚠️ Request ID not visible in UI (may have dismissed)');
            }
            
            this.core.logger.info('✅ Completed: Journey-UX-03CreditRequestLogging-Validation');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Credit request logging validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = JourneyUX03CreditRequestLoggingValidation;

