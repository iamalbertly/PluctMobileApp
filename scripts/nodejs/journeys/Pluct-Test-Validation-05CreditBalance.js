const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Pluct-Test-Validation-05CreditBalance - Credit balance validation module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Validates credit balance system functionality
 */
class PluctTestValidationCreditBalance extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Pluct-Test-Validation-05CreditBalance';
        this.maxDuration = 30000; // 30 seconds max
    }

    async execute() {
        try {
            this.core.logger.info('🔍 Validating credit balance system...');
            
            // Generate test JWT token
            const jwtToken = this.core.generateTestJWT('mobile');
            
            // Test credit balance endpoint with valid JWT
            const balanceResponse = await this.core.httpGet(
                'https://pluct-business-engine.romeo-lya2.workers.dev/v1/credits/balance',
                { 'Authorization': `Bearer ${jwtToken}` }
            );
            
            if (!balanceResponse.success || balanceResponse.status !== 200) {
                return { success: false, error: 'Credit balance request failed' };
            }
            
            const balanceData = JSON.parse(balanceResponse.body);
            if (typeof balanceData.balance !== 'number') {
                return { success: false, error: 'Invalid balance response format' };
            }
            
            this.core.logger.info(`✅ Credit balance validation passed - Balance: ${balanceData.balance}`);
            return { success: true, details: { balance: balanceData.balance, userId: balanceData.userId } };
            
        } catch (error) {
            this.core.logger.error(`❌ Credit balance validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctTestValidationCreditBalance;
