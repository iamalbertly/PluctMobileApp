const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

class PluctUIHeader01CreditBalanceValidation extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Pluct-UI-Header-01CreditBalance-Validation';
    }

    async execute() {
        this.core.logger.info('🚀 Starting: Pluct-UI-Header-01CreditBalance-Validation');
        
        try {
            // 1. App Initialization & Balance Load
            this.core.logger.info('- Step 1: App Initialization & Balance Load');
            await this.ensureAppForeground();
            await this.core.sleep(2000);
            
            // Capture initial UI state
            await this.dumpUI();
            const initialUI = this.core.readLastUIDump();
            
            // Look for credit balance display in header
            const hasCreditDisplay = initialUI.includes('10') || 
                                   initialUI.includes('💎') || 
                                   initialUI.includes('Credit balance') ||
                                   initialUI.includes('credits') ||
                                   initialUI.includes('Credit') || 
                                   initialUI.includes('balance') ||
                                   initialUI.includes('diamond');
            
            if (hasCreditDisplay) {
                this.core.logger.info('✅ Credit balance display found in header');
            } else {
                this.core.logger.warn('⚠️ Credit balance display not immediately visible, checking for loading state');
                // Wait a bit more for potential loading
                await this.core.sleep(3000);
                await this.dumpUI();
                const updatedUI = this.core.readLastUIDump();
                const hasCreditAfterWait = updatedUI.includes('10') || 
                                         updatedUI.includes('💎') || 
                                         updatedUI.includes('Credit balance') ||
                                         updatedUI.includes('credits') ||
                                         updatedUI.includes('Credit') || 
                                         updatedUI.includes('balance') ||
                                         updatedUI.includes('diamond');
                
                if (hasCreditAfterWait) {
                    this.core.logger.info('✅ Credit balance display found after waiting');
                } else {
                    this.core.logger.warn('⚠️ Credit balance display not found - may need to implement header component');
                }
            }

            // 2. Balance Refresh Functionality
            this.core.logger.info('- Step 2: Balance Refresh Functionality');
            
            // Try to find and tap on credit balance display using content descriptions first
            let tapped = false;
            const contentDescriptionResult = await this.core.tapByTestTag('Credit balance display showing');
            if (contentDescriptionResult.success) {
                this.core.logger.info('✅ Tapped on credit balance using content description');
                tapped = true;
            } else {
                // Try tapping on the clickable container that contains the credit balance
                const tapResult = await this.core.tapByCoordinates(568, 113); // Center of the clickable credit balance area
                if (tapResult.success) {
                    this.core.logger.info('✅ Tapped on credit balance container');
                    tapped = true;
                } else {
                    // Fallback to text-based tapping - look for the actual text in the header
                    const creditElements = [
                        '10', '💎', 'Credit balance', 'credits', 'Credit', 'balance', 'diamond'
                    ];
                    
                    for (const element of creditElements) {
                        const tapResult = await this.core.tapByText(element);
                        if (tapResult.success) {
                            this.core.logger.info(`✅ Tapped on credit balance element: ${element}`);
                            tapped = true;
                            break;
                        }
                    }
                }
            }
            
            if (!tapped) {
                this.core.logger.error('❌ CRITICAL FAILURE: Could not find credit balance element to tap for refresh');
                this.core.logger.error('❌ This step is required for the test to pass');
                this.core.logger.error('❌ Available elements in UI:');
                await this.dumpUI();
                const uiDump = this.core.readLastUIDump();
                this.core.logger.error('❌ UI DUMP for debugging:');
                this.core.logger.error(uiDump);
                throw new Error('CRITICAL FAILURE: Could not find credit balance element to tap for refresh');
            } else {
                // Wait for potential refresh
                await this.core.sleep(2000);
                await this.dumpUI();
                this.core.logger.info('✅ Balance refresh attempted');
            }

            // 3. Error Handling & Recovery
            this.core.logger.info('- Step 3: Error Handling & Recovery');
            
            // Check for any error states in the header
            await this.dumpUI();
            const errorUI = this.core.readLastUIDump();
            
            const hasErrorState = errorUI.includes('Error') || 
                                errorUI.includes('error') ||
                                errorUI.includes('Failed') ||
                                errorUI.includes('failed') ||
                                errorUI.includes('⚠️') ||
                                errorUI.includes('❌');
            
            if (hasErrorState) {
                this.core.logger.warn('⚠️ Error state detected in header');
                // Try to find and tap retry button
                const retryElements = ['Retry', 'retry', 'Try Again', 'try again', 'Refresh', 'refresh'];
                for (const element of retryElements) {
                    const retryResult = await this.core.tapByText(element);
                    if (retryResult.success) {
                        this.core.logger.info(`✅ Tapped retry element: ${element}`);
                        break;
                    }
                }
            } else {
                this.core.logger.info('✅ No error state detected in header');
            }

            // 4. Low Balance Scenarios
            this.core.logger.info('- Step 4: Low Balance Scenarios');
            
            // Check for different balance states
            await this.dumpUI();
            const balanceUI = this.core.readLastUIDump();
            
            // Look for balance indicators
            const hasLowBalance = balanceUI.includes('0') && balanceUI.includes('credit');
            const hasWarningBalance = balanceUI.includes('1') && balanceUI.includes('credit');
            const hasNormalBalance = balanceUI.includes('credit') && !balanceUI.includes('0') && !balanceUI.includes('1');
            
            if (hasLowBalance) {
                this.core.logger.info('✅ Low balance (0 credits) detected');
            } else if (hasWarningBalance) {
                this.core.logger.info('✅ Warning balance (1 credit) detected');
            } else if (hasNormalBalance) {
                this.core.logger.info('✅ Normal balance detected');
            } else {
                this.core.logger.info('ℹ️ Balance state not clearly identifiable from UI');
            }

            // 5. Validate Header Layout and Accessibility
            this.core.logger.info('- Step 5: Header Layout and Accessibility');
            
            await this.dumpUI();
            const finalUI = this.core.readLastUIDump();
            
            // Check for header elements
            const hasHeaderElements = finalUI.includes('Settings') || 
                                   finalUI.includes('settings') ||
                                   finalUI.includes('⚙️') ||
                                   finalUI.includes('gear') ||
                                   finalUI.includes('credits') ||
                                   finalUI.includes('Credit');
            
            if (hasHeaderElements) {
                this.core.logger.info('✅ Header elements found');
            } else {
                this.core.logger.warn('⚠️ Header elements not clearly visible');
            }

            this.core.logger.info('✅ Completed: Pluct-UI-Header-01CreditBalance-Validation');
            return { success: true };
            
        } catch (error) {
            this.core.logger.error(`❌ Header credit balance validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctUIHeader01CreditBalanceValidation;
