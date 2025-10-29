const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Pluct-Test-Validation-04APIConnectivity - API connectivity validation module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Validates Business Engine connectivity and endpoint responses
 */
class PluctTestValidationAPIConnectivity extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Pluct-Test-Validation-04APIConnectivity';
        this.maxDuration = 30000; // 30 seconds max
    }

    async execute() {
        try {
            this.core.logger.info('🔍 Validating API connectivity...');
            
            // Test Business Engine health endpoint
            const healthResponse = await this.core.httpGet('https://pluct-business-engine.romeo-lya2.workers.dev/health');
            if (!healthResponse.success || healthResponse.status !== 200) {
                return { success: false, error: 'Business Engine health check failed' };
            }
            
            const healthData = JSON.parse(healthResponse.body);
            if (healthData.status !== 'ok') {
                return { success: false, error: 'Business Engine not operational' };
            }
            
            // Test credit balance endpoint (without auth for basic connectivity)
            const balanceResponse = await this.core.httpGet('https://pluct-business-engine.romeo-lya2.workers.dev/v1/credits/balance');
            // We expect this to fail with 401, which is correct behavior
            if (balanceResponse.status !== 401) {
                return { success: false, error: 'Credit balance endpoint not responding correctly' };
            }
            
            this.core.logger.info('✅ API connectivity validation passed');
            return { success: true, details: { healthEndpoint: true, balanceEndpoint: true } };
            
        } catch (error) {
            this.core.logger.error(`❌ API connectivity validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctTestValidationAPIConnectivity;
