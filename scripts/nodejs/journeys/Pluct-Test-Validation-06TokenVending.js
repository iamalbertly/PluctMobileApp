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
            
            // Generate test JWT token
            const jwtToken = this.core.generateTestJWT('mobile');
            
            // Test token vending endpoint
            const vendResponse = await this.core.httpPost(
                'https://pluct-business-engine.romeo-lya2.workers.dev/v1/vend-token',
                { userId: 'mobile' },
                { 'Authorization': `Bearer ${jwtToken}`, 'Content-Type': 'application/json' }
            );
            
            if (!vendResponse.success || vendResponse.status !== 200) {
                return { success: false, error: 'Token vending request failed' };
            }
            
            const vendData = JSON.parse(vendResponse.body);
            if (!vendData.token || !vendData.expiresAt) {
                return { success: false, error: 'Invalid token vending response format' };
            }
            
            this.core.logger.info('✅ Token vending validation passed');
            return { success: true, details: { tokenReceived: true, expiresAt: vendData.expiresAt } };
            
        } catch (error) {
            this.core.logger.error(`❌ Token vending validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctTestValidationTokenVending;
