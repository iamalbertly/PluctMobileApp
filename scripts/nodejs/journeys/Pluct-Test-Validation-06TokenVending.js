const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Pluct-Test-Validation-06TokenVending - Token vending validation module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Validates token vending system functionality
 */
class PluctTestValidationTokenVending extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Pluct-Test-Validation-06TokenVending';
        this.maxDuration = 30000; // 30 seconds max
    }

    async execute() {
        try {
            this.core.logger.info('🔍 Validating token vending system...');
            
            // Test token vending endpoint
            const userId = 'mobile-test-runner';
            const vendResponse = await this.core.httpPost(
                `${this.core.config.businessEngineUrl}/v1/vend-token`,
                { userId, clientRequestId: `vend-${Date.now()}` },
                { ...this.core.buildUserAuthHeaders(userId), 'Content-Type': 'application/json' }
            );
            
            if (!vendResponse.success || vendResponse.status !== 200) {
                return { success: false, error: 'Token vending request failed' };
            }
            
            const vendData = JSON.parse(vendResponse.body);
            const expiry = vendData.expiresAt || vendData.expiresIn;
            if (!vendData.token || !expiry) {
                return { success: false, error: 'Invalid token vending response format' };
            }
            
            this.core.logger.info('✅ Token vending validation passed');
            return { success: true, details: { tokenReceived: true, expiresAt: vendData.expiresAt, expiresIn: vendData.expiresIn } };
            
        } catch (error) {
            this.core.logger.error(`❌ Token vending validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctTestValidationTokenVending;
