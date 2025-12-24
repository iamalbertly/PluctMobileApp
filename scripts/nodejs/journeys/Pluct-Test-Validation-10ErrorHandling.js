const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Pluct-Test-Validation-10ErrorHandling - Error handling validation module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Validates error handling and recovery mechanisms
 */
class PluctTestValidationErrorHandling extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Pluct-Test-Validation-10ErrorHandling';
        this.maxDuration = 30000; // 30 seconds max
    }

    async execute() {
        try {
            this.core.logger.info('🔍 Validating error handling...');
            
            // Test invalid URL input
            const invalidUrlResult = await this.core.inputText('url_input', 'invalid-url');
            if (!invalidUrlResult.success) {
                return { success: false, error: 'Invalid URL input test failed' };
            }
            
            await this.core.sleep(2000);
            
            // Check for error message display
            await this.core.dumpUIHierarchy();
            const uiDump = this.core.readLastUIDump();
            
            const hasErrorMessage = uiDump.includes('Invalid URL') || 
                                  uiDump.includes('error') || 
                                  uiDump.includes('Error');
            
            if (!hasErrorMessage) {
                return { success: false, error: 'Error message not displayed for invalid URL' };
            }
            
            // Test error recovery by clearing and entering valid URL
            const clearResult = await this.core.clickElement('clear_button');
            if (!clearResult.success) {
                return { success: false, error: 'Error recovery clear failed' };
            }
            
            const validUrlResult = await this.core.inputText('url_input', 'https://vm.tiktok.com/ZMDRUGT2P/');
            if (!validUrlResult.success) {
                return { success: false, error: 'Error recovery valid input failed' };
            }
            
            this.core.logger.info('✅ Error handling validation passed');
            return { success: true, details: { errorDisplay: true, errorRecovery: true } };
            
        } catch (error) {
            this.core.logger.error(`❌ Error handling validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctTestValidationErrorHandling;
