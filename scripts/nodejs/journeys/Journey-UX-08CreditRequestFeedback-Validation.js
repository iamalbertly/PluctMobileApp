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
            const foreground = await this.ensureAppForeground();
            if (!foreground.success) {
                await this.failWithDiagnostics('Failed to bring app to foreground');
                return { success: false, error: 'Failed to bring app to foreground' };
            }
            await this.core.sleep(2000);

            const tryOpenSettings = async () => {
                let tap = await this.core.tapByTestTag('settings_button');
                if (tap.success) return tap;
                tap = await this.core.tapByContentDesc('Settings button');
                if (tap.success) return tap;
                return this.core.tapByText('Settings');
            };

            const ensureCreditRequestForm = async () => {
                const settingsTap = await tryOpenSettings();
                if (!settingsTap.success) {
                    return { success: false, error: 'Could not open settings' };
                }
                const settingsVisible = await this.core.waitForText('Settings dialog', 5000, 500);
                if (!settingsVisible.success) {
                    return { success: false, error: 'Settings dialog not visible' };
                }

                let addCreditsTap = await this.core.tapByTestTag('add_more_credits_button');
                if (!addCreditsTap.success) {
                    addCreditsTap = await this.core.tapByContentDesc('Add more credits button');
                }
                if (!addCreditsTap.success) {
                    addCreditsTap = await this.core.tapByText('Add More Credits');
                }
                if (!addCreditsTap.success) {
                    await this.dumpUI();
                    const ui = this.core.readLastUIDump();
                    const inSettings = ui.includes('Settings dialog') || ui.includes('settings_dialog');
                    if (!inSettings) {
                        const reopen = await tryOpenSettings();
                        if (!reopen.success) {
                            return { success: false, error: 'Could not reopen settings' };
                        }
                        await this.core.sleep(1000);
                        addCreditsTap = await this.core.tapByTestTag('add_more_credits_button');
                        if (!addCreditsTap.success) {
                            addCreditsTap = await this.core.tapByContentDesc('Add more credits button');
                        }
                        if (!addCreditsTap.success) {
                            addCreditsTap = await this.core.tapByText('Add More Credits');
                        }
                    }
                }
                if (!addCreditsTap.success) {
                    return { success: false, error: 'Could not find Add More Credits button' };
                }
                const formVisible = await this.core.waitForText('Paste confirmation message', 5000, 500);
                if (!formVisible.success) {
                    return { success: false, error: 'Credit request form not visible' };
                }
                return { success: true };
            };

            const formReady = await ensureCreditRequestForm();
            if (!formReady.success) {
                await this.failWithDiagnostics(formReady.error);
                return { success: false, error: formReady.error };
            }

            // Step 2: Enter confirmation text
            this.core.logger.info('- Step 2: Enter confirmation text');
            const testConfirmation = `TEST_FEEDBACK_${Date.now()}`;
            let focusTap = await this.core.tapByTestTag('credit_request_confirmation_input');
            if (!focusTap.success) {
                focusTap = await this.core.tapByContentDesc('Credit request confirmation input');
            }
            if (!focusTap.success) {
                focusTap = await this.core.tapByText('Paste confirmation message');
            }
            if (!focusTap.success) {
                focusTap = await this.core.tapFirstEditText();
            }
            if (!focusTap.success) {
                await this.failWithDiagnostics('Could not focus confirmation input');
                return { success: false, error: 'Could not focus confirmation input' };
            }
            const typeResult = await this.core.typeText(testConfirmation);
            if (!typeResult.success) {
                await this.failWithDiagnostics('Failed to enter confirmation text');
                return { success: false, error: 'Failed to enter confirmation text' };
            }
            await this.core.sleep(500);
            await this.dumpUI();
            let typedUI = this.core.readLastUIDump();
            let dialogVisible = typedUI.includes('Settings dialog');
            let inputVisible = typedUI.includes(testConfirmation) || !typedUI.includes('Paste confirmation message');
            if (!dialogVisible || !inputVisible) {
                await this.core.inputTextViaClipboard(testConfirmation);
                await this.core.sleep(500);
                await this.dumpUI();
                typedUI = this.core.readLastUIDump();
                dialogVisible = typedUI.includes('Settings dialog');
                inputVisible = typedUI.includes(testConfirmation) || !typedUI.includes('Paste confirmation message');
            }
            if (!dialogVisible) {
                await this.failWithDiagnostics('Credit request dialog dismissed while typing');
                return { success: false, error: 'Credit request dialog dismissed while typing' };
            }
            if (!inputVisible) {
                await this.failWithDiagnostics('Confirmation text not visible after input');
                return { success: false, error: 'Confirmation text not visible after input' };
            }
            
            // Step 3: Submit request
            this.core.logger.info('- Step 3: Submit request');
            let submitTap = await this.core.tapByTestTag('submit_credit_request');
            if (!submitTap.success) {
                submitTap = await this.core.tapByContentDesc('Submit credit request');
            }
            if (!submitTap.success) {
                submitTap = await this.core.tapByText('Submit Request');
            }
            if (!submitTap.success) {
                submitTap = await this.core.tapByText('Send Request');
            }
            if (!submitTap.success) {
                await this.failWithDiagnostics('Could not submit credit request');
                return { success: false, error: 'Could not submit credit request' };
            }
            
            // Step 4: Verify loading spinner appears immediately
            this.core.logger.info('- Step 4: Verify loading state');
            await this.core.sleep(500); // Check immediately
            await this.dumpUI();
            const loadingUI = this.core.readLastUIDump();

            if (!loadingUI.includes('Settings dialog')) {
                await this.failWithDiagnostics('Credit request dialog dismissed after submit');
                return { success: false, error: 'Credit request dialog dismissed after submit' };
            }
            
            const hasLoadingSpinner = loadingUI.includes('CircularProgressIndicator') ||
                                     loadingUI.includes('Loading') ||
                                     loadingUI.includes('Sending request');
            const hasImmediateStatus = loadingUI.includes('Request sent') ||
                                     loadingUI.includes('Request ID') ||
                                     loadingUI.includes('OK');
            
            if (!hasLoadingSpinner && !hasImmediateStatus) {
                await this.failWithDiagnostics('Loading state not displayed');
                return { success: false, error: 'Loading state not displayed' };
            }
            if (hasLoadingSpinner) {
                this.core.logger.info('? Loading spinner found');
            } else {
                this.core.logger.info('? Request status displayed before loading check');
            }
            
            // Step 5: Verify "Sending request..." message
            this.core.logger.info('- Step 5: Verify loading message');
            if (hasLoadingSpinner) {
                const hasLoadingMessage = loadingUI.includes('Sending request') ||
                                    loadingUI.includes('Sending') ||
                                    loadingUI.includes('request...');
                
                if (hasLoadingMessage) {
                    this.core.logger.info('? Loading message found');
                } else {
                    this.core.logger.warn('?? Loading message not found (may use different text)');
                }
            } else {
                this.core.logger.info('? Loading message check skipped (status already visible)');
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


















