const { BaseJourney } = require('./Pluct-Journey-01Orchestrator');

/**
 * Pluct-Test-Validation-02SystemHealth - System health validation module
 * Follows naming convention: [Project]-[ParentScope]-[ChildScope]-[Separation of Concern][CoreResponsibility]
 * Validates device connectivity, network status, and app installation
 */
class PluctTestValidationSystemHealth extends BaseJourney {
    constructor(core) {
        super(core);
        this.name = 'Pluct-Test-Validation-02SystemHealth';
        this.maxDuration = 30000; // 30 seconds max
    }

    async execute() {
        try {
            this.core.logger.info('🔍 Validating system health...');
            
            // Check device connectivity
            const deviceResult = await this.core.checkDeviceConnectivity();
            if (!deviceResult.success) {
                return { success: false, error: 'Device connectivity check failed' };
            }
            
            // Check network connectivity
            const networkResult = await this.core.checkNetworkConnectivity();
            if (!networkResult.success) {
                return { success: false, error: 'Network connectivity check failed' };
            }
            
            // Check app installation
            const appResult = await this.core.checkAppInstallation();
            if (!appResult.success) {
                return { success: false, error: 'App installation check failed' };
            }
            
            this.core.logger.info('✅ System health validation passed');
            return { success: true, details: { device: deviceResult, network: networkResult, app: appResult } };
            
        } catch (error) {
            this.core.logger.error(`❌ System health validation failed: ${error.message}`);
            return { success: false, error: error.message };
        }
    }
}

module.exports = PluctTestValidationSystemHealth;
