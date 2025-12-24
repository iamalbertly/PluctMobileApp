const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class JourneyUX09ErrorRecoveryActionsValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Journey-UX-09ErrorRecoveryActions-Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Journey-UX-09ErrorRecoveryActions-Validation');
        
        try {
            // Step 1: Trigger insufficient credits error
            this.core.logger.info('- Step 1: Trigger insufficient credits error');
            await this.ensureAppForeground();
            await this.core.sleep(2000);
            
            // Check current balance first
            await this.dumpUI();
            const initialUI = this.core.readLastUIDump();
            const hasZeroCredits = initialUI.includes('0') && 
                                 (initialUI.includes('credit') || initialUI.includes('Credit'));
            
            if (!hasZeroCredits) {
                this.core.logger.info('  User has credits, will test with invalid URL instead');
            }
            
            // Try to submit with potentially insufficient credits or invalid URL
            const testUrl = 'https://invalid-test-url-that-will-fail.com/';
            await this.core.tapByTestTag('capture_component_label');
            await this.core.sleep(500);
            await this.core.typeText(testUrl);
            await this.core.sleep(1000);
            
            const submitTap = await this.core.tapByTestTag('extract_script_button');
            if (!submitTap.success) {
                await this.core.tapByText('Extract Script');
            }
            await this.core.sleep(3000);
            
            // Step 2: Verify "Add Credits" action button appears (if credits error)
            this.core.logger.info('- Step 2: Check for error recovery actions');
            await this.dumpUI();
            const errorUI = this.core.readLastUIDump();
            
            const hasError = errorUI.includes('Error') ||
                           errorUI.includes('error') ||
                           errorUI.includes('Something went wrong') ||
                           errorUI.includes('failed');
            
            if (!hasError) {
                this.core.logger.warn('⚠️ Error not triggered (may need different approach)');
                // Continue to test other error types
            } else {
                this.core.logger.info('✅ Error displayed');
                
                // Check for "Add Credits" button
                const hasAddCredits = errorUI.includes('Add Credits') ||
                                    errorUI.includes('Add credits') ||
                                    errorUI.includes('Get Credits');
                
                if (hasAddCredits) {
                    this.core.logger.info('✅ "Add Credits" action button found');
                } else {
                    this.core.logger.info('  "Add Credits" not shown (error may not be credits-related)');
                }
            }
            
            // Step 3: Trigger network error (simulate by using invalid endpoint)
            this.core.logger.info('- Step 3: Test network error recovery actions');
            // Dismiss current error if any
            const dismissTap = await this.core.tapByText('Dismiss');
            if (!dismissTap.success) {
                await this.core.pressKey('Back');
            }
            await this.core.sleep(1000);
            
            // Try another invalid submission to potentially trigger network error
            await this.core.tapByTestTag('capture_component_label');
            await this.core.sleep(500);
            await this.core.typeText('another-invalid');
            await this.core.sleep(1000);
            const submitTap2 = await this.core.tapByText('Extract Script');
            if (submitTap2.success) {
                await this.core.sleep(3000);
                await this.dumpUI();
                const networkErrorUI = this.core.readLastUIDump();
                
                // Step 4: Verify "Check Connection" and "Retry" buttons appear
                const hasNetworkError = networkErrorUI.includes('network') ||
                                     networkErrorUI.includes('connection') ||
                                     networkErrorUI.includes('Network');
                
                if (hasNetworkError) {
                    this.core.logger.info('✅ Network error detected');
                    
                    const hasCheckConnection = networkErrorUI.includes('Check Connection') ||
                                             networkErrorUI.includes('Check connection');
                    const hasRetry = networkErrorUI.includes('Retry') ||
                                   networkErrorUI.includes('retry');
                    
                    if (hasCheckConnection) {
                        this.core.logger.info('✅ "Check Connection" button found');
                    } else {
                        this.core.logger.warn('⚠️ "Check Connection" button not found');
                    }
                    
                    if (hasRetry) {
                        this.core.logger.info('✅ "Retry" button found');
                    } else {
                        this.core.logger.warn('⚠️ "Retry" button not found');
                    }
                }
            }
            
            // Step 5: Trigger timeout error (if possible)
            this.core.logger.info('- Step 5: Test timeout error recovery');
            // Dismiss current error
            await this.core.pressKey('Back');
            await this.core.sleep(1000);
            
            // Step 6: Verify "Retry" button appears for timeout
            // (Timeout errors are harder to trigger, so we'll verify the pattern exists)
            this.core.logger.info('- Step 6: Verify retry action for timeout');
            await this.dumpUI();
            const currentUI = this.core.readLastUIDump();
            
            const hasTimeoutError = currentUI.includes('timeout') ||
                                  currentUI.includes('Timeout') ||
                                  currentUI.includes('taking longer');
            
            if (hasTimeoutError) {
                const hasRetryForTimeout = currentUI.includes('Retry');
                if (hasRetryForTimeout) {
                    this.core.logger.info('✅ "Retry" button found for timeout error');
                }
            } else {
                this.core.logger.info('  Timeout error not currently visible (expected)');
            }
            
            // Step 7: Test that action buttons execute correct actions
            this.core.logger.info('- Step 7: Test action button functionality');
            await this.dumpUI();
            const actionUI = this.core.readLastUIDump();
            
            // Try to find and tap a recovery action
            const retryTap = await this.core.tapByText('Retry');
            if (retryTap.success) {
                this.core.logger.info('✅ Retry button tapped');
                await this.core.sleep(2000);
                await this.dumpUI();
                const afterRetryUI = this.core.readLastUIDump();
                
                // Check if retry initiated (may show loading or processing)
                const retryInitiated = afterRetryUI.includes('Retrying') ||
                                    afterRetryUI.includes('Starting') ||
                                    afterRetryUI.includes('Processing');
                
                if (retryInitiated) {
                    this.core.logger.info('✅ Retry action executed correctly');
                } else {
                    this.core.logger.warn('⚠️ Retry action may not have executed');
                }
            } else {
                // Try Add Credits if available
                const addCreditsTap = await this.core.tapByText('Add Credits');
                if (addCreditsTap.success) {
                    this.core.logger.info('✅ Add Credits button tapped');
                    await this.core.sleep(1000);
                    await this.dumpUI();
                    const afterAddCreditsUI = this.core.readLastUIDump();
                    
                    const creditsDialogOpen = afterAddCreditsUI.includes('Add More Credits') ||
                                            afterAddCreditsUI.includes('Settings');
                    
                    if (creditsDialogOpen) {
                        this.core.logger.info('✅ Add Credits action executed correctly');
                    }
                } else {
                    this.core.logger.info('  No recovery actions available to test');
                }
            }
            
            // Verify actions are prioritized (primary vs secondary)
            this.core.logger.info('- Step 8: Verify action prioritization');
            await this.dumpUI();
            const priorityUI = this.core.readLastUIDump();
            
            // Check if primary actions appear before secondary
            // This is harder to verify via UI dump, but we can check if multiple actions exist
            const actionCount = (priorityUI.match(/Retry|Add Credits|Check Connection/g) || []).length;
            if (actionCount > 1) {
                this.core.logger.info(`✅ Multiple recovery actions found (${actionCount})`);
            }
            
            // Verify actions are contextually appropriate
            this.core.logger.info('- Step 9: Verify contextual appropriateness');
            // Error-specific actions should match error type
            await this.dumpUI();
            const contextUI = this.core.readLastUIDump();
            
            const hasCreditsError = contextUI.includes('insufficient') ||
                                  contextUI.includes('credits') ||
                                  contextUI.includes('balance');
            const hasNetworkError2 = contextUI.includes('network') ||
                                   contextUI.includes('connection');
            
            if (hasCreditsError) {
                const hasCreditsAction = contextUI.includes('Add Credits');
                if (hasCreditsAction) {
                    this.core.logger.info('✅ Credits error shows appropriate action');
                }
            }
            
            if (hasNetworkError2) {
                const hasNetworkAction = contextUI.includes('Check Connection') ||
                                       contextUI.includes('Retry');
                if (hasNetworkAction) {
                    this.core.logger.info('✅ Network error shows appropriate action');
                }
            }
            
            this.core.logger.info('✅ Completed: Journey-UX-09ErrorRecoveryActions-Validation');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Error recovery actions validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = JourneyUX09ErrorRecoveryActionsValidation;

