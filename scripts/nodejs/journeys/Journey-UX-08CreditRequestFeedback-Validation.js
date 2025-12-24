const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyUX08CreditRequestFeedbackValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Journey-UX-08CreditRequestFeedback-Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-UX-08CreditRequestFeedback-Validation');
        
        try {
            // Step 1: Navigate to settings and tap "Add More Credits"
            this.core.logger.info('- Step 1: Navigate to credit request');
            await this.ensureAppForeground();
            await this.core.sleep(2000);
            
            const settingsTap = await this.core.tapByTestTag('settings_button');
            if (!settingsTap.success) {
                const settingsTap2 = await this.core.tapByText('Settings');
                if (!settingsTap2.success) {
                    this.core.logger.error('❌ Could not open settings');
                    return { success: false, error: 'Could not open settings' };
                }
            }
            await this.core.sleep(1000);
            
            const addCreditsTap = await this.core.tapByText('Add More Credits');
            if (!addCreditsTap.success) {
                this.core.logger.error('❌ Could not find "Add More Credits" button');
                return { success: false, error: 'Could not find Add More Credits button' };
            }
            await this.core.sleep(1000);
            
            // Step 2: Enter confirmation text
            this.core.logger.info('- Step 2: Enter confirmation text');
            const testConfirmation = `TEST_FEEDBACK_${Date.now()}`;
            await this.core.typeText(testConfirmation);
            await this.core.sleep(500);
            
            // Step 3: Submit request
            this.core.logger.info('- Step 3: Submit request');
            const submitTap = await this.core.tapByText('Submit Request');
            if (!submitTap.success) {
                const submitTap2 = await this.core.tapByText('Send Request');
                if (!submitTap2.success) {
                    this.core.logger.error('❌ Could not submit request');
                    return { success: false, error: 'Could not submit credit request' };
                }
            }
            
            // Step 4: Verify loading spinner appears immediately
            this.core.logger.info('- Step 4: Verify loading state');
            await this.core.sleep(500); // Check immediately
            await this.dumpUI();
            const loadingUI = this.core.readLastUIDump();
            
            const hasLoadingSpinner = loadingUI.includes('CircularProgressIndicator') ||
                                     loadingUI.includes('Loading') ||
                                     loadingUI.includes('Sending request');
            
            if (!hasLoadingSpinner) {
                this.core.logger.error('❌ FAILURE: Loading spinner not shown immediately');
                return { success: false, error: 'Loading state not displayed' };
            }
            this.core.logger.info('✅ Loading spinner found');
            
            // Step 5: Verify "Sending request..." message
            this.core.logger.info('- Step 5: Verify loading message');
            const hasLoadingMessage = loadingUI.includes('Sending request') ||
                                    loadingUI.includes('Sending') ||
                                    loadingUI.includes('request...');
            
            if (hasLoadingMessage) {
                this.core.logger.info('✅ Loading message found');
            } else {
                this.core.logger.warn('⚠️ Loading message not found (may use different text)');
            }
            
            // Step 6: Wait for response
            this.core.logger.info('- Step 6: Wait for response');
            await this.core.sleep(3000);
            await this.dumpUI();
            const responseUI = this.core.readLastUIDump();
            
            // Step 7: Verify success/error message with request ID
            this.core.logger.info('- Step 7: Verify feedback message with request ID');
            const hasSuccessMessage = responseUI.includes('Request sent') ||
                                    responseUI.includes('received') ||
                                    responseUI.includes('acknowledged');
            
            const hasRequestId = /credit_req_\d+|Request ID|ID:/i.test(responseUI);
            
            if (!hasSuccessMessage && !hasRequestId) {
                this.core.logger.warn('⚠️ Success message or request ID not immediately visible');
            } else {
                if (hasSuccessMessage) {
                    this.core.logger.info('✅ Success message found');
                }
                if (hasRequestId) {
                    this.core.logger.info('✅ Request ID found in feedback');
                }
            }
            
            // Verify request ID format
            const requestIdMatch = responseUI.match(/credit_req_\d+/i);
            if (requestIdMatch) {
                const requestId = requestIdMatch[0];
                this.core.logger.info(`✅ Request ID format correct: ${requestId.substring(0, 20)}...`);
            } else {
                this.core.logger.warn('⚠️ Request ID not in expected format');
            }
            
            // Step 8: Verify user can dismiss feedback
            this.core.logger.info('- Step 8: Verify feedback is dismissible');
            await this.core.sleep(2000);
            await this.dumpUI();
            const dismissUI = this.core.readLastUIDump();
            
            const hasDismissButton = dismissUI.includes('OK') ||
                                   dismissUI.includes('Close') ||
                                   dismissUI.includes('Dismiss');
            
            if (hasDismissButton) {
                this.core.logger.info('✅ Dismiss button found');
                
                const okTap = await this.core.tapByText('OK');
                if (okTap.success) {
                    await this.core.sleep(1000);
                    await this.dumpUI();
                    const afterDismissUI = this.core.readLastUIDump();
                    
                    const feedbackStillVisible = afterDismissUI.includes('Request sent') ||
                                               afterDismissUI.includes('Request ID');
                    
                    if (!feedbackStillVisible) {
                        this.core.logger.info('✅ Feedback dismissed successfully');
                    } else {
                        this.core.logger.warn('⚠️ Feedback still visible after dismiss');
                    }
                }
            } else {
                this.core.logger.warn('⚠️ Dismiss button not found (may auto-dismiss)');
            }
            
            // Verify request ID is logged (check logcat)
            this.core.logger.info('- Step 9: Verify request ID in logcat');
            const apiLogs = await this.core.captureAPILogs(200);
            if (apiLogs.success) {
                const allLogs = [
                    ...apiLogs.parsed.requests,
                    ...apiLogs.parsed.responses,
                    ...apiLogs.parsed.info || []
                ].join('\n');
                
                const hasRequestIdInLogs = /credit_req_\d+/i.test(allLogs);
                if (hasRequestIdInLogs) {
                    this.core.logger.info('✅ Request ID found in logcat');
                } else {
                    this.core.logger.warn('⚠️ Request ID not found in recent logcat');
                }
            }
            
            this.core.logger.info('✅ Completed: Journey-UX-08CreditRequestFeedback-Validation');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Credit request feedback validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = JourneyUX08CreditRequestFeedbackValidation;

